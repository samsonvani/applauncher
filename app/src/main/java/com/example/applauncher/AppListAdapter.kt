package com.example.applauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val selected: Set<String>,
    private val onToggle: (packageName: String, isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val label: TextView = view.findViewById(R.id.label)
        val pkg: TextView = view.findViewById(R.id.pkg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.label.text = app.label
        holder.pkg.text = app.packageName

        // Только устанавливаем состояние — без слушателя.
        // Слушатель ставится на всю строку ниже и пересоздаётся каждый биндинг.
        holder.checkBox.isChecked = selected.contains(app.packageName)

        // Вешаем клик на всю строку: тап где угодно = переключение
        holder.itemView.setOnClickListener {
            // Читаем актуальное состояние из selected, а не из View
            val currentlySelected = selected.contains(app.packageName)
            val newState = !currentlySelected

            // Обновляем визуально
            holder.checkBox.isChecked = newState

            // Сообщаем наружу — там обновят selected и вызовут нужные notify
            onToggle(app.packageName, newState)
        }
    }

    override fun getItemCount() = apps.size
}
