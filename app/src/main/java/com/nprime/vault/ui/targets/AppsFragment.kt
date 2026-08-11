package com.nprime.vault.ui.targets

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nprime.vault.R
import com.nprime.vault.data.VaultPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsFragment : Fragment() {

    private val selected = mutableSetOf<String>()
    private val allItems = mutableListOf<AppItem>()
    private lateinit var adapter: AppAdapter

    data class AppItem(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_apps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler  = view.findViewById<RecyclerView>(R.id.apps_recycler)
        val progress  = view.findViewById<ProgressBar>(R.id.apps_progress)
        val search    = view.findViewById<SearchView>(R.id.apps_search)
        val tvCount   = view.findViewById<TextView>(R.id.tv_selection_count)

        selected.addAll(VaultPrefs.getSelectedApps(requireContext()))

        adapter = AppAdapter(selected) { pkg, checked ->
            if (checked) selected.add(pkg) else selected.remove(pkg)
            VaultPrefs.saveSelectedApps(requireContext(), selected.toList())
            tvCount.text = if (selected.isEmpty()) "" else "${selected.size} selected"
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                adapter.filter(q.orEmpty())
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            recycler.visibility = View.INVISIBLE
            progress.visibility = View.VISIBLE
            val apps = withContext(Dispatchers.IO) { loadUserApps() }
            allItems.addAll(apps)
            adapter.setItems(allItems)
            progress.visibility = View.GONE
            recycler.visibility = View.VISIBLE
            tvCount.text = if (selected.isEmpty()) "" else "${selected.size} selected"
        }
    }

    private fun loadUserApps(): List<AppItem> {
        val pm = requireContext().packageManager
        val myPkg = requireContext().packageName
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != myPkg }
            .mapNotNull { info ->
                runCatching {
                    AppItem(
                        pm.getApplicationLabel(info).toString(),
                        info.packageName,
                        pm.getApplicationIcon(info)
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
    }

    inner class AppAdapter(
        private val selected: MutableSet<String>,
        private val onToggle: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>() {

        private var items = listOf<AppItem>()
        private var filtered = listOf<AppItem>()

        fun setItems(list: List<AppItem>) { items = list; filtered = list; notifyDataSetChanged() }

        fun filter(q: String) {
            filtered = if (q.isBlank()) items
            else items.filter { it.label.contains(q, ignoreCase = true) }
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val label: TextView = view.findViewById(R.id.app_label)
            val pkg: TextView   = view.findViewById(R.id.app_package)
            val check: CheckBox = view.findViewById(R.id.app_check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        )

        override fun getItemCount() = filtered.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = filtered[position]
            holder.icon.setImageDrawable(item.icon)
            holder.label.text = item.label
            holder.pkg.text   = item.packageName
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = item.packageName in selected
            holder.check.setOnCheckedChangeListener { _, c -> onToggle(item.packageName, c) }
            holder.itemView.setOnClickListener { holder.check.toggle() }
        }
    }
}
