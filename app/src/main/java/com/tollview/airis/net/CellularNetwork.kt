package com.tollview.airis.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.SystemClock
import android.util.Log

/**
 * Process-wide hold on the cellular data path.
 *
 * When the phone is associated with the camera's Wi-Fi (which has no internet), Android
 * may tear down mobile data, or never bring it up. Filing a NetworkRequest for
 * TRANSPORT_CELLULAR + NET_CAPABILITY_INTERNET makes the system keep a cellular network
 * up for us, and hands us its [Network] so every OpenAI request can open its
 * connection on it directly instead of on whatever Android picked as the default.
 *
 * Started once from [com.tollview.airis.AirisApplication.onCreate] and deliberately never
 * unregistered: it lives for the whole process, including while the app is backgrounded.
 */
object CellularNetwork {

    private const val TAG = "CellularNetwork"
    private val lock = Object()

    @Volatile
    private var started = false

    /** The live cellular network, or null while it's coming up / lost. */
    @Volatile
    var network: Network? = null
        private set

    fun start(context: Context) {
        if (started) return
        synchronized(lock) {
            if (started) return
            started = true
        }

        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Cellular available: $network")
                synchronized(lock) {
                    this@CellularNetwork.network = network
                    lock.notifyAll()
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Cellular lost: $network")
                synchronized(lock) {
                    if (this@CellularNetwork.network == network) this@CellularNetwork.network = null
                }
            }

            override fun onUnavailable() {
                Log.w(TAG, "Cellular unavailable")
            }
        }

        try {
            // No timeout, no unregister: the request (and the radio) stays held for the process lifetime.
            cm.requestNetwork(request, callback)
        } catch (e: Exception) {
            // e.g. SecurityException if CHANGE_NETWORK_STATE were missing, or too many requests.
            Log.e(TAG, "requestNetwork failed", e)
            synchronized(lock) { started = false }
        }
    }

    /**
     * Returns the held cellular network, blocking the calling (background) thread for up to
     * [timeoutMs] if it isn't up yet. Null if it never shows up. Never call on the main thread.
     */
    fun await(timeoutMs: Long = 10_000): Network? {
        network?.let { return it }
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        synchronized(lock) {
            while (network == null) {
                val remaining = deadline - SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                try {
                    lock.wait(remaining)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
            return network
        }
    }
}
