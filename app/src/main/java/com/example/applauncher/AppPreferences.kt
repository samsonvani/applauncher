package com.example.applauncher

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

/**
 * Хранилище настроек в device-protected storage — доступно ДО разблокировки
 * устройства (после LOCKED_BOOT_COMPLETED).
 */
object AppPreferences {

    private const val PREFS_NAME = "app_launcher_prefs"
    private const val KEY_PACKAGES = "selected_packages"
    private const val KEY_DELAY_MS = "launch_delay_ms"
    private const val KEY_LAST_LAUNCH_ELAPSED = "last_launch_elapsed"

    private fun storageContext(context: Context): Context =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }

    private fun prefs(context: Context): SharedPreferences =
        storageContext(context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // -------- Пакеты --------

    fun savePackages(context: Context, packages: List<String>) {
        prefs(context).edit()
            .putString(KEY_PACKAGES, packages.joinToString(","))
            .apply()
    }

    fun loadPackages(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_PACKAGES, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    // -------- Задержка --------

    fun saveDelay(context: Context, delayMs: Long) {
        prefs(context).edit().putLong(KEY_DELAY_MS, delayMs).apply()
    }

    fun loadDelay(context: Context): Long =
        prefs(context).getLong(KEY_DELAY_MS, 3000L)

    // -------- Последний запуск (для защиты от двойного срабатывания) --------

    fun saveLastLaunchElapsed(context: Context, elapsedMs: Long) {
        prefs(context).edit().putLong(KEY_LAST_LAUNCH_ELAPSED, elapsedMs).apply()
    }

    fun loadLastLaunchElapsed(context: Context): Long =
        prefs(context).getLong(KEY_LAST_LAUNCH_ELAPSED, -1L)

    // -------- Миграция из старого credential-protected хранилища --------

    /**
     * Копирует настройки из обычного (credential-protected) хранилища
     * в device-protected. Вызывается из MainActivity.onCreate — то есть
     * только когда устройство уже разблокировано.
     */
    fun migrateIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val deviceProtected = context.createDeviceProtectedStorageContext()
        val devicePrefs = deviceProtected.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Если в device-protected уже что-то есть — миграция не нужна
        if (devicePrefs.all.isNotEmpty()) return

        val credentialPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (credentialPrefs.all.isEmpty()) return

        // Явная копия по типам — обходит проблемы с Editor.putAll(Map<String, *>)
        // в некоторых версиях Kotlin/AGP
        val editor = devicePrefs.edit()
        for ((key, value) in credentialPrefs.all) {
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as Set<String>)
                }
            }
        }
        editor.apply()

        // Старые данные больше не нужны
        credentialPrefs.edit().clear().apply()
    }
}
