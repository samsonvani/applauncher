package com.example.applauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SelectedAppsAdapter(
    private val selected: Set<String>,
    private val allApps: List<AppInfo>,
    private val onRemove: (packageName: String) -> Unit
) : RecyclerView.Adapter<SelectedAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.label)
        val pkg: TextView = view.findViewById(R.id.pkg)
        val removeButton: ImageButton = view.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // toList() — чтобы гарантированно сохранить порядок и иметь доступ по индексу
        val packageName = selected.toList()[position]
        val app = allApps.firstOrNull { it.packageName == packageName }

        holder.label.text = app?.label ?: packageName
        holder.pkg.text = packageName

        holder.removeButton.setOnClickListener {
            onRemove(packageName)
        }
    }

    override fun getItemCount() = selected.size
}
