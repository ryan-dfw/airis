package com.tollview.airis.pipeline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.tollview.airis.net.CellularNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * The AIris pipeline, ported 1:1 from the original MainActivity.java:
 *
 *  1. Poll the Sony Camera Remote API (getEvent) on the camera's Wi-Fi until a takePictureUrl shows up.
 *  2. Download the JPEG from the camera (default network = camera Wi-Fi).
 *  3. Caption it with gpt-4o-mini over the cellular network.
 *  4. Send "In a photorealistic style: <caption>" to the image model over cellular
 *     (originally dall-e-3; now gpt-image-1, since dall-e-3 was retired).
 *  5. Download the generated image over cellular.
 *
 * Same endpoints, payloads, prompt, models and network binding as the original.
 * What's new is only plumbing: coroutines instead of raw threads, the key comes from
 * on-device storage, errors are surfaced to the UI instead of swallowed, and polling
 * keeps retrying while the camera isn't reachable yet.
 */
class Pipeline(private val context: Context) {

    class PipelineException(message: String, cause: Throwable? = null) : Exception(message, cause)

    enum class CameraStatus { Searching, Connected }

    // ---- 1. Camera polling --------------------------------------------------------------

    /**
     * Polls every 500 ms until the camera reports a new takePictureUrl.
     * [skipUrl] is the last URL we already developed, so a repeated event is ignored.
     */
    suspend fun pollForPhotoUrl(
        skipUrl: String?,
        onStatus: (CameraStatus) -> Unit,
    ): String = withContext(Dispatchers.IO) { pollLoop(skipUrl, onStatus) }

    private suspend fun pollLoop(skipUrl: String?, onStatus: (CameraStatus) -> Unit): String {
        val payload = """
            {
                "method": "getEvent",
                "params": [false],
                "id": 1,
                "version": "1.0"
            }
        """.trimIndent()

        while (true) {
            coroutineContext.ensureActive()
            var found: String? = null
            try {
                val connection = URL(CAMERA_ENDPOINT).openConnection() as HttpURLConnection
                connection.connectTimeout = 3_000
                connection.readTimeout = 5_000
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                if (connection.responseCode == 200) {
                    onStatus(CameraStatus.Connected)
                    found = extractTakePictureUrl(connection.inputStream.readText())
                } else {
                    onStatus(CameraStatus.Searching)
                }
                connection.disconnect()
            } catch (e: Exception) {
                // Camera not reachable yet (not on its Wi-Fi, powered off...). Keep trying.
                onStatus(CameraStatus.Searching)
                delay(1_000)
                continue
            }
            if (found != null && found != skipUrl) {
                Log.d(TAG, "Photo URL: $found")
                return found
            }
            delay(500)
        }
    }

    /** result[5][0].takePictureUrl[i] — exactly where the original looked. */
    private fun extractTakePictureUrl(body: String): String? {
        val resultArray = JSONObject(body).optJSONArray("result") ?: return null
        if (resultArray.length() <= 5) return null
        val takePictureArray = resultArray.optJSONArray(5) ?: return null
        if (takePictureArray.length() == 0) return null
        val takePictureObject = takePictureArray.optJSONObject(0) ?: return null
        val urls = takePictureObject.optJSONArray("takePictureUrl") ?: return null
        for (i in 0 until urls.length()) {
            val url = urls.optString(i)
            if (!url.isNullOrEmpty()) return url
        }
        return null
    }

    // ---- 2. Download from camera -------------------------------------------------------------

    suspend fun downloadFromCamera(photoUrl: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            val connection = URL(photoUrl).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw PipelineException("Couldn't download the photo from the camera.")
            }
            connection.inputStream.use { it.readAllBytesCompat() }
        } catch (e: PipelineException) {
            throw e
        } catch (e: Exception) {
            throw PipelineException("Couldn't download the photo from the camera.", e)
        }
    }

    // ---- 3. Caption with gpt-4o-mini ---------------------------------------------------------

    suspend fun sendToOpenAI(imageBytes: ByteArray, apiKey: String): String = withContext(Dispatchers.IO) {
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val data = JSONObject()
        data.put("model", "gpt-4o-mini")

        val messagesArray = JSONArray()
        val textMessage = JSONObject()
            .put("type", "text")
            .put("text", CAPTION_PROMPT)
        val imageMessage = JSONObject()
            .put("type", "image_url")
            .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64Image"))
        messagesArray.put(textMessage)
        messagesArray.put(imageMessage)

        data.put(
            "messages", JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", messagesArray)
            )
        )
        Log.d("OpenAI", "Request payload length: ${data.toString().length}")

        val response = postJson(CHAT_URL, data, apiKey, "caption")
        val caption = JSONObject(response)
            .optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content", "")
        if (caption.isNullOrBlank()) throw PipelineException("Processing returned no image.")
        Log.d("OpenAI", "Caption: $caption")
        caption
    }

    // ---- 4. Generate with gpt-image-1 ---------------------------------------------------------

    /**
     * Same prompt as the original dall-e-3 call. OpenAI shut dall-e-3 down on May 12, 2026, so
     * this uses its replacement, gpt-image-1. GPT Image models return the PNG inline as
     * base64 (`b64_json`) rather than a hosted URL; a URL is still handled just in case.
     */
    suspend fun generateImage(caption: String, apiKey: String): ByteArray = withContext(Dispatchers.IO) {
        val data = JSONObject()
        data.put("model", IMAGE_MODEL)
        data.put("prompt", "In a photorealistic style: $caption")
        data.put("n", 1)
        data.put("size", "1024x1024")
        Log.d("Image", "Request: model=$IMAGE_MODEL, prompt length=${caption.length}")

        val response = postJson(IMAGES_URL, data, apiKey, "image")
        val first = JSONObject(response).optJSONArray("data")?.optJSONObject(0)
        val b64 = first?.optString("b64_json").orEmpty()
        val url = first?.optString("url").orEmpty()
        when {
            b64.isNotEmpty() -> try {
                Base64.decode(b64, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                throw PipelineException("Processing returned no image.", e)
            }
            url.isNotEmpty() -> downloadGenerated(url)
            else -> {
                Log.e("Image", "Response missing image data: ${response.take(500)}")
                throw PipelineException("Processing returned no image.")
            }
        }
    }

    // ---- 5. Download the generated image -----------------------------------------------------

    suspend fun downloadGenerated(imageUrl: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            val connection = openOnCellular(URL(imageUrl))
            connection.doInput = true
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw PipelineException("Processing failed (error ${connection.responseCode}).")
            }
            connection.inputStream.use { it.readAllBytesCompat() }
        } catch (e: PipelineException) {
            throw e
        } catch (e: Exception) {
            throw PipelineException("Lost the connection while processing.", e)
        }
    }

    // ---- Networking helpers ------------------------------------------------------------------

    /** Same lookup the original did: the first network with TRANSPORT_CELLULAR. */
    @Suppress("DEPRECATION")
    private fun cellularNetwork(): Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return network
            }
        }
        return null
    }

    /**
     * Opens a connection on cellular, bypassing Android's default network (which is the
     * camera's internet-less Wi-Fi). Prefers the network held app-wide by [CellularNetwork],
     * waiting briefly for it to come up; then the original allNetworks scan; and only if the
     * phone has no cellular at all (Wi-Fi-only tablet, airplane mode) the default network.
     * Always called on Dispatchers.IO.
     */
    private fun openOnCellular(url: URL): HttpURLConnection {
        val cellular = CellularNetwork.await() ?: cellularNetwork()
        if (cellular == null) Log.w(TAG, "No cellular network; using default for $url")
        return (cellular?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
    }

    private fun postJson(url: String, body: JSONObject, apiKey: String, what: String): String {
        val connection = try {
            openOnCellular(URL(url)).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        } catch (e: Exception) {
            throw PipelineException(
                "Couldn't reach the processing server. Make sure mobile data is on.",
                e
            )
        }

        val code = try {
            connection.responseCode
        } catch (e: Exception) {
            throw PipelineException("Lost the connection while processing.", e)
        }
        if (code == 200) return connection.inputStream.readText()

        val error = connection.errorStream?.readText().orEmpty()
        Log.e(TAG, "Error: HTTP $code - $error")
        val friendly = when (code) {
            401 -> "The API key was rejected. Check it in Setup."
            429 -> "Usage limit reached. Check your account, then try again."
            else -> "Processing failed (error $code)."
        }
        // The raw server message is logged above but never shown; it names the provider.
        throw PipelineException(friendly)
    }

    private fun InputStream.readText(): String = bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun InputStream.readAllBytesCompat(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    companion object {
        private const val TAG = "AIris"
        const val CAMERA_ENDPOINT = "http://10.0.0.1:10000/sony/camera"
        private const val CHAT_URL = "https://api.openai.com/v1/chat/completions"
        private const val IMAGES_URL = "https://api.openai.com/v1/images/generations"
        const val IMAGE_MODEL = "gpt-image-1"
        const val CAPTION_PROMPT =
            "Describe the image in detail. If there are people in the image, describe their appearance, facial expression, and pose (if visible). Make note of the image's composition. If the facial expression or pose is extreme, feel free to describe it as such."
    }
}
