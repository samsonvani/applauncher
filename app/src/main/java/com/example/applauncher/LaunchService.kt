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
import android.widget.Toast

class LaunchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_NOT_STICKY
        isRunning = true

        // Создаём foreground-уведомление (обязательно для Android 8+)
        createNotificationChannel()
        val notification: Notification = Notification.Builder(this, "launch_channel")
            .setContentTitle("Запуск приложений")
            .setContentText("Выполняется последовательный запуск...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, notification)

        val packages = AppPreferences.loadPackages(this)
        val delayMs = AppPreferences.loadDelay(this)

        if (packages.isEmpty()) {
            Toast.makeText(this, "Список приложений пуст", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        // Запускаем последовательно с заданной задержкой
        launchSequentially(packages, delayMs)

        return START_NOT_STICKY
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

            // Запуск приложения по package name
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(launchIntent)
                } catch (e: Exception) {
                    // На Android 10+ может потребоваться разрешение или foreground-сервис
                    Toast.makeText(
                        this@LaunchService,
                        "Не удалось запустить $packageName: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Планируем следующий запуск через delayMs
            handler.postDelayed({ launchNext() }, delayMs)
        }

        launchNext()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "launch_channel",
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
}
