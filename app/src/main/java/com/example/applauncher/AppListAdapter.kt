package com.example.applauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val apps: List<AppInfo>,
    initialSelected: Set<String>,
    private val onSelectionChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private val selected = initialSelected.toMutableSet()

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
        holder.checkBox.isChecked = selected.contains(app.packageName)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selected.add(app.packageName)
            else selected.remove(app.packageName)
            onSelectionChanged(selected)
        }
    }

    override fun getItemCount() = apps.size
}
