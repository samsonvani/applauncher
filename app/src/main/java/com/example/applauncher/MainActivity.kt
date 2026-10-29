package com.example.applauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var delayEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        delayEdit = findViewById(R.id.delayEdit)

        // Загружаем сохранённые настройки
        val savedPackages = AppPreferences.loadPackages(this).toSet()
        val savedDelay = AppPreferences.loadDelay(this)
        delayEdit.setText((savedDelay / 1000).toString())

        // Получаем список установленных приложений (только с launch intent)
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(it.loadLabel(pm).toString(), it.packageName) }
            .sortedBy { it.label }

        adapter = AppListAdapter(apps, savedPackages) { selected ->
            // Сохраняем выбранные пакеты
            AppPreferences.savePackages(this, selected.toList())
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Кнопка сохранения задержки
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val seconds = delayEdit.text.toString().toLongOrNull() ?: 3L
            val delayMs = seconds * 1000
            AppPreferences.saveDelay(this, delayMs)
            Toast.makeText(this, "Задержка сохранена: $seconds сек", Toast.LENGTH_SHORT).show()
        }

        // Кнопка немедленного запуска (для теста)
        findViewById<Button>(R.id.launchNowButton).setOnClickListener {
            startLaunchService()
        }
    }

    private fun startLaunchService() {
        val intent = Intent(this, LaunchService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
