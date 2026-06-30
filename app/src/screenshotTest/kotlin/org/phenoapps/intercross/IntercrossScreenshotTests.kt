package org.phenoapps.intercross

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.google.mlkit.vision.barcode.common.Barcode
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.LabelTemplateUiState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.BrapiMode
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.barcode.BARCODE_MODE_CONTINUOUS
import org.phenoapps.intercross.ui.barcode.BARCODE_MODE_SEARCH
import org.phenoapps.intercross.ui.barcode.BARCODE_MODE_SINGLE
import org.phenoapps.intercross.ui.barcode.BarcodeScannerScreen
import org.phenoapps.intercross.ui.barcode.SequenceScanSlot
import org.phenoapps.intercross.ui.brapi.BrapiCrossImportScreen
import org.phenoapps.intercross.ui.brapi.BrapiExportSummaryScreen
import org.phenoapps.intercross.ui.brapi.BrapiPlannedCrossesScreen
import org.phenoapps.intercross.ui.brapi.BrapiPotentialParentsScreen
import org.phenoapps.intercross.ui.brapi.BrapiProjectsScreen
import org.phenoapps.intercross.ui.crosstracker.CrossBlockScreen
import org.phenoapps.intercross.ui.crosstracker.CrossDatesCalendarDialogPreview
import org.phenoapps.intercross.ui.crosstracker.CrossFilter
import org.phenoapps.intercross.ui.crosstracker.CrossTrackerScreen
import org.phenoapps.intercross.ui.crosstracker.WishEditDialogPreview
import org.phenoapps.intercross.ui.crosstracker.WishlistDetailScreen
import org.phenoapps.intercross.ui.crosstracker.WishlistProgressItem
import org.phenoapps.intercross.ui.events.EventDetailScreen
import org.phenoapps.intercross.ui.events.EventsScreenContent
import org.phenoapps.intercross.ui.labels.LabelTemplateEditorScreen
import org.phenoapps.intercross.ui.parents.ParentCreatorScreen
import org.phenoapps.intercross.ui.parents.ParentSortType
import org.phenoapps.intercross.ui.parents.ParentsScreen
import org.phenoapps.intercross.ui.pollenmanager.PollenManagerScreen
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.settings.AboutSettingsScreenContent
import org.phenoapps.intercross.ui.settings.AllSettingsItems
import org.phenoapps.intercross.ui.settings.AppearanceSettingsScreenContent
import org.phenoapps.intercross.ui.settings.BehaviorSettingsScreenContent
import org.phenoapps.intercross.ui.settings.CrossIdMode
import org.phenoapps.intercross.ui.settings.DatabaseSettingsScreenContent
import org.phenoapps.intercross.ui.settings.LayoutSettingsScreenContent
import org.phenoapps.intercross.ui.settings.MetadataSettingsScreenContent
import org.phenoapps.intercross.ui.settings.PatternSettingsScreenContent
import org.phenoapps.intercross.ui.settings.PrintingSettingsScreenContent
import org.phenoapps.intercross.ui.settings.ProfileSettingsScreenContent
import org.phenoapps.intercross.ui.settings.SettingsRow
import org.phenoapps.intercross.ui.settings.SettingsScreenContent
import org.phenoapps.intercross.ui.summary.SummaryScreen
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import org.phenoapps.intercross.ui.wishlist.ParentChoiceStep
import org.phenoapps.intercross.ui.wishlist.WishDraft
import org.phenoapps.intercross.ui.wishlist.WishDraftRow
import org.phenoapps.intercross.ui.wishlist.WishParentOption
import org.phenoapps.intercross.ui.wishlist.WishSummaryStep
import org.phenoapps.intercross.ui.wishlist.WishValuesStepPreviewContent
import org.phenoapps.intercross.util.LabelTemplateConfig
import org.phenoapps.intercross.util.LabelTemplateType
import java.time.LocalDate

// ─── Events ────────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Populated")
@Composable
fun EventsScreenPopulatedScreenshot() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction(
                        "import",
                        R.string.import_file,
                        iconRes = R.drawable.ic_nv_import_white
                    ),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events,
            firstParent = "Honeycrisp",
            secondParent = "Fuji",
            crossId = "Cross-1",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Empty")
@Composable
fun EventsScreenEmptyScreenshot() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = emptyList(),
            firstParent = "",
            secondParent = "",
            crossId = "",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Selected")
@Composable
fun EventsScreenSelectedScreenshot() {
    IntercrossPreviewTheme {
        val selectedIds = setOf(PreviewSampleData.events.first().id ?: 0L)
        EventsScreenContent(
            topBarState = TopBarState(
                title = "1 selected",
                showBack = false,
                actions = listOf(
                    TopBarAction("delete", R.string.delete, iconRes = R.drawable.ic_menu_delete),
                    TopBarAction("archive", R.string.archive_selected_events_title, iconRes = R.drawable.ic_folder_lock),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events,
            firstParent = "",
            secondParent = "",
            crossId = "",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
            selectedEventIds = selectedIds,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Archived")
@Composable
fun EventsScreenArchivedScreenshot() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                title = "Archived Events",
                showBack = false,
                actions = listOf(
                    TopBarAction("unarchive", R.string.unarchive_selected_events_title, iconRes = R.drawable.ic_visibility),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events.take(2),
            firstParent = "",
            secondParent = "",
            crossId = "",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
            showArchivedEvents = true,
        )
    }
}

// ─── Event Detail ──────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventDetail - Populated")
@Composable
fun EventDetailPopulatedScreenshot() {
    IntercrossPreviewTheme {
        EventDetailScreen(
            event = PreviewSampleData.events.first(),
            parents = EventsDao.ParentData(
                momCode = "HC001",
                momReadableName = "Honeycrisp",
                dadCode = "FJ003",
                dadReadableName = "Fuji",
            ),
            metadata = listOf(
                EventsDao.CrossMetadataWithDefaults(
                    eid = 1,
                    property = "Seeds",
                    value = 5,
                    defaultValue = 0,
                ),
            ),
            metaList = emptyList(),
            metaValues = emptyList(),
            wishes = emptyList(),
            allEvents = PreviewSampleData.events,
            onShowMessage = {},
            topBarState = TopBarState(
                titleRes = R.string.event_detail_label,
                showBack = true,
                actions = listOf(
                    TopBarAction(
                        "toggle_metadata_edit",
                        R.string.metaData,
                        iconRes = R.drawable.ic_edit
                    ),
                    TopBarAction(
                        "delete_event",
                        R.string.delete,
                        iconRes = R.drawable.ic_menu_delete
                    ),
                ),
            ),
            showMetadataInputs = false,
            onSaveMetadata = {},
            onNavigateToEvent = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventDetail - Metadata Collection")
@Composable
fun MetadataCollectionScreenshot() {
    IntercrossPreviewTheme {
        EventDetailScreen(
            event = PreviewSampleData.events.first(),
            parents = EventsDao.ParentData(
                momCode = "HC001",
                momReadableName = "Honeycrisp",
                dadCode = "FJ003",
                dadReadableName = "Fuji",
            ),
            metadata = listOf(
                EventsDao.CrossMetadataWithDefaults(
                    eid = 1,
                    property = "Seeds",
                    value = 5,
                    defaultValue = 0,
                ),
                EventsDao.CrossMetadataWithDefaults(
                    eid = 1,
                    property = "Fruits",
                    value = 2,
                    defaultValue = 0,
                ),
            ),
            metaList = listOf(
                Meta("Seeds", 0, "🌱"),
                Meta("Fruits", 0, "🍎")
            ),
            metaValues = emptyList(),
            wishes = listOf(
                WishlistView("HC001", "Honeycrisp", "FJ003", "Fuji", 0, 10, "Seeds", 5),
                WishlistView("HC001", "Honeycrisp", "FJ003", "Fuji", 0, 5, "Fruits", 2)
            ),
            allEvents = PreviewSampleData.events,
            onShowMessage = {},
            topBarState = TopBarState(
                titleRes = R.string.event_detail_label,
                showBack = true,
                actions = listOf(
                    TopBarAction(
                        "toggle_metadata_edit",
                        R.string.metaData,
                        iconRes = R.drawable.ic_edit
                    ),
                ),
            ),
            showMetadataInputs = true,
            onSaveMetadata = {},
            onNavigateToEvent = {},
        )
    }
}

// ─── Cross Tracker ─────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossTrackerScreen - Populated")
@Composable
fun CrossTrackerPopulatedScreenshot() {
    IntercrossPreviewTheme {
        CrossTrackerScreen(
            rows = PreviewSampleData.crossTrackerRowsWithComplete,
            filter = CrossFilter.ALL,
            onFilterChange = {},
            onOpenCrossBlock = {},
            onCreateWishlist = {},
            onRowClick = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("cross_block", R.string.crossblock, iconRes = R.drawable.ic_grid_view_black_24dp),
                    TopBarAction("hide_completed", R.string.summary_label, iconRes = R.drawable.ic_visibility),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "cross_tracker",
                onTabSelected = {},
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossTrackerScreen - Empty")
@Composable
fun CrossTrackerEmptyScreenshot() {
    IntercrossPreviewTheme {
        CrossTrackerScreen(
            rows = emptyList(),
            filter = CrossFilter.ALL,
            onFilterChange = {},
            onOpenCrossBlock = {},
            onCreateWishlist = {},
            onRowClick = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("cross_block", R.string.crossblock, iconRes = R.drawable.ic_grid_view_black_24dp),
                    TopBarAction("hide_completed", R.string.summary_label, iconRes = R.drawable.ic_visibility),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "cross_tracker",
                onTabSelected = {},
            ),
        )
    }
}

// ─── Cross Block ───────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossBlockScreen - Default")
@Composable
fun CrossBlockScreenshot() {
    IntercrossPreviewTheme {
        CrossBlockScreen(
            matrix = PreviewSampleData.crossBlockMatrix,
            onCellClick = {},
            topBarState = TopBarState(
                titleRes = R.string.cross_block_label,
                showBack = true,
                actions = listOf(
                    TopBarAction(
                        "crossblock_add_wish",
                        R.string.add_wishlist_item,
                        iconRes = R.drawable.ic_add_black_24dp
                    ),
                ),
            ),
            selectedWishType = "seeds",
            wishTypes = listOf("seeds", "flowers", "chips"),
            onWishTypeChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossBlockScreen - Complex")
@Composable
fun CrossBlockComplexScreenshot() {
    IntercrossPreviewTheme {
        CrossBlockScreen(
            matrix = PreviewSampleData.crossBlockMatrix,
            onCellClick = {},
            topBarState = TopBarState(
                titleRes = R.string.cross_block_label,
                showBack = true,
                actions = listOf(
                    TopBarAction(
                        "crossblock_add_wish",
                        R.string.add_wishlist_item,
                        iconRes = R.drawable.ic_add_black_24dp
                    ),
                ),
            ),
            selectedWishType = "chips",
            wishTypes = listOf("chips", "beans"),
            onWishTypeChange = {},
        )
    }
}

// ─── Parents ───────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentsScreen - Populated")
@Composable
fun ParentsScreenPopulatedScreenshot() {
    IntercrossPreviewTheme {
        ParentsScreen(
            rows = PreviewSampleData.parentListRows,
            tab = 0,
            sortType = ParentSortType.NAME,
            onTabChange = {},
            onSortTypeChange = {},
            onCreateParent = {},
            onDeleteSelected = {},
            onToggleSelection = {},
            onShowMessage = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("select_all_parents", R.string.sort_by, iconRes = R.drawable.ic_select_all),
                    TopBarAction("import_parents", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("sort_parents", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "parents",
                onTabSelected = {},
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentsScreen - Empty")
@Composable
fun ParentsScreenEmptyScreenshot() {
    IntercrossPreviewTheme {
        ParentsScreen(
            rows = emptyList(),
            tab = 0,
            sortType = ParentSortType.NAME,
            onTabChange = {},
            onSortTypeChange = {},
            onCreateParent = {},
            onDeleteSelected = {},
            onToggleSelection = {},
            onShowMessage = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("select_all_parents", R.string.sort_by, iconRes = R.drawable.ic_select_all),
                    TopBarAction("import_parents", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("sort_parents", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "parents",
                onTabSelected = {},
            ),
        )
    }
}

// ─── Pollen Manager ────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PollenManagerScreen - Populated")
@Composable
fun PollenManagerPopulatedScreenshot() {
    val males = listOf(
        Parent("FJ003", 1).apply { name = "Fuji"; id = 3L },
        Parent("GL004", 1).apply { name = "Gala"; id = 4L },
        Parent("GS005", 1).apply { name = "Granny Smith"; id = 5L },
    )
    IntercrossPreviewTheme {
        PollenManagerScreen(
            normalizedCode = "PG001",
            normalizedName = "Spring Pollen Mix",
            males = males,
            selectedMaleIds = setOf(3L),
            onSelectAll = {},
            onClearAll = {},
            onToggleMale = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.add_male_group,
                showBack = true,
                actions = listOf(
                    TopBarAction("pollen_select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                ),
            ),
        )
    }
}

// ─── Wishlist Factory ──────────────────────────────────────────────────────────

// ── Step 1: Parent Choice ──────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentChoiceStep - Female Selection",
    showSystemUi = true
)
@Composable
fun WishlistParentChoiceFemaleScreenshot() {
    IntercrossPreviewTheme {
        ParentChoiceStep(
            summary = "Choose female parent",
            options = PreviewSampleData.wishParentOptions,
            selected = PreviewSampleData.wishParentOptions.first(),
            onSelected = {},
            onNext = {},
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentChoiceStep - Male Selection",
    showSystemUi = true
)
@Composable
fun WishlistParentChoiceMaleScreenshot() {
    IntercrossPreviewTheme {
        ParentChoiceStep(
            summary = "Choose male parent",
            options = PreviewSampleData.wishParentOptions,
            selected = PreviewSampleData.wishParentOptions.getOrNull(1),
            onSelected = {},
            onNext = {},
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentChoiceStep - No Selection",
    showSystemUi = true
)
@Composable
fun WishlistParentChoiceNoneScreenshot() {
    IntercrossPreviewTheme {
        ParentChoiceStep(
            summary = "Choose female parent",
            options = PreviewSampleData.wishParentOptions,
            selected = null,
            onSelected = {},
            onNext = {},
            onBack = {},
        )
    }
}

// ── Step 2: Wish Values ────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishValuesStep - Populated",
    showSystemUi = true
)
@Composable
fun WishlistValuesStepPopulatedScreenshot() {
    IntercrossPreviewTheme {
        WishValuesStepPreviewContent(
            female = WishParentOption("HC001", "Honeycrisp"),
            male = WishParentOption("GA001", "Gala"),
        )
    }
}

// ── Wish Draft Row Variations ──────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Valid Cross",
    showSystemUi = true
)
@Composable
fun WishDraftRowValidScreenshot() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Cross", min = "5", max = "10"),
            onChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Min Error",
    showSystemUi = true
)
@Composable
fun WishDraftRowMinErrorScreenshot() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Cross", min = "0", max = "10"),
            onChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Max Error",
    showSystemUi = true
)
@Composable
fun WishDraftRowMaxErrorScreenshot() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Cross", min = "5", max = "3"),
            onChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Empty",
    showSystemUi = true
)
@Composable
fun WishDraftRowEmptyScreenshot() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Cross", min = "", max = ""),
            onChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Seed Type",
    showSystemUi = true
)
@Composable
fun WishDraftRowSeedScreenshot() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Seed", min = "3", max = "8"),
            onChange = {},
        )
    }
}

// ── Step 3: Wish Summary ───────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishSummaryStep - Populated",
    showSystemUi = true
)
@Composable
fun WishlistSummaryPopulatedScreenshot() {
    IntercrossPreviewTheme {
        val wishes = listOf(
            Wishlist("HC001", "GA001", "Honeycrisp", "Gala", "Cross", 5, 10),
            Wishlist("HC001", "GA001", "Honeycrisp", "Gala", "Seed", 3, 8),
        )
        WishSummaryStep(
            wishes = wishes,
            femaleName = "Honeycrisp",
            maleName = "Gala",
            onBack = {},
            onConfirm = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishSummaryStep - Single Item",
    showSystemUi = true
)
@Composable
fun WishlistSummarySingleScreenshot() {
    IntercrossPreviewTheme {
        val wishes = listOf(
            Wishlist("HC001", "FJ003", "Honeycrisp", "Fuji", "Cross", 10, 20),
        )
        WishSummaryStep(
            wishes = wishes,
            femaleName = "Honeycrisp",
            maleName = "Fuji",
            onBack = {},
            onConfirm = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishSummaryStep - Multiple Types",
    showSystemUi = true
)
@Composable
fun WishlistSummaryMultipleTypesScreenshot() {
    IntercrossPreviewTheme {
        val wishes = listOf(
            Wishlist("HC001", "GA001", "Honeycrisp", "Gala", "Cross", 5, 10),
            Wishlist("HC001", "GA001", "Honeycrisp", "Gala", "Seed", 3, 8),
            Wishlist("HC001", "GA001", "Honeycrisp", "Gala", "Fruit", 20, 50),
        )
        WishSummaryStep(
            wishes = wishes,
            femaleName = "Honeycrisp",
            maleName = "Gala",
            onBack = {},
            onConfirm = {}
        )
    }
}

// ─── Parent Creator ────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentCreatorScreen - Female")
@Composable
fun ParentCreatorFemaleScreenshot() {
    IntercrossPreviewTheme {
        ParentCreatorScreen(
            mode = 0,
            code = "HC001",
            name = "Honeycrisp",
            bulk = false,
            onCodeChange = {},
            onNameChange = {},
            onBulkChange = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.parent_creator_label,
                showBack = true,
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentCreatorScreen - Male")
@Composable
fun ParentCreatorMaleScreenshot() {
    IntercrossPreviewTheme {
        ParentCreatorScreen(
            mode = 1,
            code = "FJ003",
            name = "Fuji",
            bulk = false,
            onCodeChange = {},
            onNameChange = {},
            onBulkChange = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.parent_creator_label,
                showBack = true,
            ),
        )
    }
}

// ─── Summary ───────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SummaryScreen - Populated")
@Composable
fun SummaryPopulatedScreenshot() {
    IntercrossPreviewTheme {
        SummaryScreen(
            chartData = PreviewSampleData.populatedChartData,
            topBarState = TopBarState(title = "Summary"),
            bottomBarState = BottomBarState(
                selectedRoute = "summary",
                onTabSelected = {},
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SummaryScreen - Empty")
@Composable
fun SummaryEmptyScreenshot() {
    IntercrossPreviewTheme {
        SummaryScreen(
            chartData = PreviewSampleData.emptyChartData,
            topBarState = TopBarState(titleRes = R.string.summary_label),
            bottomBarState = BottomBarState(
                selectedRoute = "summary",
                onTabSelected = {},
            ),
        )
    }
}

// ─── Settings Hub ───────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SettingsHub - Category List")
@Composable
fun SettingsHubScreenshot() {
    IntercrossPreviewTheme {
        SettingsScreenContent(
            rows = listOf(
                SettingsRow(R.string.prefs_profile_title, R.drawable.ic_account_circle_black_24dp, "profile"),
                SettingsRow(R.string.prefs_appearance_title, R.drawable.ic_pref_appearance, "appearance"),
                SettingsRow(R.string.prefs_layout_title, R.drawable.ic_book_open, "layout"),
                SettingsRow(R.string.prefs_behavior_title, R.drawable.ic_edit, "behavior"),
                SettingsRow(R.string.prefs_printing_title, R.drawable.ic_cross_print, "printing"),
                SettingsRow(R.string.prefs_database_title, R.drawable.ic_database, "database"),
                SettingsRow(R.string.prefs_brapi_title, R.drawable.ic_brapi, "brapi"),
                SettingsRow(R.string.prefs_about_title, R.drawable.ic_about_info, "about"),
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            searchItemTitles = emptyList(),
            onOpenPage = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SettingsHub - Search")
@Composable
fun SettingsHubSearchScreenshot() {
    IntercrossPreviewTheme {
        SettingsScreenContent(
            rows = listOf(
                SettingsRow(R.string.prefs_profile_title, R.drawable.ic_account_circle_black_24dp, "profile"),
                SettingsRow(R.string.prefs_appearance_title, R.drawable.ic_pref_appearance, "appearance"),
                SettingsRow(R.string.prefs_layout_title, R.drawable.ic_book_open, "layout"),
                SettingsRow(R.string.prefs_behavior_title, R.drawable.ic_edit, "behavior"),
                SettingsRow(R.string.prefs_printing_title, R.drawable.ic_cross_print, "printing"),
                SettingsRow(R.string.prefs_database_title, R.drawable.ic_database, "database"),
                SettingsRow(R.string.prefs_brapi_title, R.drawable.ic_brapi, "brapi"),
                SettingsRow(R.string.prefs_about_title, R.drawable.ic_about_info, "about"),
            ),
            searchQuery = "person",
            onSearchQueryChange = {},
            searchItemTitles = AllSettingsItems.map {
                it to "placeholder"
            },
            onOpenPage = {},
        )
    }
}

// ─── Profile Settings ──────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ProfileSettings - Default")
@Composable
fun ProfileSettingsScreenshot() {
    IntercrossPreviewTheme {
        ProfileSettingsScreenContent(
            personSummary = "Jane",
            personsListSummary = "Jane, Bob, Alice",
            intervalSummary = "Every 24 hours",
            intervalEntries = listOf("Never", "Every 12 hours", "Every 24 hours", "Always"),
            intervalValues = listOf("0", "12", "24", "-1"),
            verificationInterval = "24",
            onSelectPerson = {},
            onAddPerson = {},
            onManagePersons = {},
            onIntervalChange = {},
            onReset = {},
            showSelectDialog = false,
            onSelectDialogDismiss = {},
            onSelectDialogClear = {},
            onSelectDialogSave = {},
            selectedPerson = "Jane",
            persons = listOf("Jane", "Bob", "Alice"),
            showAddDialog = false,
            onAddDialogDismiss = {},
            onAddDialogAdd = { null },
            showManageDialog = false,
            onManageDialogDismiss = {},
            onManageDialogRemove = {},
            showResetDialog = false,
            onResetDialogDismiss = {},
            onResetDialogConfirm = {},
        )
    }
}

// ─── Appearance Settings ───────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppearanceSettings - Default")
@Composable
fun AppearanceSettingsScreenshot() {
    IntercrossPreviewTheme {
        AppearanceSettingsScreenContent(
            themeEntries = listOf("Default", "Green", "High Contrast"),
            themeValues = listOf("Default", "Green", "HighContrast"),
            currentThemeName = "Default",
            onThemeSelected = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppearanceSettings - Green")
@Composable
fun AppearanceSettingsGreenScreenshot() {
    IntercrossPreviewTheme {
        AppearanceSettingsScreenContent(
            themeEntries = listOf("Default", "Green", "High Contrast"),
            themeValues = listOf("Default", "Green", "HighContrast"),
            currentThemeName = "Green",
            onThemeSelected = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppearanceSettings - High Contrast")
@Composable
fun AppearanceSettingsHighContrastScreenshot() {
    IntercrossPreviewTheme {
        AppearanceSettingsScreenContent(
            themeEntries = listOf("Default", "Green", "High Contrast"),
            themeValues = listOf("Default", "Green", "HighContrast"),
            currentThemeName = "HighContrast",
            onThemeSelected = {},
        )
    }
}

// ─── Layout Settings ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "LayoutSettings - Default")
@Composable
fun LayoutSettingsScreenshot() {
    IntercrossPreviewTheme {
        LayoutSettingsScreenContent(
            showPersonInput = true,
            onShowPersonInputChange = {},
        )
    }
}

// ─── Behavior Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BehaviorSettings - Default")
@Composable
fun BehaviorSettingsScreenshot() {
    IntercrossPreviewTheme {
        BehaviorSettingsScreenContent(
            blankMale = false,
            onBlankMaleChange = {},
            crossOrder = false,
            onCrossOrderChange = {},
            crossIdSummary = "UUID",
            onOpenPattern = {},
            collectAdditionalInfo = true,
            onCollectAdditionalInfoChange = {},
            onOpenMetadata = {},
            soundNotifications = false,
            onSoundNotificationsChange = {},
            openCrossAfterCreate = false,
            onOpenCrossAfterCreateChange = {},
            commutativeCrossing = false,
            onCommutativeCrossingChange = {},
            barcodeFormats = setOf("QR", "1D"),
            barcodeFormatSummary = "Barcode Summary",
            onBarcodeFormatsChange = {},
            onBack = {},
        )
    }
}

// ─── Pattern Settings ──────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PatternSettings - UUID")
@Composable
fun PatternSettingsUUIDScreenshot() {
    IntercrossPreviewTheme {
        PatternSettingsScreenContent(
            mode = CrossIdMode.UUID,
            onModeChange = {},
            uuidPreview = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            patternPreview = "",
            prefix = "",
            onPrefixChange = {},
            suffix = "",
            onSuffixChange = {},
            numberPreview = "",
            autoIncrement = true,
            onAutoIncrementChange = {},
            number = "1",
            onNumberChange = {},
            pad = "4",
            onPadChange = {},
            showResetDialog = false,
            onShowResetDialogChange = {},
            onResetConfirm = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PatternSettings - Pattern")
@Composable
fun PatternSettingsPatternScreenshot() {
    IntercrossPreviewTheme {
        PatternSettingsScreenContent(
            mode = CrossIdMode.PATTERN,
            onModeChange = {},
            uuidPreview = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            patternPreview = "CROSS-0001-A",
            prefix = "CROSS-",
            onPrefixChange = {},
            suffix = "-A",
            onSuffixChange = {},
            numberPreview = "0001",
            autoIncrement = true,
            onAutoIncrementChange = {},
            number = "1",
            onNumberChange = {},
            pad = "4",
            onPadChange = {},
            showResetDialog = false,
            onShowResetDialogChange = {},
            onResetConfirm = {},
        )
    }
}

// ─── Metadata Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "MetadataSettings - Populated")
@Composable
fun MetadataSettingsPopulatedScreenshot() {
    IntercrossPreviewTheme {
        val meta1 = Meta("Seeds", 0).apply { id = 1L }
        val meta2 = Meta("Fruit", null).apply { id = 2L }
        MetadataSettingsScreenContent(
            metadata = listOf(meta1, meta2),
            onAddClick = {},
            onEditClick = {},
            onDeleteClick = {},
            showEditor = false,
            editingMeta = null,
            onEditorDismiss = {},
            onEditorSave = { _, _ , _-> },
            pendingDelete = null,
            onDeleteDismiss = {},
            onDeleteConfirm = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "MetadataSettings - Empty")
@Composable
fun MetadataSettingsEmptyScreenshot() {
    IntercrossPreviewTheme {
        MetadataSettingsScreenContent(
            metadata = emptyList(),
            onAddClick = {},
            onEditClick = {},
            onDeleteClick = {},
            showEditor = false,
            editingMeta = null,
            onEditorDismiss = {},
            onEditorSave = { _, _ , _ -> },
            pendingDelete = null,
            onDeleteDismiss = {},
            onDeleteConfirm = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "MetadataSettings - Editor Dialog")
@Composable
fun MetadataSettingsEditorDialogScreenshot() {
    IntercrossPreviewTheme {
        MetadataSettingsScreenContent(
            metadata = emptyList(),
            onAddClick = {},
            onEditClick = {},
            onDeleteClick = {},
            showEditor = true,
            editingMeta = Meta("Seeds", 5, "🌱"),
            onEditorDismiss = {},
            onEditorSave = { _, _, _ -> },
            pendingDelete = null,
            onDeleteDismiss = {},
            onDeleteConfirm = {},
        )
    }
}

// ─── Printing Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PrintingSettings - Default")
@Composable
fun PrintingSettingsScreenshot() {
    IntercrossPreviewTheme {
        PrintingSettingsScreenContent(
            selectedDeviceName = "Zebra ZQ620",
            onConnectPrinter = {},
            onOpenZebraSetup = {},
            onOpenLabelDesigner = {},
            crossTemplateName = "Standard 2x1",
            parentTemplateName = "Parent Label",
            onShowCrossTemplateDialog = {},
            onShowParentTemplateDialog = {},
            onRequestLabel = {},
            showDeviceDialog = false,
            onDeviceDialogDismiss = {},
            onDeviceSelected = {},
            showCrossTemplateDialog = false,
            crossTemplates = emptyList(),
            onCrossTemplateDialogDismiss = {},
            onCrossTemplateSelected = {},
            showParentTemplateDialog = false,
            parentTemplates = emptyList(),
            onParentTemplateDialogDismiss = {},
            onParentTemplateSelected = {},
            message = null,
            onMessageDismiss = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PrintingSettings - Template Dialog")
@Composable
fun PrintingSettingsTemplateDialogScreenshot() {
    IntercrossPreviewTheme {
        PrintingSettingsScreenContent(
            selectedDeviceName = "Zebra ZQ620",
            onConnectPrinter = {},
            onOpenZebraSetup = {},
            onOpenLabelDesigner = {},
            crossTemplateName = "Standard 2x1",
            parentTemplateName = "Parent Label",
            onShowCrossTemplateDialog = {},
            onShowParentTemplateDialog = {},
            onRequestLabel = {},
            showDeviceDialog = false,
            onDeviceDialogDismiss = {},
            onDeviceSelected = {},
            showCrossTemplateDialog = true,
            crossTemplates = listOf(
                LabelTemplateConfig(name = "Standard 2x1", rawZpl = "", labelType = LabelTemplateType.CROSS.name),
                LabelTemplateConfig(name = "Compact 1x1", rawZpl = "", labelType = LabelTemplateType.CROSS.name),
                LabelTemplateConfig(name = "Detailed 3x2", rawZpl = "", labelType = LabelTemplateType.CROSS.name),
            ),
            onCrossTemplateDialogDismiss = {},
            onCrossTemplateSelected = {},
            showParentTemplateDialog = false,
            parentTemplates = emptyList(),
            onParentTemplateDialogDismiss = {},
            onParentTemplateSelected = {},
            message = null,
            onMessageDismiss = {},
        )
    }
}

// ─── Database Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "DatabaseSettings - Default")
@Composable
fun DatabaseSettingsScreenshot() {
    IntercrossPreviewTheme {
        DatabaseSettingsScreenContent(
            storageSummary = "Not configured",
            onOpenStorage = {},
            onImport = {},
            onExportClick = {},
            onReloadClick = {},
            onResetClick = {},
            showExportDialog = false,
            onExportDismiss = {},
            onExportConfirm = { _ -> },
            showResetFirstDialog = false,
            onResetFirstDismiss = {},
            onResetFirstConfirm = {},
            showResetSecondDialog = false,
            onResetSecondDismiss = {},
            onResetSecondConfirm = {},
            showReloadDialog = false,
            onReloadDismiss = {},
            onReloadConfirm = {},
            message = null,
            onMessageDismiss = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "DatabaseSettings - Export Dialog")
@Composable
fun DatabaseExportDialogScreenshot() {
    IntercrossPreviewTheme {
        DatabaseSettingsScreenContent(
            storageSummary = "Documents/Intercross",
            onOpenStorage = {},
            onImport = {},
            onExportClick = {},
            onReloadClick = {},
            onResetClick = {},
            showExportDialog = true,
            onExportDismiss = {},
            onExportConfirm = { _ -> },
            showResetFirstDialog = false,
            onResetFirstDismiss = {},
            onResetFirstConfirm = {},
            showResetSecondDialog = false,
            onResetSecondDismiss = {},
            onResetSecondConfirm = {},
            showReloadDialog = false,
            onReloadDismiss = {},
            onReloadConfirm = {},
            message = null,
            onMessageDismiss = {},
        )
    }
}

// ─── About Settings ────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AboutSettings - Default")
@Composable
fun AboutSettingsScreenshot() {
    IntercrossPreviewTheme {
        AboutSettingsScreenContent(
            versionName = "1.2.3",
            onChangelog = {},
            onRate = {},
            developerName = "PhenoApps",
            developerLocation = "Clemson",
            developerEmail = "developer@email.com",
            onEmail = {},
            onContributors = {},
            onFunding = {},
            onPhenoApps = {},
            onFieldBook = {},
            onCoordinate = {},
            onGitHub = {},
            onLibraries = {},
            onBack = {},
        )
    }
}

// ─── Label Template Editor ─────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "LabelTemplateEditor - Default")
@Composable
fun LabelTemplateEditorScreenshot() {
    IntercrossPreviewTheme {
        val sampleConfig = LabelTemplateConfig(
            name = "Sample 2x1",
            rawZpl = "^XA\n^PW406\n^LH10,10^FS\n^FO0,0^A0,25,20^FD{crossId}^FS\n^FO140,30^BQN,2,3^FDQA,{crossId}^FS\n^FO140,170^A0,25,20^FD{date}^FS\n^XZ",
            labelType = LabelTemplateType.CROSS.name
        )

        val sampleUiState = LabelTemplateUiState(
            config = sampleConfig,
            savedTemplates = listOf(sampleConfig),
            deviceName = "Zebra ZQ620",
            printerDetails = "203 dpi, 2x1 in"
        )

        LabelTemplateEditorScreen(
            uiState = sampleUiState,
            onConfigChange = {},
            onSelectSavedTemplate = { _, _ -> },
            onSaveTemplate = {},
            onImportZpl = {},
            onDetectDpi = {},
            onRenderPreview = {},
            onMessageShown = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "LabelTemplateEditor - Parent")
@Composable
fun LabelTemplateEditorParentScreenshot() {
    IntercrossPreviewTheme {
        val sampleConfig = LabelTemplateConfig(
            name = "Parent Label",
            rawZpl = "^XA\n^PW406\n^FO0,0^A0,25,20^FD{parentCode}^FS\n^FO0,30^A0,25,20^FD{parentName}^FS\n^XZ",
            labelType = LabelTemplateType.PARENT.name
        )

        val sampleUiState = LabelTemplateUiState(
            config = sampleConfig,
            savedTemplates = listOf(sampleConfig),
            deviceName = "Zebra ZQ620",
            printerDetails = "203 dpi, 2x1 in"
        )

        LabelTemplateEditorScreen(
            uiState = sampleUiState,
            onConfigChange = {},
            onSelectSavedTemplate = { _, _ -> },
            onSaveTemplate = {},
            onImportZpl = {},
            onDetectDpi = {},
            onRenderPreview = {},
            onMessageShown = {},
        )
    }
}

// ─── Barcode Scanner ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BarcodeSingleScan")
@Composable
fun BarcodeSingleScanScreenshot() {
    IntercrossPreviewTheme {
        BarcodeScannerScreen(
            mode = BARCODE_MODE_SINGLE,
            events = emptyList(),
            parents = emptyList(),
            mlKitFormats = Barcode.FORMAT_QR_CODE,
            torchEnabled = false,
            onSingleScan = {},
            onSequenceScan = {},
            onSequenceCode = {},
            onOpenEvent = {},
            onShowMessage = {},
            topBarState = TopBarState(
                titleRes = R.string.barcode_scan_label,
                showBack = true,
                actions = listOf(
                    TopBarAction(
                        id = "toggle_flash",
                        labelRes = R.string.barcode_flash_on,
                        iconRes = R.drawable.ic_flash_on,
                    ),
                ),
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BarcodeContinuousScan")
@Composable
fun BarcodeContinuousScanScreenshot() {
    IntercrossPreviewTheme {
        BarcodeScannerScreen(
            mode = BARCODE_MODE_CONTINUOUS,
            events = emptyList(),
            parents = emptyList(),
            mlKitFormats = Barcode.FORMAT_QR_CODE,
            torchEnabled = false,
            onSingleScan = {},
            onSequenceScan = {},
            onSequenceCode = {},
            sequenceFemaleScanned = true,
            sequenceMaleScanned = false,
            sequenceCrossScanned = false,
            sequenceFemaleCode = "HC001",
            sequenceMaleCode = "",
            sequenceCrossIdCode = "",
            sequenceNeedsMale = true,
            sequenceNeedsCrossId = true,
            sequenceNextSlot = SequenceScanSlot.Male,
            onOpenEvent = {},
            onShowMessage = {},
            topBarState = TopBarState(
                titleRes = R.string.barcode_scan_label,
                showBack = true,
                actions = listOf(
                    TopBarAction(
                        id = "toggle_flash",
                        labelRes = R.string.barcode_flash_on,
                        iconRes = R.drawable.ic_flash_on,
                    ),
                ),
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BarcodeSearch - Results Dialog")
@Composable
fun BarcodeSearchScreenshot() {
    IntercrossPreviewTheme {
        val searchEvents = listOf(
            Event(
                eventDbId = "Cross-HC001xFJ003-001",
                femaleObsUnitDbId = "HC001",
                maleObsUnitDbId = "FJ003",
                readableName = "Honeycrisp × Fuji #1",
                timestamp = "2024-06-10",
                person = "Breeder A",
            ).apply { id = 101L },
            Event(
                eventDbId = "Cross-HC001xFJ003-002",
                femaleObsUnitDbId = "HC001",
                maleObsUnitDbId = "FJ003",
                readableName = "Honeycrisp × Fuji #2",
                timestamp = "2024-06-12",
                person = "Breeder A",
            ).apply { id = 102L },
        )
        BarcodeScannerScreen(
            mode = BARCODE_MODE_SEARCH,
            events = searchEvents,
            parents = emptyList(),
            mlKitFormats = Barcode.FORMAT_QR_CODE,
            torchEnabled = false,
            onSingleScan = {},
            onSequenceScan = {},
            onSequenceCode = {},
            onOpenEvent = {},
            onShowMessage = {},
            topBarState = TopBarState(
                titleRes = R.string.barcode_scan_label,
                showBack = true,
                actions = listOf(
                    TopBarAction(
                        id = "toggle_flash",
                        labelRes = R.string.barcode_flash_on,
                        iconRes = R.drawable.ic_flash_on,
                    ),
                ),
            ),
            initialChildDialogEvents = searchEvents,
        )
    }
}

// ─── BrAPI ──────────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiProjects - Populated")
@Composable
fun BrapiProjectsScreenshot() {
    IntercrossPreviewTheme {
        BrapiProjectsScreen(
            serverUrl = "https://test-server.brapi.org",
            isLoading = false,
            projects = PreviewSampleData.brapiCrossingProjects,
            selectedProject = PreviewSampleData.brapiCrossingProjects.first(),
            onSelectProject = {},
            page = 0,
            totalPages = 1,
            onPageChange = {},
            mode = BrapiMode.IMPORT_CROSSES,
            onAction = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiPlannedCrosses - Populated")
@Composable
fun BrapiPlannedCrossesScreenshot() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiPlannedCrossesScreen(
            args = args,
            plannedCrosses = PreviewSampleData.brapiPlannedCrosses,
            importParents = true,
            onImportParentsChange = {},
            isLoading = false,
            isImporting = false,
            importProgress = 0,
            hasError = false,
            onRetry = {},
            onImport = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiCrossImport - Populated")
@Composable
fun BrapiCrossImportScreenshot() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiCrossImportScreen(
            args = args,
            crosses = PreviewSampleData.brapiCrosses,
            isLoading = false,
            isImporting = false,
            importProgress = 0,
            hasError = false,
            onRetry = {},
            onImport = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiExportSummary - Populated")
@Composable
fun BrapiExportSummaryScreenshot() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiExportSummaryScreen(
            args = args,
            exportableEvents = PreviewSampleData.events,
            isLoading = false,
            hasError = false,
            onRetry = {},
            onExport = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiPotentialParents - Populated")
@Composable
fun BrapiPotentialParentsScreenshot() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 3)
        }
        BrapiPotentialParentsScreen(
            args = args,
            potentialParents = PreviewSampleData.brapiPotentialParents,
            expandedParentIds = setOf("HC001"),
            onToggleExpanded = {},
            isImporting = false,
            onImport = {}
        )
    }
}

// ─── Wishlist Detail ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishlistDetail - Populated")
@Composable
fun WishlistDetailPopulatedScreenshot() {
    IntercrossPreviewTheme {
        WishlistDetailScreen(
            femaleName = "Honeycrisp",
            maleName = "Fuji",
            femaleId = "HC001",
            maleId = "FJ003",
            wishes = listOf(
                Wishlist("HC001", "FJ003", "Honeycrisp", "Fuji", "Cross", 5, 10).apply { id = 1L },
                Wishlist("HC001", "FJ003", "Honeycrisp", "Fuji", "Seeds", 3, 8).apply { id = 2L },
            ),
            wishProgress = listOf(
                WishlistProgressItem("Cross", min = 5, max = 10, progress = 7),
                WishlistProgressItem("Seeds", min = 3, max = 8, progress = 5),
            ),
            crosses = PreviewSampleData.events.take(2),
            onEditWish = {},
            onDeleteWish = {},
            onAddWish = {},
            onMakeCross = {},
            onOpenEvent = {},
            topBarState = TopBarState(titleRes = R.string.wishlist_detail_title, showBack = true),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishlistDetail - Edit Dialog")
@Composable
fun WishlistDetailEditDialogScreenshot() {
    IntercrossPreviewTheme {
        WishEditDialogPreview(
            wish = Wishlist(
                "HC001", "FJ003", "Honeycrisp", "Fuji", "Cross", 5, 10
            ).apply { id = 1L },
        )
    }
}

// ─── Cross Dates Calendar ──────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossDatesCalendarDialog")
@Composable
fun CrossDatesCalendarDialogScreenshot() {
    IntercrossPreviewTheme {
        CrossDatesCalendarDialogPreview(
            dates = setOf(
                LocalDate.parse("2024-06-05"),
                LocalDate.parse("2024-06-12"),
                LocalDate.parse("2024-06-20"),
            ),
        )
    }
}
