package com.example.applauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        // Логируем КАЖДЫЙ пришедший action — это ключ к диагностике
        Log.d(TAG, "Received action: $action")

        // Набор экшенов, на которые реагируем (по образцу ru.fmplay + расширения для ГУ)
        val bootActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "com.fyt.boot.ACCON",
            "android.intent.action.ACTION_MT_COMMAND_SLEEP_OUT",
            "android.intent.action.ACTION_BOOT_COMPLETED"
        )

        if (action !in bootActions) {
            Log.d(TAG, "Skipping action: $action")
            return
        }

        Log.i(TAG, "Starting LaunchService for action: $action")

        // Используем device-protected context, чтобы сервис мог читать настройки
        // даже до разблокировки (для LOCKED_BOOT_COMPLETED)
        val storageContext: Context =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }

        val serviceIntent = Intent(storageContext, LaunchService::class.java)
        serviceIntent.putExtra("boot_action", action)  // для диагностики в сервисе

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                storageContext.startForegroundService(serviceIntent)
            } else {
                storageContext.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LaunchService", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
