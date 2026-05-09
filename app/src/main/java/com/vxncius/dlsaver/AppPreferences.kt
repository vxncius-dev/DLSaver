package com.vxncius.dlsaver

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "dlsaver_prefs"
    private const val KEY_SIMULTANEOUS_DOWNLOADS = "simultaneous_downloads"
    private const val KEY_PLAYER_MONOCHROMATIC = "player_monochromatic"
    private const val KEY_LABS_MODE = "labs_mode"
    private const val KEY_MEDIA_PERMISSION_REQUESTED = "media_permission_requested"

    fun getSimultaneousDownloadsLimit(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SIMULTANEOUS_DOWNLOADS, 4)
            .coerceIn(1, 4)
    }

    fun setSimultaneousDownloadsLimit(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SIMULTANEOUS_DOWNLOADS, value.coerceIn(1, 4))
            .apply()
    }

    fun isPlayerMonochromatic(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PLAYER_MONOCHROMATIC, false)
    }

    fun setPlayerMonochromatic(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PLAYER_MONOCHROMATIC, enabled)
            .apply()
    }

    fun isLabsModeEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LABS_MODE, false)
    }

    fun setLabsModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LABS_MODE, enabled)
            .apply()
    }

    fun wasMediaPermissionRequested(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MEDIA_PERMISSION_REQUESTED, false)
    }

    fun markMediaPermissionRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MEDIA_PERMISSION_REQUESTED, true)
            .apply()
    }
}
