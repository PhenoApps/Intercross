package org.phenoapps.intercross.ui.wishlist

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.PollenGroupRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.PollenGroupListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.theme.AppTheme

private enum class WishlistFactoryStep {
    Female,
    Male,
    Wishes,
    Summary,
}

@Composable
fun WishlistFactoryRoute(
    onDone: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val db = rememberDatabase()
    val parentsModel: ParentsListViewModel = viewModel(
        factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())),
    )
    val groupModel: PollenGroupListViewModel = viewModel(
        factory = PollenGroupListViewModelFactory(PollenGroupRepository.getInstance(db.pollenGroupDao())),
    )
    val metadataModel: MetadataViewModel = viewModel(
        factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())),
    )
    val wishlistModel: WishlistViewModel = viewModel(
        factory = WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao())),
    )

    val females by parentsModel.females.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val males by parentsModel.males.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val groups by groupModel.groups.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metadata by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    val metaIcons = remember(metadata) {
        metadata.associate { it.property to it.icon }
    }

    var step by remember { mutableStateOf(WishlistFactoryStep.Female) }
    var selectedFemale by remember { mutableStateOf<WishParentOption?>(null) }
    var selectedMale by remember { mutableStateOf<WishParentOption?>(null) }
    val pendingWishes = remember { mutableStateListOf<Wishlist>() }

    BackHandler(enabled = true) {
        when (step) {
            WishlistFactoryStep.Female -> onBack()
            WishlistFactoryStep.Male -> step = WishlistFactoryStep.Female
            WishlistFactoryStep.Wishes -> step = WishlistFactoryStep.Male
            WishlistFactoryStep.Summary -> step = WishlistFactoryStep.Wishes
        }
    }

    // Lifted WishValuesStep state so it survives back/forward navigation
    val wishDrafts = remember { mutableStateListOf<WishDraft>() }
    var wishDraftsInitializedKey by remember { mutableStateOf<String?>(null) }
    val crossType = stringResource(R.string.literal_cross)

    val femaleRequiredMessage = stringResource(R.string.frag_wf_a_female_must_be_chosen)

    val topBarState = TopBarState(
        titleRes = R.string.frag_wf_toolbar_title,
        showBack = true,
        onBack = {
            when (step) {
                WishlistFactoryStep.Female -> onBack()
                WishlistFactoryStep.Male -> step = WishlistFactoryStep.Female
                WishlistFactoryStep.Wishes -> step = WishlistFactoryStep.Male
                WishlistFactoryStep.Summary -> step = WishlistFactoryStep.Wishes
            }
        }
    )

    @OptIn(ExperimentalMaterial3Api::class)
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(topBarState.title ?: topBarState.titleRes?.let { stringResource(it) }.orEmpty())
                },
                navigationIcon = {
                    if (topBarState.showBack) {
                        IconButton(onClick = { topBarState.onBack?.invoke() }) {
                            Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.navigate_back))
                        }
                    }
                },
                actions = {
                    topBarState.actions.forEach { action ->
                        if (action.iconRes != null) {
                            IconButton(onClick = { action.onClick?.invoke() }) {
                                Icon(painterResource(action.iconRes), contentDescription = stringResource(action.labelRes))
                            }
                        } else {
                            TextButton(onClick = { action.onClick?.invoke() }) {
                                Text(stringResource(action.labelRes))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.primary,
                    titleContentColor = AppTheme.colors.surface.topBarContentColor,
                    actionIconContentColor = AppTheme.colors.surface.topBarContentColor,
                    navigationIconContentColor = AppTheme.colors.surface.topBarContentColor,
                ),
            )
        },
    ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding).imePadding()) {
    when (step) {
        WishlistFactoryStep.Female -> {
            ParentChoiceStep(
                summary = stringResource(R.string.frag_wf_choose_female_summary),
                options = females
                    .filter { it.name.isNotBlank() }
                    .map { WishParentOption(it.codeId, it.name) }
                    .distinctBy { it.codeId },
                selected = selectedFemale,
                onSelected = { selectedFemale = it },
                onNext = {
                    if (selectedFemale == null) {
                        onShowMessage(femaleRequiredMessage)
                    } else {
                        step = WishlistFactoryStep.Male
                    }
                },
            )
        }

        WishlistFactoryStep.Male -> {
            val female = selectedFemale
            ParentChoiceStep(
                summary = stringResource(R.string.frag_wf_male_choice_summary, female?.name.orEmpty()),
                options = (listOf(WishParentOption("-1", "blank")) +
                    males.filter { it.name.isNotBlank() }.map { WishParentOption(it.codeId, it.name) } +
                    groups.filter { it.name.isNotBlank() }.map { WishParentOption(it.codeId, it.name) })
                    .distinctBy { it.codeId },
                selected = selectedMale,
                onSelected = { selectedMale = it },
                onBack = { step = WishlistFactoryStep.Female },
                onNext = {
                    if (selectedMale == null) {
                        selectedMale = WishParentOption("-1", "blank")
                    }
                    step = WishlistFactoryStep.Wishes
                },
            )
        }

        WishlistFactoryStep.Wishes -> {
            val female = selectedFemale
            val male = selectedMale ?: WishParentOption("-1", "blank")
            if (female != null) {
                // Initialize drafts when parent pair changes (runs once per pair)
                val existingLiveData = remember(female.codeId, male.codeId) {
                    wishlistModel.getByParents(female.codeId, male.codeId)
                }
                val existingWishes by existingLiveData.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
                val metadataPairs = metadata.map { it.property to it.defaultValue }

                LaunchedEffect(female.codeId, male.codeId, metadataPairs, existingWishes, crossType) {
                    val key = "${female.codeId}:${male.codeId}"
                    if (wishDraftsInitializedKey == key) return@LaunchedEffect
                    wishDrafts.clear()
                    val crossWish = existingWishes.find { it.wishType == crossType }
                    wishDrafts.add(
                        WishDraft(
                            type = crossType,
                            min = crossWish?.wishMin?.toString().orEmpty(),
                            max = crossWish?.wishMax?.toString().orEmpty(),
                        ),
                    )
                    metadataPairs.forEach { (property, defaultValue) ->
                        val wish = existingWishes.find { it.wishType == property }
                        wishDrafts.add(
                            WishDraft(
                                type = property,
                                min = (wish?.wishMin ?: defaultValue)?.toString().orEmpty(),
                                max = wish?.wishMax?.toString().orEmpty(),
                            ),
                        )
                    }
                    wishDraftsInitializedKey = key
                }

                WishValuesStep(
                    female = female,
                    male = male,
                    drafts = wishDrafts,
                    onDraftChange = { updated ->
                        val index = wishDrafts.indexOfFirst { it.type == updated.type }
                        if (index >= 0) wishDrafts[index] = updated
                    },
                    onBack = { step = WishlistFactoryStep.Male },
                    onReview = { wishes ->
                        pendingWishes.clear()
                        pendingWishes.addAll(wishes)
                        step = WishlistFactoryStep.Summary
                    },
                    onShowMessage = onShowMessage,
                    metaIcons = metaIcons,
                )
            }
        }

        WishlistFactoryStep.Summary -> {
            WishSummaryStep(
                wishes = pendingWishes,
                femaleName = selectedFemale?.name.orEmpty(),
                maleName = (selectedMale ?: WishParentOption("-1", "blank")).name,
                onBack = { step = WishlistFactoryStep.Wishes },
                onConfirm = {
                    if (pendingWishes.isNotEmpty()) {
                        wishlistModel.insert(*pendingWishes.toTypedArray())
                    }
                    onDone()
                },
                metaIcons = metaIcons,
            )
        }
    }
    } // Box
    } // Scaffold
}
