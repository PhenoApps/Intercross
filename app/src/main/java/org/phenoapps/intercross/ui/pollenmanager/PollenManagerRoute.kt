package org.phenoapps.intercross.ui.pollenmanager

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
import org.phenoapps.intercross.data.PollenGroupRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.PollenGroupListViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase

@Composable
fun PollenManagerRoute(
    codeId: String,
    readableName: String,
    onDone: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val db = rememberDatabase()
    val parentModel: ParentsListViewModel = viewModel(factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())))
    val groupModel: PollenGroupListViewModel = viewModel(factory = PollenGroupListViewModelFactory(PollenGroupRepository.getInstance(db.pollenGroupDao())))
    val males by parentModel.males.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val normalizedCode = remember(codeId) { codeId.trim() }
    val normalizedName = remember(readableName, normalizedCode) { readableName.ifBlank { normalizedCode } }
    var selectedMaleIds by remember { mutableStateOf(emptySet<Long>()) }
    val codeBlankError = stringResource(R.string.cross_id_cannot_be_blank)

    val allIds = males.mapNotNull { it.id }.toSet()
    val topBarState = TopBarState(
        titleRes = R.string.add_male_group,
        showBack = true,
        onBack = onDone,
        actions = listOf(
            TopBarAction("pollen_select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all, onClick = {
                selectedMaleIds = if (selectedMaleIds == allIds) emptySet() else allIds
            }),
        ),
    )

    PollenManagerScreen(
        normalizedCode = normalizedCode,
        normalizedName = normalizedName,
        males = males,
        selectedMaleIds = selectedMaleIds,
        onSelectAll = { selectedMaleIds = males.mapNotNull { it.id }.toSet() },
        onClearAll = { selectedMaleIds = emptySet() },
        onToggleMale = { maleId ->
            selectedMaleIds = if (maleId in selectedMaleIds) {
                selectedMaleIds - maleId
            } else {
                selectedMaleIds + maleId
            }
        },
        onSave = {
            if (normalizedCode.isBlank()) {
                onShowMessage(codeBlankError)
                return@PollenManagerScreen
            }

            val selectedGroups = males
                .filter { it.id != null && it.id in selectedMaleIds }
                .distinctBy { it.id }
                .mapNotNull { male ->
                    male.id?.let { PollenGroup(normalizedCode, normalizedName, it) }
                }

            if (selectedGroups.isEmpty()) {
                parentModel.insert(Parent(normalizedCode, 1, normalizedName))
            } else {
                groupModel.insert(*selectedGroups.toTypedArray())
            }
            onDone()
        },
        topBarState = topBarState,
    )
}
