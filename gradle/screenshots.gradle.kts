// Map from screenshot test function name to doc-friendly filename
val screenshotRenameMap = mapOf(
    "EventsScreenPopulatedScreenshot" to "events_populated.png",
    "EventsScreenEmptyScreenshot" to "events_empty.png",
    "EventsScreenSelectedScreenshot" to "events_selected.png",
    "EventsScreenArchivedScreenshot" to "events_archived.png",
    "EventDetailPopulatedScreenshot" to "event_detail.png",
    "MetadataCollectionScreenshot" to "metadata_collection.png",
    "CrossTrackerPopulatedScreenshot" to "cross_tracker_populated.png",
    "CrossTrackerEmptyScreenshot" to "cross_tracker_empty.png",
    "CrossBlockScreenshot" to "cross_block.png",
    "CrossBlockComplexScreenshot" to "cross_block_complex.png",
    "ParentsScreenPopulatedScreenshot" to "parents_populated.png",
    "ParentsScreenEmptyScreenshot" to "parents_empty.png",
    "ParentCreatorFemaleScreenshot" to "parent_creator_female.png",
    "ParentCreatorMaleScreenshot" to "parent_creator_male.png",
    "PollenManagerPopulatedScreenshot" to "pollen_manager.png",
    "SummaryPopulatedScreenshot" to "summary_populated.png",
    "SummaryEmptyScreenshot" to "summary_empty.png",
    "WishlistParentChoiceFemaleScreenshot" to "wishlist_parent_choice_female.png",
    "WishlistParentChoiceMaleScreenshot" to "wishlist_parent_choice_male.png",
    "WishlistParentChoiceNoneScreenshot" to "wishlist_parent_choice_none.png",
    "WishlistValuesStepPopulatedScreenshot" to "wishlist_values_step.png",
    "WishDraftRowValidScreenshot" to "wishlist_draft_row_valid.png",
    "WishDraftRowMinErrorScreenshot" to "wishlist_draft_row_min_error.png",
    "WishDraftRowMaxErrorScreenshot" to "wishlist_draft_row_max_error.png",
    "WishDraftRowEmptyScreenshot" to "wishlist_draft_row_empty.png",
    "WishDraftRowSeedScreenshot" to "wishlist_draft_row_seed.png",
    "WishlistSummaryPopulatedScreenshot" to "wishlist_summary_populated.png",
    "WishlistSummarySingleScreenshot" to "wishlist_summary_single.png",
    "WishlistSummaryMultipleTypesScreenshot" to "wishlist_summary_multiple.png",
    "SettingsHubScreenshot" to "settings_hub.png",
    "SettingsHubSearchScreenshot" to "settings_hub_search.png",
    "ProfileSettingsScreenshot" to "settings_profile.png",
    "AppearanceSettingsScreenshot" to "settings_appearance.png",
    "AppearanceSettingsGreenScreenshot" to "settings_appearance_green.png",
    "AppearanceSettingsHighContrastScreenshot" to "settings_appearance_high_contrast.png",
    "LayoutSettingsScreenshot" to "settings_layout.png",
    "BehaviorSettingsScreenshot" to "settings_behavior.png",
    "PatternSettingsUUIDScreenshot" to "settings_pattern_uuid.png",
    "PatternSettingsPatternScreenshot" to "settings_pattern_pattern.png",
    "MetadataSettingsPopulatedScreenshot" to "settings_metadata_populated.png",
    "MetadataSettingsEmptyScreenshot" to "settings_metadata_empty.png",
    "MetadataSettingsEditorDialogScreenshot" to "settings_metadata_editor_dialog.png",
    "PrintingSettingsScreenshot" to "settings_printing.png",
    "PrintingSettingsTemplateDialogScreenshot" to "settings_printing_dialog.png",
    "DatabaseSettingsScreenshot" to "settings_database.png",
    "DatabaseExportDialogScreenshot" to "settings_database_export_dialog.png",
    "AboutSettingsScreenshot" to "settings_about.png",
    "LabelTemplateEditorScreenshot" to "label_template_editor.png",
    "LabelTemplateEditorParentScreenshot" to "label_template_parent.png",

    // Barcode Scanner
    "BarcodeSingleScanScreenshot" to "barcode_single_scan.png",
    "BarcodeContinuousScanScreenshot" to "barcode_continuous_scan.png",
    "BarcodeSearchScreenshot" to "barcode_search.png",

    // BrAPI
    "BrapiProjectsScreenshot" to "brapi_projects.png",
    "BrapiPotentialParentsScreenshot" to "brapi_potential_parents.png",
    "BrapiPlannedCrossesScreenshot" to "brapi_planned_crosses.png",
    "BrapiCrossImportScreenshot" to "brapi_cross_import.png",
    "BrapiExportSummaryScreenshot" to "brapi_export_summary.png",

    // Wishlist Detail
    "WishlistDetailPopulatedScreenshot" to "wishlist_detail_populated.png",
    "WishlistDetailEditDialogScreenshot" to "wishlist_detail_edit_dialog.png",

    // Cross Dates Calendar
    "CrossDatesCalendarDialogScreenshot" to "cross_dates_calendar_dialog.png",
)

// Regex to extract function name from generated filename like:
// EventsScreenPopulatedScreenshot_EventsScreen - Populated_769910d5_0.png
val screenshotNameRegex = Regex("^([A-Za-z]+)_.+_[a-f0-9]{8}_\\d+\\.png$")

// Copy screenshot test renders to docs/_static/images with clean names
tasks.register<Copy>("copyScreenshotTestImages") {
    description = "Copies screenshot test rendered images to docs/_static/images with renamed filenames"
    group = "intercross"

    val screenshotDir = project.layout.buildDirectory.dir(
        "outputs/screenshotTest-results/preview/debug/rendered"
    )
    val docsImagesDir = rootProject.layout.projectDirectory.dir("docs/_static/images")

    from(screenshotDir)
    into(docsImagesDir)

    // Only copy PNG files
    include("**/*.png")

    // Flatten directory structure and rename using the map
    rename { fileName ->
        val match = screenshotNameRegex.find(fileName)
        if (match != null) {
            val funcName = match.groupValues[1]
            screenshotRenameMap[funcName] ?: fileName
        } else {
            fileName
        }
    }

    // Only include files that match a known rename mapping
    include {
        val fileName = it.name
        val match = screenshotNameRegex.find(fileName)
        if (match != null) {
            val funcName = match.groupValues[1]
            funcName in screenshotRenameMap
        } else {
            false
        }
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// Helper task to copy specific screenshot by name
// Usage: ./gradlew copyScreenshotTestImagesByName -PscreenshotName=CrossBlockComplexScreenshot
tasks.register<Copy>("copyScreenshotTestImagesByName") {
    description = "Copies a specific screenshot test image to docs/_static/images with renamed filename"
    group = "intercross"

    val screenshotName = project.findProperty("screenshotName") as? String ?: ""

    val screenshotDir = project.layout.buildDirectory.dir(
        "outputs/screenshotTest-results/preview/debug/rendered"
    )
    val docsImagesDir = rootProject.layout.projectDirectory.dir("docs/_static/images")

    from(screenshotDir)
    into(docsImagesDir)

    include("*${screenshotName}*.png")

    // Rename using the same map
    rename { fileName ->
        val match = screenshotNameRegex.find(fileName)
        if (match != null) {
            val funcName = match.groupValues[1]
            screenshotRenameMap[funcName] ?: fileName
        } else {
            fileName
        }
    }

    onlyIf {
        screenshotName.isNotEmpty() && screenshotDir.get().asFile.exists()
    }
}

// Exclude ExoPlayer from screenshot test classpath to avoid IncompatibleClassChangeError
// (exoplayer2.R and exoplayer2.R$menu disagree on InnerClasses attribute)
configurations.all {
    if (name.contains("ScreenshotTest", ignoreCase = true)) {
        exclude(group = "com.google.android.exoplayer")
        // Exclude Media library to avoid IncompatibleClassChangeError with androidx.media.R
        exclude(group = "androidx.media")
        // Exclude ViewPager2 to avoid R class conflict
        exclude(group = "androidx.viewpager2")
        // Exclude CoordinatorLayout to avoid R class conflict
        exclude(group = "androidx.coordinatorlayout")
        // Exclude Changelog library to avoid R class conflict
        exclude(group = "com.github.MFlisar")
    }
}

// Ensure ExoPlayer is also excluded from the screenshotTest configurations explicitly
configurations {
    named("screenshotTestImplementation") {
        exclude(group = "com.google.android.exoplayer")
        exclude(group = "androidx.media")
        exclude(group = "androidx.viewpager2")
        exclude(group = "androidx.coordinatorlayout")
        exclude(group = "com.github.MFlisar")
    }
    named("screenshotTestRuntimeOnly") {
        exclude(group = "com.google.android.exoplayer")
        exclude(group = "androidx.media")
        exclude(group = "androidx.viewpager2")
        exclude(group = "androidx.coordinatorlayout")
        exclude(group = "com.github.MFlisar")
    }
}

// Fix: ensure merged manifest is available for screenshot test config generation
// In AGP 8.5+, GenerateTestConfig requires the merged manifest property to be set.
afterEvaluate {
    tasks.filter { it.name.contains("ScreenshotTestConfig") }.forEach {
        val screenshotTask = it
        val variantName = screenshotTask.name.removePrefix("generate").removeSuffix("ScreenshotTestConfig")
        val manifestTask = tasks.findByName("process${variantName}MainManifest")
            ?: tasks.findByName("process${variantName}Manifest")

        if (manifestTask != null) {
            try {
                // Reach the nested property: task.getTestConfigInputs().getMergedManifest()
                val getTestConfigInputs = screenshotTask.javaClass.getMethod("getTestConfigInputs")
                val testConfigInputs = getTestConfigInputs.invoke(screenshotTask)
                
                val getMergedManifest = testConfigInputs.javaClass.getMethod("getMergedManifest")
                val mergedManifestProperty = getMergedManifest.invoke(testConfigInputs) as org.gradle.api.file.DirectoryProperty
                
                // Set the directory property to the directory containing the merged manifest and output-metadata.json.
                // In AGP 8.5+, this is typically intermediates/merged_manifests/{variant}/process{Variant}Manifest
                val variantLower = variantName.replaceFirstChar { it.lowercase() }
                val dir = project.layout.buildDirectory.dir("intermediates/merged_manifests/$variantLower/process${variantName}Manifest")

                mergedManifestProperty.set(dir)
                
                // Ensure dependency
                screenshotTask.dependsOn(manifestTask)
            } catch (e: Exception) {
                // Fallback to ordering and dependencies if wiring fails
                screenshotTask.mustRunAfter(manifestTask)
                screenshotTask.dependsOn(manifestTask)
            }
        }
    }
}
