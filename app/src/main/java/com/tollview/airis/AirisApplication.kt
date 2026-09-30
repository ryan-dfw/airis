package com.tollview.airis

import android.app.Application
import com.tollview.airis.net.CellularNetwork

class AirisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hold the cellular data path for the whole process, not just while a screen is visible.
        CellularNetwork.start(this)
    }
}
