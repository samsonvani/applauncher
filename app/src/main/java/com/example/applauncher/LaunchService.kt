package com.example.applauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast

class LaunchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Защита от повторного запуска, пока сервис уже выполняется
        if (isRunning) return START_NOT_STICKY

        // Защита от двойного срабатывания после LOCKED_BOOT_COMPLETED + BOOT_COMPLETED.
        // Если сервис уже отрабатывал в этом же boot — не запускаем повторно.
        if (shouldSkipAsDuplicate()) {
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true

        // Проверка разрешения «Поверх других приложений» (SAW) — ключ для запуска
        // приложений из фона на Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)) {
            // На LOCKED_BOOT тост может не показаться на экране блокировки,
            // но код не упадёт. Пользователь увидит предупреждение после разблокировки.
            try {
                Toast.makeText(
                    this,
                    "Нет разрешения «Поверх других приложений». Откройте App Launcher и выдайте его.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) { /* игнорируем */ }
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification: Notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Запуск приложений")
                    .setContentText("Выполняется последовательный запуск...")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setContentTitle("Запуск приложений")
                    .setContentText("Выполняется последовательный запуск...")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .build()
            }
        startForeground(NOTIFICATION_ID, notification)

        val packages = AppPreferences.loadPackages(this)
        val delayMs = AppPreferences.loadDelay(this)

        if (packages.isEmpty()) {
            try {
                Toast.makeText(this, "Список приложений пуст", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { /* игнорируем */ }
            stopSelf()
            return START_NOT_STICKY
        }

        // Запоминаем, что в этом boot мы уже запускали —
        // чтобы BOOT_COMPLETED не дублировал LOCKED_BOOT_COMPLETED
        AppPreferences.saveLastLaunchElapsed(this, SystemClock.elapsedRealtime())

        launchSequentially(packages, delayMs)
        return START_NOT_STICKY
    }

    /**
     * Проверяет, был ли запуск уже в этом boot.
     *
     * Логика:
     *  - elapsedRealtime() сбрасывается при перезагрузке;
     *  - если сохранённое значение больше текущего — значит, был reboot,
     *    это новый boot, запускаем;
     *  - если сохранённое значение меньше текущего, но разница < 5 минут —
     *    это дубликат (LOCKED_BOOT + BOOT), пропускаем.
     */
    private fun shouldSkipAsDuplicate(): Boolean {
        val lastElapsed = AppPreferences.loadLastLaunchElapsed(this)
        if (lastElapsed < 0) return false          // первый запуск
        val current = SystemClock.elapsedRealtime()
        if (lastElapsed > current) return false    // был reboot — новый boot
        return (current - lastElapsed) < DUPLICATE_WINDOW_MS
    }

    private fun launchSequentially(packages: List<String>, delayMs: Long) {
        var index = 0

        fun launchNext() {
            if (index >= packages.size) {
                stopSelf()
                return
            }
            val packageName = packages[index]
            index++

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(launchIntent)
                } catch (e: Exception) {
                    try {
                        Toast.makeText(
                            this@LaunchService,
                            "Не удалось запустить $packageName: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) { /* игнорируем */ }
                }
            }
            handler.postDelayed({ launchNext() }, delayMs)
        }

        launchNext()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Запуск приложений",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "launch_channel"
        private const val NOTIFICATION_ID = 1
        private const val DUPLICATE_WINDOW_MS = 5 * 60 * 1000L  // 5 минут
    }
}
