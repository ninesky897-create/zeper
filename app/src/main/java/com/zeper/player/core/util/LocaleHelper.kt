package com.zeper.player.core.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context, language: String): Context {
        val locale = if (language == "system") {
            Locale.getDefault()
        } else {
            Locale(language)
        }
        
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            configuration.locale = locale
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
}
