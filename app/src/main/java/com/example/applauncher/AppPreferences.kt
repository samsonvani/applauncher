package com.example.applauncher

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "app_launcher_prefs"
    private const val KEY_PACKAGES = "selected_packages"
    private const val KEY_DELAY_MS = "launch_delay_ms"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Сохраняем список пакетов (разделённых запятой)
    fun savePackages(context: Context, packages: List<String>) {
        prefs(context).edit().putString(KEY_PACKAGES, packages.joinToString(",")).apply()
    }

    fun loadPackages(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_PACKAGES, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    // Задержка в миллисекундах (по умолчанию 3000 мс)
    fun saveDelay(context: Context, delayMs: Long) {
        prefs(context).edit().putLong(KEY_DELAY_MS, delayMs).apply()
    }

    fun loadDelay(context: Context): Long =
        prefs(context).getLong(KEY_DELAY_MS, 3000L)
}
