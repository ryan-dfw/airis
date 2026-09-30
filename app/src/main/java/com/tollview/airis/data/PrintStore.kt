package com.tollview.airis.data

import android.content.Context
import java.io.File

/** One developed print: the generated image only, stored in the app's private files. */
data class Print(
    val id: String,
    val file: File,
    val takenAt: Long,
)

/** A thumbs up or down on a print. No rating is simply absent. */
enum class Rating { Like, Dislike }

/** Saves generated images to filesDir/prints and lists them newest first. */
class PrintStore(private val context: Context) {

    private val dir: File = File(context.filesDir, "prints").apply { mkdirs() }
    private val ratingPrefs = context.getSharedPreferences(RATINGS, Context.MODE_PRIVATE)

    /** Print id → rating, for every rated print. */
    fun ratings(): Map<String, Rating> =
        ratingPrefs.all.mapNotNull { (id, value) ->
            val rating = (value as? String)?.let { name -> Rating.entries.firstOrNull { it.name == name } }
            rating?.let { id to it }
        }.toMap()

    fun setRating(print: Print, rating: Rating?) {
        ratingPrefs.edit().apply {
            if (rating == null) remove(print.id) else putString(print.id, rating.name)
        }.apply()
    }

    fun all(): List<Print> =
        (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.startsWith(PREFIX) && it.length() > 0 }
            .mapNotNull { file ->
                val stamp = file.nameWithoutExtension.removePrefix(PREFIX).toLongOrNull()
                    ?: return@mapNotNull null
                Print(id = file.name, file = file, takenAt = stamp)
            }
            .sortedByDescending { it.takenAt }

    /** Writes the downloaded image bytes (GPT Image returns PNG) atomically. */
    fun save(bytes: ByteArray, takenAt: Long = System.currentTimeMillis()): Print {
        val ext = if (bytes.isJpeg()) "jpg" else "png"
        val file = File(dir, "$PREFIX$takenAt.$ext")
        val tmp = File(dir, "${file.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
        return Print(id = file.name, file = file, takenAt = takenAt)
    }

    fun delete(print: Print) {
        print.file.delete()
        setRating(print, null)
    }

    private fun ByteArray.isJpeg(): Boolean =
        size > 2 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte()

    private companion object {
        const val PREFIX = "print_"
        const val RATINGS = "airis_ratings"
    }
}
