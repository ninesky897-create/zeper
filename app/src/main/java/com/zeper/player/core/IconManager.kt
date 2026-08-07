package com.zeper.player.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconManager {

    private const val PACKAGE_NAME = "com.zeper.player"
    private val ALIASES = listOf(
        "$PACKAGE_NAME.MainActivityLogo1",
        "$PACKAGE_NAME.MainActivityLogo2",
        "$PACKAGE_NAME.MainActivityLogo3",
        "$PACKAGE_NAME.MainActivityLogo4"
    )

    fun changeIcon(context: Context, logoType: String) {
        val pm = context.packageManager
        val targetAlias = when (logoType) {
            "logo1" -> ALIASES[0]
            "logo2" -> ALIASES[1]
            "logo3" -> ALIASES[2]
            "logo4" -> ALIASES[3]
            else -> ALIASES[0]
        }

        ALIASES.forEach { alias ->
            val state = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(context, alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
