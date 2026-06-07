package org.phenoapps.intercross.fragments

// removed unused ProgressDialog import
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.viewmodels.LabelTemplateViewModel
import org.phenoapps.intercross.ui.labels.LabelTemplateEditorScreen
import org.phenoapps.intercross.ui.theme.AppTheme
import java.io.InputStreamReader

/**
 * Fragment hosting the Compose-based `LabelTemplateEditorScreen`.
 *
 * The fragment wires up activity-level components (import file chooser, action bar)
 * and forwards user interactions to the `LabelTemplateViewModel`.
 */
@AndroidEntryPoint
class LabelTemplateEditorFragment : Fragment() {

    private val viewModel: LabelTemplateViewModel by viewModels()

    private val importZplFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val inputStream = requireContext().contentResolver.openInputStream(uri)
            ?: return@registerForActivityResult
        val text = InputStreamReader(inputStream)
            .readLines()
            .joinToString("\n")
        viewModel.importRawZpl(text, uri.lastPathSegment)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val uiState by viewModel.uiState.collectAsState()

                    LabelTemplateEditorScreen(
                        uiState = uiState,
                        onConfigChange = viewModel::updateConfig,
                        onSelectSavedTemplate = { name, type -> viewModel.selectSavedTemplate(name, type) },
                        onSaveTemplate = viewModel::saveCurrentTemplate,
                        onImportZpl = { importZplFile.launch("*/*") },
                        onDetectDpi = viewModel::detectDpi,
                        onRenderPreview = viewModel::renderPreview,
                        onMessageShown = viewModel::clearMessage,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.setBackButtonToolbar()
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = getString(R.string.label_designer_title)
            show()
        }
    }
}
