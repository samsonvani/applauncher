package com.example.applauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // ВАЖНО: используем device-protected context, чтобы сервис
        // мог читать настройки из хранилища до разблокировки
        val storageContext: Context =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }

        val serviceIntent = Intent(storageContext, LaunchService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            storageContext.startForegroundService(serviceIntent)
        } else {
            storageContext.startService(serviceIntent)
        }
    }
}
