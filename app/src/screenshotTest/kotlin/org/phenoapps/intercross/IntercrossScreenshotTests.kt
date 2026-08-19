package org.phenoapps.intercross

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.phenoapps.intercross.data.models.Wishlist
import java.time.LocalDate

// ─── Events ────────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Populated")
@Composable
fun EventsScreenPopulatedScreenshot() {
    org.phenoapps.intercross.ui.events.EventsScreenPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Empty")
@Composable
fun EventsScreenEmptyScreenshot() {
    org.phenoapps.intercross.ui.events.EventsScreenEmptyPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Selected")
@Composable
fun EventsScreenSelectedScreenshot() {
    org.phenoapps.intercross.ui.events.EventsScreenSelectedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Archived")
@Composable
fun EventsScreenArchivedScreenshot() {
    org.phenoapps.intercross.ui.events.EventsScreenArchivedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Archived Selected")
@Composable
fun EventsScreenArchivedSelectedScreenshot() {
    org.phenoapps.intercross.ui.events.EventsScreenArchivedSelectedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Person Selection")
@Composable
fun EventsScreenPersonSelectionScreenshot() {
    org.phenoapps.intercross.ui.events.EventsScreenPersonSelectionPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventsScreen - Person Selection Expanded")
@Composable
fun EventsScreenPersonSelectionExpandedScreenshot() {
    org.phenoapps.intercross.ui.events.EventsScreenPersonSelectionExpandedPreview()
}

// ─── Event Detail ──────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventDetail - Populated")
@Composable
fun EventDetailPopulatedScreenshot() {
    org.phenoapps.intercross.ui.events.EventDetailScreenPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "EventDetail - Metadata Collection")
@Composable
fun MetadataCollectionScreenshot() {
    org.phenoapps.intercross.ui.events.MetadataCollectionPreview()
}

// ─── Cross Tracker ─────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossTrackerScreen - Populated")
@Composable
fun CrossTrackerPopulatedScreenshot() {
    org.phenoapps.intercross.ui.crosstracker.CrossTrackerScreenPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossTrackerScreen - Empty")
@Composable
fun CrossTrackerEmptyScreenshot() {
    org.phenoapps.intercross.ui.crosstracker.CrossTrackerScreenEmptyPreview()
}

// ─── Cross Block ───────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossBlockScreen - Default")
@Composable
fun CrossBlockScreenshot() {
    org.phenoapps.intercross.ui.crosstracker.CrossBlockScreenPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossBlockScreen - Complex")
@Composable
fun CrossBlockComplexScreenshot() {
    org.phenoapps.intercross.ui.crosstracker.CrossBlockScreenComplexPreview()
}

// ─── Parents ───────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentsScreen - Populated")
@Composable
fun ParentsScreenPopulatedScreenshot() {
    org.phenoapps.intercross.ui.parents.ParentsScreenPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentsScreen - Empty")
@Composable
fun ParentsScreenEmptyScreenshot() {
    org.phenoapps.intercross.ui.parents.ParentsScreenEmptyPreview()
}

// ─── Pollen Manager ────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PollenManagerScreen - Populated")
@Composable
fun PollenManagerPopulatedScreenshot() {
    org.phenoapps.intercross.ui.pollenmanager.PollenManagerScreenPopulatedPreview()
}

// ─── Wishlist Factory ──────────────────────────────────────────────────────────

// ── Step 1: Parent Choice ──────────────────────────────────────────────────────

@PreviewTest
@Preview(
    showBackground = true, device = "id:pixel_8", name = "ParentChoiceStep - Female Selection",
    showSystemUi = true
)
@Composable
fun WishlistParentChoiceFemaleScreenshot() {
    org.phenoapps.intercross.ui.wishlist.ParentChoiceStepPreview()
}

// ── Step 2: Wish Values ────────────────────────────────────────────────────────

@PreviewTest
@Preview(
    showBackground = true, device = "id:pixel_8", name = "WishValuesStep - Populated",
    showSystemUi = true
)
@Composable
fun WishlistValuesStepPopulatedScreenshot() {
    org.phenoapps.intercross.ui.wishlist.WishValuesStepPreview()
}

// ── Wish Draft Row Variations ──────────────────────────────────────────────────

@PreviewTest
@Preview(
    showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Valid Cross",
    showSystemUi = true
)
@Composable
fun WishDraftRowValidScreenshot() {
    org.phenoapps.intercross.ui.wishlist.WishDraftRowValidPreview()
}

@PreviewTest
@Preview(
    showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Min Error",
    showSystemUi = true
)
@Composable
fun WishDraftRowMinErrorScreenshot() {
    org.phenoapps.intercross.ui.wishlist.WishDraftRowMinErrorPreview()
}

@PreviewTest
@Preview(
    showBackground = true, device = "id:pixel_8", name = "WishDraftRow - Max Error",
    showSystemUi = true
)
@Composable
fun WishDraftRowMaxErrorScreenshot() {
    org.phenoapps.intercross.ui.wishlist.WishDraftRowMaxErrorPreview()
}

// ── Step 3: Wish Summary ───────────────────────────────────────────────────────

@PreviewTest
@Preview(
    showBackground = true, device = "id:pixel_8", name = "WishlistSummaryPopulatedScreenshot",
    showSystemUi = true
)
@Composable
fun WishlistSummaryPopulatedScreenshot() {
    org.phenoapps.intercross.ui.wishlist.WishSummaryStepPreview()
}

// ─── Parent Creator ────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentCreatorScreen - Female")
@Composable
fun ParentCreatorFemaleScreenshot() {
    org.phenoapps.intercross.ui.parents.ParentCreatorScreenFemalePreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ParentCreatorScreen - Male")
@Composable
fun ParentCreatorMaleScreenshot() {
    org.phenoapps.intercross.ui.parents.ParentCreatorScreenMalePreview()
}

// ─── Summary ───────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SummaryScreen - Populated")
@Composable
fun SummaryPopulatedScreenshot() {
    org.phenoapps.intercross.ui.summary.SummaryScreenPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SummaryScreen - Empty")
@Composable
fun SummaryEmptyScreenshot() {
    org.phenoapps.intercross.ui.summary.SummaryScreenEmptyPreview()
}

// ─── Settings Hub ───────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SettingsHub - Category List")
@Composable
fun SettingsHubScreenshot() {
    org.phenoapps.intercross.ui.settings.SettingsHubPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "SettingsHub - Search")
@Composable
fun SettingsHubSearchScreenshot() {
    org.phenoapps.intercross.ui.settings.SettingsHubSearchPreview()
}

// ─── Profile Settings ──────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "ProfileSettings - Default")
@Composable
fun ProfileSettingsScreenshot() {
    org.phenoapps.intercross.ui.settings.ProfileSettingsPreview()
}

// ─── Appearance Settings ───────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppearanceSettings - Default")
@Composable
fun AppearanceSettingsScreenshot() {
    org.phenoapps.intercross.ui.settings.AppearanceSettingsPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppearanceSettings - Green")
@Composable
fun AppearanceSettingsGreenScreenshot() {
    org.phenoapps.intercross.ui.settings.AppearanceSettingsGreenPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppearanceSettings - High Contrast")
@Composable
fun AppearanceSettingsHighContrastScreenshot() {
    org.phenoapps.intercross.ui.settings.AppearanceSettingsHighContrastPreview()
}

// ─── Layout Settings ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "LayoutSettings - Default")
@Composable
fun LayoutSettingsScreenshot() {
    org.phenoapps.intercross.ui.settings.LayoutSettingsPreview()
}

// ─── Behavior Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BehaviorSettings - Default")
@Composable
fun BehaviorSettingsScreenshot() {
    org.phenoapps.intercross.ui.settings.BehaviorSettingsPreview()
}

// ─── Pattern Settings ──────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PatternSettings - UUID")
@Composable
fun PatternSettingsUUIDScreenshot() {
    org.phenoapps.intercross.ui.settings.PatternSettingsUUIDPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PatternSettings - Pattern")
@Composable
fun PatternSettingsPatternScreenshot() {
    org.phenoapps.intercross.ui.settings.PatternSettingsPatternPreview()
}

// ─── Metadata Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "MetadataSettings - Populated")
@Composable
fun MetadataSettingsPopulatedScreenshot() {
    org.phenoapps.intercross.ui.settings.MetadataSettingsPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "MetadataSettings - Empty")
@Composable
fun MetadataSettingsEmptyScreenshot() {
    org.phenoapps.intercross.ui.settings.MetadataSettingsEmptyPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "MetadataSettings - Editor Dialog")
@Composable
fun MetadataSettingsEditorDialogScreenshot() {
    org.phenoapps.intercross.ui.settings.MetadataSettingsEditorDialogPreview()
}

// ─── Printing Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PrintingSettings - Default")
@Composable
fun PrintingSettingsScreenshot() {
    org.phenoapps.intercross.ui.settings.PrintingSettingsPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "PrintingSettings - Template Dialog")
@Composable
fun PrintingSettingsTemplateDialogScreenshot() {
    org.phenoapps.intercross.ui.settings.PrintingSettingsTemplateDialogPreview()
}

// ─── Database Settings ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "DatabaseSettings - Default")
@Composable
fun DatabaseSettingsScreenshot() {
    org.phenoapps.intercross.ui.settings.DatabaseSettingsPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "DatabaseSettings - Export Dialog")
@Composable
fun DatabaseExportDialogScreenshot() {
    org.phenoapps.intercross.ui.settings.DatabaseExportDialogPreview()
}

// ─── About Settings ────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AboutSettings - Default")
@Composable
fun AboutSettingsScreenshot() {
    org.phenoapps.intercross.ui.settings.AboutSettingsPreview()
}

// ─── Label Template Editor ─────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "LabelTemplateEditor - Default")
@Composable
fun LabelTemplateEditorScreenshot() {
    org.phenoapps.intercross.ui.labels.LabelTemplateEditorScreenPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "LabelTemplateEditor - Parent")
@Composable
fun LabelTemplateEditorParentScreenshot() {
    org.phenoapps.intercross.ui.labels.LabelTemplateEditorParentPreview()
}

// ─── Barcode Scanner ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BarcodeSingleScan")
@Composable
fun BarcodeSingleScanScreenshot() {
    org.phenoapps.intercross.ui.barcode.BarcodeScannerSinglePreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BarcodeContinuousScan")
@Composable
fun BarcodeContinuousScanScreenshot() {
    org.phenoapps.intercross.ui.barcode.BarcodeScannerContinuousPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BarcodeSearch - Results Dialog")
@Composable
fun BarcodeSearchScreenshot() {
    org.phenoapps.intercross.ui.barcode.BarcodeScannerSearchPreview()
}

// ─── Sequence Scan Indicator ───────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "Indicator - Waiting Female")
@Composable
fun BarcodeIndicatorWaitingFemaleScreenshot() {
    org.phenoapps.intercross.ui.barcode.SequenceScanIndicatorFemalePreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "Indicator - Waiting Male")
@Composable
fun BarcodeIndicatorWaitingMaleScreenshot() {
    org.phenoapps.intercross.ui.barcode.SequenceScanIndicatorMalePreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "Indicator - Waiting Cross")
@Composable
fun BarcodeIndicatorWaitingCrossScreenshot() {
    org.phenoapps.intercross.ui.barcode.SequenceScanIndicatorCompletePreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "Indicator - Cooldown")
@Composable
fun BarcodeIndicatorCooldownScreenshot() {
    org.phenoapps.intercross.ui.barcode.SequenceScanIndicatorCooldownPreview()
}

// ─── BrAPI ──────────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiProjects - Populated")
@Composable
fun BrapiProjectsScreenshot() {
    org.phenoapps.intercross.ui.brapi.BrapiProjectsPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiPlannedCrosses - Populated")
@Composable
fun BrapiPlannedCrossesScreenshot() {
    org.phenoapps.intercross.ui.brapi.BrapiPlannedCrossesPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiCrossImport - Populated")
@Composable
fun BrapiCrossImportScreenshot() {
    org.phenoapps.intercross.ui.brapi.BrapiCrossImportPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiExportSummary - Populated")
@Composable
fun BrapiExportSummaryScreenshot() {
    org.phenoapps.intercross.ui.brapi.BrapiExportSummaryPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "BrapiPotentialParents - Populated")
@Composable
fun BrapiPotentialParentsScreenshot() {
    org.phenoapps.intercross.ui.brapi.BrapiPotentialParentsPopulatedPreview()
}

// ─── Wishlist Detail ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishlistDetail - Populated")
@Composable
fun WishlistDetailPopulatedScreenshot() {
    org.phenoapps.intercross.ui.crosstracker.WishlistDetailPopulatedPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "WishlistDetail - Edit Dialog")
@Composable
fun WishlistDetailEditDialogScreenshot() {
    org.phenoapps.intercross.ui.crosstracker.WishEditDialogPreview(
        wish = Wishlist(
            "HC001", "FJ003", "Honeycrisp", "Fuji", "Cross", 5, 10
        ).apply { id = 1L },
    )
}

// ─── Cross Dates Calendar ──────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "CrossDatesCalendarDialog")
@Composable
fun CrossDatesCalendarDialogScreenshot() {
    org.phenoapps.intercross.ui.crosstracker.CrossDatesCalendarDialogPreview(
        dates = setOf(
            LocalDate.parse("2024-06-05"),
            LocalDate.parse("2024-06-12"),
            LocalDate.parse("2024-06-20"),
        ),
    )
}

// ─── App Intro ─────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppIntro - Welcome", showSystemUi = true)
@Composable
fun AppIntroWelcomeScreenshot() {
    org.phenoapps.intercross.ui.app_intro.IntroWelcomePreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppIntro - Required Setup", showSystemUi = true)
@Composable
fun AppIntroRequiredSetupScreenshot() {
    org.phenoapps.intercross.ui.app_intro.IntroRequiredSetupPreview()
}

@PreviewTest
@Preview(showBackground = true, device = "id:pixel_8", name = "AppIntro - Optional Setup", showSystemUi = true)
@Composable
fun AppIntroOptionalSetupScreenshot() {
    org.phenoapps.intercross.ui.app_intro.IntroOptionalSetupPreview()
}
