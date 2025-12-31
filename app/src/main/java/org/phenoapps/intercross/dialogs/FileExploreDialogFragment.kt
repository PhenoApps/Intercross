package org.phenoapps.intercross.dialogs

import org.phenoapps.intercross.adapters.FileAdapter
import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.intercross.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class FileExploreDialogFragment : DialogFragment(), CoroutineScope by MainScope() {

    companion object {
        const val TAG = "FileExploreDialogFragment"
    }

    private var mainListView: RecyclerView? = null
    private var progressBar: ProgressBar? = null

    // Stores names of traversed directories
    private val str = ArrayList<String?>()

    // Check if the first level of the directory structure is the one showing
    private var firstLvl = true
    private var fileList = mutableListOf<Item>()
    private var chosenFile: String? = null

    private var onFileSelectedListener: ((Uri) -> Unit)? = null

    // updates whenever we traverse a directory, loads the current level's files
    private var path: DocumentFile? = null

    private var exclude: Array<String>? = null
    private var include: Array<String>? = null

    // Wrapper class to hold file data
    class Item(var file: String, var isDir: Boolean, var icon: Int) {
        override fun toString(): String {
            return file
        }
    }

    private val mComparator = java.util.Comparator { f1: Item, f2: Item ->
        if (f1.isDir && !f2.isDir) {
            // Directory before non-directory
            return@Comparator -1
        } else if (!f1.isDir && f2.isDir) {
            // Non-directory after directory
            return@Comparator 1
        } else {
            // Alphabetic order otherwise
            return@Comparator f1.file.compareTo(f2.file, ignoreCase = true)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity?.layoutInflater
        val view = inflater?.inflate(R.layout.dialog_file_explorer, null)

        mainListView = view?.findViewById(R.id.fileRecyclerView)
        progressBar = view?.findViewById(R.id.progressBar)

        val fileUri = arguments?.getString(getString(R.string.path))?.toUri()
            ?: return showPathError()

        path = DocumentFile.fromTreeUri(requireContext(), fileUri)
        exclude = arguments?.getStringArray(getString(R.string.exclude))
        include = arguments?.getStringArray(getString(R.string.include))
        val dialogTitle = arguments?.getString(getString(R.string.dialog_title))

        val builder = AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setView(view)

        if (path != null) {
            loadFilesProgress(path)
        }

        setupRecyclerView()

        return builder.create()
    }

    private fun showPathError() =
        AlertDialog.Builder(context)
            .setTitle(R.string.error)
            .setMessage(R.string.error_opening_path_message)
            .create()

    private fun setupRecyclerView() {
        // set the click action for each type of item
        val fileAdapter = FileAdapter { item ->
            chosenFile = item.file
            val file = path?.findFile(chosenFile!!)
            if (file != null && file.exists() && file.isDirectory) {
                firstLvl = false

                // Adds chosen directory to list
                str.add(chosenFile)
                fileList.clear()
                path = file

                loadFilesProgress(file)

                mainListView?.adapter?.notifyDataSetChanged()
            } else if (chosenFile.equals(getString(R.string.activity_file_explorer_up_directory_name)) && (file == null || !file.exists())) {
                // for "up" dummy folder

                // present directory removed from list
                str.removeAt(str.size - 1)

                // path modified to exclude present directory
                path = path?.parentFile

                fileList.clear()

                // if there are no more directories in the list, then
                // its the first level
                if (str.isEmpty()) {
                    firstLvl = true
                }

                loadFilesProgress(path)
                return@FileAdapter
            } else if (file != null && file.isFile) {
                onFileSelectedListener?.invoke(file.uri)
                dismiss()
            }


        }

        mainListView?.adapter = fileAdapter
        mainListView?.layoutManager = LinearLayoutManager(context)
    }

    private fun loadFilesProgress(path: DocumentFile?) {
        progressBar?.visibility = View.VISIBLE
        mainListView?.visibility = View.INVISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                loadFileList(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                progressBar?.visibility = View.GONE
                mainListView?.visibility = View.VISIBLE
                (mainListView?.adapter as? FileAdapter)?.apply {
                    submitList(fileList)
                    notifyDataSetChanged()
                }
            }
        }
    }


    private fun loadFileList(path: DocumentFile?) {
        if (path?.exists() == true) {
            val files = path.listFiles()
            val excludedExtensions = exclude?.toList() ?: emptyList()
            val includedExtensions = include?.toList() ?: emptyList()

            for (file in files) {
                val name = file.name ?: continue

                // Skip asset files
                if (name.contains(".intercross")) continue

                // Check extensions only for files
                val extension = name.substringAfterLast('.', "").lowercase()
                if (extension in excludedExtensions) continue

                // only add files with included extensions
                if (includedExtensions.contains(extension) || file.isDirectory) {
                    val icon = when {
                        name.lowercase().endsWith(".csv") -> R.drawable.ic_file_csv
                        name.endsWith(".xls") -> R.drawable.ic_file_xls
                        name.endsWith(".zip") -> R.drawable.ic_file_zip
                        file.isDirectory -> R.drawable.ic_file_directory
                        else -> R.drawable.ic_file_generic
                    }
                    fileList.add(Item(name, file.isDirectory, icon))
                }
            }

            fileList.sortWith(mComparator)

            if (!firstLvl) {
                fileList.add(
                    0, Item(
                        getString(R.string.activity_file_explorer_up_directory_name),
                        true,
                        R.drawable.ic_file_up_dir
                    )
                )
            }
        }
    }

    fun setOnFileSelectedListener(listener: (Uri) -> Unit) {
        onFileSelectedListener = listener
    }
}