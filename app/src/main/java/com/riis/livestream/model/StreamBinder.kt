package com.riis.livestream.model

import android.os.Binder

class StreamBinder(private val service: DisplayService): Binder() {
    fun getService(): DisplayService {
        return service
    }
}