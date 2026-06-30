package org.phenoapps.intercross.ui.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.phenoapps.intercross.data.viewmodels.LabelTemplateViewModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.labels.LabelTemplateEditorScreen
import java.io.InputStreamReader

@Composable
fun LabelTemplateEditorRoute(
    onBack: () -> Unit = {},
    viewModel: LabelTemplateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val importZplFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@rememberLauncherForActivityResult
        val text = InputStreamReader(inputStream).use { reader ->
            reader.readLines().joinToString("\n")
        }
        viewModel.importRawZpl(text, uri.lastPathSegment)
    }

    LabelTemplateEditorScreen(
        uiState = uiState,
        onConfigChange = viewModel::updateConfig,
        onSelectSavedTemplate = { name, type -> viewModel.selectSavedTemplate(name, type) },
        onSaveTemplate = viewModel::saveCurrentTemplate,
        onImportZpl = { importZplFile.launch("*/*") },
        onDetectDpi = viewModel::detectDpi,
        onRenderPreview = viewModel::renderPreview,
        onMessageShown = viewModel::clearMessage,
        topBarState = TopBarState(
            titleRes = R.string.label_designer_title,
            showBack = true,
            onBack = onBack,
        ),
    )
}
