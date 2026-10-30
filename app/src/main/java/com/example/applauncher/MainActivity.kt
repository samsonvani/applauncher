package com.example.applauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private val selectedPackages = linkedSetOf<String>()

    private lateinit var allApps: List<AppInfo>
    private lateinit var allAppsAdapter: AppListAdapter
    private lateinit var selectedAppsAdapter: SelectedAppsAdapter

    private lateinit var delayEdit: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var allRecycler: RecyclerView
    private lateinit var selectedRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         // Миграция старых настроек в device-protected storage
        AppPreferences.migrateIfNeeded(this)

        delayEdit = findViewById(R.id.delayEdit)
        tabLayout = findViewById(R.id.tabLayout)
        allRecycler = findViewById(R.id.allAppsRecycler)
        selectedRecycler = findViewById(R.id.selectedAppsRecycler)

        // Восстанавливаем сохранённое
        selectedPackages.addAll(AppPreferences.loadPackages(this))
        delayEdit.setText((AppPreferences.loadDelay(this) / 1000).toString())

        // Список установленных приложений с launcher-интентом
        val pm = packageManager
        allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(it.loadLabel(pm).toString(), it.packageName) }
            .sortedBy { it.label.lowercase() }

        // Адаптеры
        allAppsAdapter = AppListAdapter(allApps, selectedPackages) { pkg, checked ->
            if (checked) selectedPackages.add(pkg) else selectedPackages.remove(pkg)
            saveSelection()
            selectedAppsAdapter.notifyDataSetChanged()
        }

        selectedAppsAdapter = SelectedAppsAdapter(selectedPackages, allApps) { pkg ->
            selectedPackages.remove(pkg)
            saveSelection()
            allAppsAdapter.notifyDataSetChanged()
            selectedAppsAdapter.notifyDataSetChanged()
        }

        allRecycler.layoutManager = LinearLayoutManager(this)
        allRecycler.adapter = allAppsAdapter

        selectedRecycler.layoutManager = LinearLayoutManager(this)
        selectedRecycler.adapter = selectedAppsAdapter

        // Табы
        tabLayout.addTab(tabLayout.newTab().setText(getTabSelectedTitle()))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_all))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val showSelected = tab.position == 0
                selectedRecycler.visibility = if (showSelected) View.VISIBLE else View.GONE
                allRecycler.visibility = if (showSelected) View.GONE else View.VISIBLE
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Кнопка сохранения задержки
        findViewById<Button>(R.id.saveDelayButton).setOnClickListener {
            val seconds = delayEdit.text.toString().toLongOrNull() ?: 3L
            if (seconds < 0) {
                Toast.makeText(this, "Задержка не может быть отрицательной", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppPreferences.saveDelay(this, seconds * 1000)
            Toast.makeText(this, "Задержка: $seconds сек", Toast.LENGTH_SHORT).show()
        }

        // Кнопка «Поверх других приложений»
        findViewById<Button>(R.id.overlayButton).setOnClickListener {
            openOverlaySettings()
        }

        // Кнопка «Запустить сейчас»
        findViewById<Button>(R.id.launchNowButton).setOnClickListener {
            if (!hasOverlayPermission()) {
                Toast.makeText(
                    this,
                    "Сначала включите «Поверх других приложений» — иначе сработает только первое приложение",
                    Toast.LENGTH_LONG
                ).show()
                openOverlaySettings()
                return@setOnClickListener
            }
            if (selectedPackages.isEmpty()) {
                Toast.makeText(this, "Не выбрано ни одного приложения", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startLaunchService()
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем заголовок таба после возврата из настроек
        updateSelectedTabTitle()
        // Если разрешение только что выдано — можно сразу подсказать
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasOverlayPermission()) {
            Toast.makeText(
                this,
                "Разрешение «Поверх других приложений» не выдано — запустится только одно приложение",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // -------- Вспомогательные --------

    private fun saveSelection() {
        AppPreferences.savePackages(this, selectedPackages.toList())
        updateSelectedTabTitle()
    }

    private fun getTabSelectedTitle(): String = "Выбранные (${selectedPackages.size})"

    private fun updateSelectedTabTitle() {
        tabLayout.getTabAt(0)?.text = getTabSelectedTitle()
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "На этой версии Android разрешение не требуется", Toast.LENGTH_SHORT).show()
            return
        }
        // Пытаемся открыть страницу именно нашего приложения
        val specific = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(specific)
        } catch (_: Exception) {
            // Фолбэк — общий экран «Поверх других приложений»
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Откройте вручную: Настройки → Приложения → App Launcher → Отображение поверх других приложений",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startLaunchService() {
        val intent = Intent(this, LaunchService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
