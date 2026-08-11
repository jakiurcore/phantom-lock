package com.nprime.vault.ui.targets

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nprime.vault.R
import com.nprime.vault.data.VaultPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FilesFragment : Fragment() {

    private val selected = mutableSetOf<String>()
    private lateinit var adapter: FileAdapter

    data class FileItem(val name: String, val path: String, val isDir: Boolean)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_files, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.files_recycler)
        val progress = view.findViewById<ProgressBar>(R.id.files_progress)
        val tvCount  = view.findViewById<TextView>(R.id.tv_files_count)

        selected.addAll(VaultPrefs.getSelectedFiles(requireContext()))

        adapter = FileAdapter(selected) { path, checked ->
            if (checked) selected.add(path) else selected.remove(path)
            VaultPrefs.saveSelectedFiles(requireContext(), selected.toList())
            tvCount.text = if (selected.isEmpty()) "" else "${selected.size} selected"
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            val files = withContext(Dispatchers.IO) { loadRootEntries() }
            adapter.setItems(files)
            progress.visibility = View.GONE
            tvCount.text = if (selected.isEmpty()) "" else "${selected.size} selected"
        }
    }

    private fun loadRootEntries(): List<FileItem> {
        val root = Environment.getExternalStorageDirectory()
        return (root.listFiles() ?: emptyArray())
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { FileItem(it.name, it.absolutePath, it.isDirectory) }
    }

    inner class FileAdapter(
        private val selected: MutableSet<String>,
        private val onToggle: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<FileAdapter.VH>() {

        private var items = listOf<FileItem>()
        fun setItems(list: List<FileItem>) { items = list; notifyDataSetChanged() }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val icon: TextView  = view.findViewById(R.id.file_icon)
            val name: TextView  = view.findViewById(R.id.file_name)
            val path: TextView  = view.findViewById(R.id.file_path)
            val check: CheckBox = view.findViewById(R.id.file_check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.icon.text  = if (item.isDir) "📁" else "📄"
            holder.name.text  = item.name
            holder.path.text  = item.path
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = item.path in selected
            holder.check.setOnCheckedChangeListener { _, c -> onToggle(item.path, c) }
            holder.itemView.setOnClickListener { holder.check.toggle() }
        }
    }
}
