package com.geurys.enfoque

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnfoqueApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Precalienta la lista de apps en segundo plano: cuando se dibuja la pantalla ya está lista.
        CoroutineScope(Dispatchers.IO).launch { AppCache.load(this@EnfoqueApp) }
    }
}
