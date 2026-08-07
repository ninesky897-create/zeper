package com.zeper.player

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import com.zeper.player.BuildConfig
import kotlinx.coroutines.*

class ZeperApp : Application() {
    companion object {
        var isInitialized = false
    }

    override fun onCreate() {
        super.onCreate()
        
        // Anti-Hack: Prevent app from running if debuggable in production (extra safety)
        if (!BuildConfig.DEBUG) {
            // Security hardening could go here
        }

        // Initialize YoutubeDL and FFmpeg on background thread with robust error trapping
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@ZeperApp)
            } catch (e: Throwable) {
                Log.e("ZeperApp", "Failed to initialize YoutubeDL", e)
            }
            try {
                FFmpeg.getInstance().init(this@ZeperApp)
            } catch (e: Throwable) {
                Log.e("ZeperApp", "Failed to initialize FFmpeg", e)
            }
            isInitialized = true
        }
    }
}
