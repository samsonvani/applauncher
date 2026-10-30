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

    /**
     * Возвращает context с device-protected storage, если API позволяет.
     * На Android 7+ (API 24+) это работает всегда — но на всякий случай
     * оставляем проверку версии.
     */
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
     * Пользователи, у которых уже были настройки в обычном (credential) хранилище,
     * после обновления увидят пустые списки, потому что настройки теперь лежат
     * в device-protected storage. Эта функция копирует старые значения.
     *
     * Вызывается из MainActivity.onCreate — то есть только при наличии уже
     * разблокированного устройства.
     */
    @Suppress("UNCHECKED_CAST")
    fun migrateIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val deviceProtected = context.createDeviceProtectedStorageContext()
        val devicePrefs = deviceProtected.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Если в device-protected уже что-то есть — значит, миграция не нужна
        if (devicePrefs.all.isNotEmpty()) return

        val credentialPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (credentialPrefs.all.isEmpty()) return

        // Копируем все значения
        devicePrefs.edit()
            .putAll(credentialPrefs.all as Map<String, Any?>)
            .apply()

        // Опционально: старые данные можно почистить
        credentialPrefs.edit().clear().apply()
    }
}
