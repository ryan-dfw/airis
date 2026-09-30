package com.tollview.airis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tollview.airis.data.ApiKeyStore
import com.tollview.airis.data.Print
import com.tollview.airis.data.PrintStore
import com.tollview.airis.data.Rating
import com.tollview.airis.pipeline.Pipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What's showing on the film of the instant camera on the home screen. */
sealed interface Film {
    /** Fresh sheet, waiting for the shutter on the camera. */
    data object Blank : Film

    /** [startedAt] doubles as the sheet id: one sheet per shot, kept across retries. */
    data class Developing(val stage: Stage, val startedAt: Long) : Film

    /** A finished print. The UI plays the exposure fade the first time it sees [print]. */
    data class Printed(val print: Print, val sheet: Long) : Film

    data class Failed(val message: String, val canRetry: Boolean, val sheet: Long) : Film
}

/** The sheet currently on screen, or null when nothing has come out of the camera. */
val Film.sheetId: Long?
    get() = when (this) {
        Film.Blank -> null
        is Film.Developing -> startedAt
        is Film.Printed -> sheet
        is Film.Failed -> sheet
    }

enum class Stage(val label: String, val detail: String) {
    Receiving("Receiving", "Pulling the shot off the camera"),
    Reading("Reading the scene", "Processing"),
    Developing("Developing", "Developing"),
    Fixing("Fixing", "Bringing the print home"),
}

class AirisViewModel(app: Application) : AndroidViewModel(app) {

    private val keyStore = ApiKeyStore(app)
    private val printStore = PrintStore(app)
    private val pipeline = Pipeline(app)

    private val _apiKey = MutableStateFlow(keyStore.load())
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    private val _film = MutableStateFlow<Film>(Film.Blank)
    val film: StateFlow<Film> = _film.asStateFlow()

    private val _camera = MutableStateFlow(Pipeline.CameraStatus.Searching)
    val camera: StateFlow<Pipeline.CameraStatus> = _camera.asStateFlow()

    private val _prints = MutableStateFlow<List<Print>>(emptyList())
    val prints: StateFlow<List<Print>> = _prints.asStateFlow()

    /** Prints whose exposure animation has already played, so it only happens once. */
    private val _exposed = MutableStateFlow<Set<String>>(emptySet())
    val exposed: StateFlow<Set<String>> = _exposed.asStateFlow()

    /** Thumbs up / down, by print id. */
    private val _ratings = MutableStateFlow<Map<String, Rating>>(emptyMap())
    val ratings: StateFlow<Map<String, Rating>> = _ratings.asStateFlow()

    private val foreground = MutableStateFlow(false)
    private var developJob: Job? = null
    private var lastSource: ByteArray? = null

    init {
        refreshPrints()
        _ratings.value = printStore.ratings()
        _exposed.value = printStore.all().map { it.id }.toSet()

        // Always listening: poll the camera while the app is visible and a key is set.
        viewModelScope.launch {
            combine(foreground, _apiKey) { fg, key -> fg && !key.isNullOrBlank() }
                .distinctUntilChanged()
                .collectLatest { ready ->
                    if (!ready) return@collectLatest
                    while (true) {
                        developJob?.join()
                        val url = pipeline.pollForPhotoUrl(keyStore.lastProcessedUrl) { status ->
                            _camera.value = status
                        }
                        keyStore.lastProcessedUrl = url
                        developJob =viewModelScope.launch { developFromCamera(url) }
                        developJob?.join()
                    }
                }
        }
    }

    fun setForeground(visible: Boolean) {
        foreground.value = visible
    }

    // ---- API key -------------------------------------------------------------------------

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        keyStore.save(trimmed)
        _apiKey.value = trimmed
    }

    fun clearApiKey() {
        keyStore.clear()
        _apiKey.value = null
    }

    // ---- Developing ----------------------------------------------------------------------

    val isBusy: Boolean get() = developJob?.isActive == true

    private suspend fun developFromCamera(url: String) {
        _film.value = Film.Developing(Stage.Receiving, System.currentTimeMillis())
        val bytes = try {
            pipeline.downloadFromCamera(url)
        } catch (e: Exception) {
            fail(e, canRetry = false)
            return
        }
        lastSource = bytes
        develop(bytes)
    }

    /** Steps 3–5 of the pipeline: caption → gpt-image-1 → save. */
    private suspend fun develop(source: ByteArray) {
        val started = currentSheet()
        val key = _apiKey.value
        if (key.isNullOrBlank()) {
            _film.value = Film.Failed("Add an API key in Setup first.", canRetry = true, sheet = started)
            return
        }
        try {
            _film.value = Film.Developing(Stage.Reading, started)
            val caption = pipeline.sendToOpenAI(source, key)

            _film.value = Film.Developing(Stage.Developing, started)
            val generated = pipeline.generateImage(caption, key)

            _film.value = Film.Developing(Stage.Fixing, started)

            val print = withContext(Dispatchers.IO) { printStore.save(generated) }
            refreshPrints()
            _film.value = Film.Printed(print, started)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fail(e, canRetry = true)
        }
    }

    private fun fail(e: Exception, canRetry: Boolean) {
        // Only our own messages reach the screen; anything else could name the provider.
        val message = (e as? Pipeline.PipelineException)?.message ?: "Processing failed."
        _film.value = Film.Failed(message, canRetry && lastSource != null, currentSheet())
    }

    private fun currentSheet(): Long = _film.value.sheetId ?: System.currentTimeMillis()

    fun retry() {
        val source = lastSource ?: return
        if (isBusy) return
        developJob = viewModelScope.launch {
            _film.value = Film.Developing(Stage.Reading, currentSheet())
            develop(source)
        }
    }

    /** Clear the film back to a blank sheet. */
    fun eject() {
        if (!isBusy) _film.value = Film.Blank
    }

    /** The last sheet whose eject animation has played, so it doesn't replay on tab switches. */
    private val _ejected = MutableStateFlow(0L)
    val ejected: StateFlow<Long> = _ejected.asStateFlow()

    fun markEjected(sheet: Long) {
        _ejected.value = sheet
    }

    fun markExposed(print: Print) {
        _exposed.update { it + print.id }
    }

    // ---- Gallery -------------------------------------------------------------------------

    /** Set a rating; tapping the one that's already set clears it. */
    fun rate(print: Print, rating: Rating) {
        val next = if (_ratings.value[print.id] == rating) null else rating
        printStore.setRating(print, next)
        _ratings.update { if (next == null) it - print.id else it + (print.id to next) }
    }

    fun deletePrint(print: Print) {
        printStore.delete(print)
        refreshPrints()
        val current = _film.value
        if (current is Film.Printed && current.print.id == print.id) _film.value = Film.Blank
        _ratings.update { it - print.id }
    }

    private fun refreshPrints() {
        _prints.value = printStore.all()
    }
}
