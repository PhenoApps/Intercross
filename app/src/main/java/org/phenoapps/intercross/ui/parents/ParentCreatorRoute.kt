package org.phenoapps.intercross.ui.parents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import java.util.UUID

@Composable
fun ParentCreatorRoute(mode: Int, onDone: () -> Unit, onOpenPollenGroup: (String, String) -> Unit, onShowMessage: (String) -> Unit) {
    val db = rememberDatabase()
    val parentModel: ParentsListViewModel = viewModel(factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())))
    val parents by parentModel.parents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    var code by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var name by remember { mutableStateOf("") }
    var bulk by remember { mutableStateOf(false) }
    val codeBlankError = stringResource(R.string.cross_id_cannot_be_blank)
    val parentExistsError = stringResource(R.string.parent_already_exists)

    ParentCreatorScreen(
        mode = mode,
        code = code,
        name = name,
        bulk = bulk,
        onCodeChange = { code = it },
        onNameChange = { name = it },
        onBulkChange = { bulk = !bulk },
        onSave = {
            val trimmed = code.trim()
            when {
                trimmed.isBlank() -> onShowMessage(codeBlankError)
                parents.any { it.codeId == trimmed } -> onShowMessage(parentExistsError)
                mode == 1 && bulk -> onOpenPollenGroup(trimmed, name.ifBlank { trimmed })
                else -> {
                    parentModel.insert(Parent(trimmed, mode, name.ifBlank { trimmed }))
                    onDone()
                }
            }
        },
        topBarState = TopBarState(
            titleRes = R.string.parent_creator_label,
            showBack = true,
            onBack = onDone,
        ),
    )
}
