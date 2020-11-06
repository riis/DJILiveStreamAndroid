package com.riis.livestream

import android.app.Application
import android.content.Context
import android.util.Log
import com.riis.livestream.fragments.MainFragment
import com.secneo.sdk.Helper

class LivestreamApplication: Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Log.d("displayService", "helper initializing...")
        // Helper needed for DJI App registration
        Helper.install(this)
    }
}