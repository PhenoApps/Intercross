package org.phenoapps.intercross.ui.app

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.phenoapps.intercross.R

sealed class IntercrossRoute(val route: String) {
    data object Events : IntercrossRoute("events")
    data object CrossTracker : IntercrossRoute("cross_tracker")
    data object CrossBlock : IntercrossRoute("cross_block")
    data object Parents : IntercrossRoute("parents")
    data object ParentCreator : IntercrossRoute("parent_creator/{mode}") {
        fun create(mode: Int) = "parent_creator/$mode"
    }
    data object EventDetail : IntercrossRoute("event_detail/{eventId}") {
        fun create(eventId: Long) = "event_detail/$eventId"
    }
    data object Summary : IntercrossRoute("summary")
    data object Settings : IntercrossRoute("settings")
    data object ProfileSettings : IntercrossRoute("settings/profile")
    data object LayoutSettings : IntercrossRoute("settings/layout")
    data object BehaviorSettings : IntercrossRoute("settings/behavior")
    data object PrintingSettings : IntercrossRoute("settings/printing")
    data object DatabaseSettings : IntercrossRoute("settings/database")
    data object BrapiSettings : IntercrossRoute("settings/brapi")
    data object PatternSettings : IntercrossRoute("settings/pattern")
    data object MetadataSettings : IntercrossRoute("settings/metadata")
    data object AboutSettings : IntercrossRoute("settings/about")
    data object AppearanceSettings : IntercrossRoute("settings/appearance")
    data object BarcodeScanner : IntercrossRoute("barcode/{mode}") {
        fun create(mode: Int) = "barcode/$mode"
    }
    data object BrapiProjects : IntercrossRoute("brapi_projects/{mode}") {
        fun create(mode: Int) = "brapi_projects/$mode"
    }
    data object BrapiPotentialParents : IntercrossRoute("brapi_potential_parents")
    data object BrapiPlannedCrosses : IntercrossRoute("brapi_planned_crosses")
    data object BrapiCrossImport : IntercrossRoute("brapi_cross_import")
    data object BrapiExportSummary : IntercrossRoute("brapi_export_summary")
    data object LabelTemplateEditor : IntercrossRoute("label_template_editor")
    data object WishlistFactory : IntercrossRoute("wishlist_factory")
    data object PollenManager : IntercrossRoute("pollen_manager?code={code}&name={name}") {
        fun create(code: String, name: String) =
            "pollen_manager?code=${Uri.encode(code)}&name=${Uri.encode(name)}"
    }
    data object WishlistDetail : IntercrossRoute("wishlist_detail?female={female}&male={male}&fname={fname}&mname={mname}") {
        fun create(femaleId: String, maleId: String, femaleName: String, maleName: String) =
            "wishlist_detail?female=${Uri.encode(femaleId)}&male=${Uri.encode(maleId)}&fname=${Uri.encode(femaleName)}&mname=${Uri.encode(maleName)}"
    }
}

/** SavedStateHandle keys for cross-screen communication. */
object NavKeys {
    const val PREFILL_FEMALE = "prefill_female"
    const val PREFILL_MALE = "prefill_male"
}

object SettingsPage {
    const val PROFILE = "profile"
    const val LAYOUT = "layout"
    const val BEHAVIOR = "behavior"
    const val PRINTING = "printing"
    const val DATABASE = "database"
    const val BRAPI = "brapi"
    const val ABOUT = "about"
    const val APPEARANCE = "appearance"
}

data class BottomDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
)

val BottomDestinations = listOf(
    BottomDestination(IntercrossRoute.Events.route, R.string.bottom_nv_home, R.drawable.ic_home),
    BottomDestination(IntercrossRoute.CrossTracker.route, R.string.bottom_nv_wishlist, R.drawable.ic_nv_crosses),
    BottomDestination(IntercrossRoute.Parents.route, R.string.bottom_nv_parents, R.drawable.ic_nv_parents_tab),
    BottomDestination(IntercrossRoute.Summary.route, R.string.bottom_nv_summary, R.drawable.ic_summarize_graph),
    BottomDestination(IntercrossRoute.Settings.route, R.string.bottom_nv_settings, R.drawable.ic_nv_settings),
)

data class TopBarAction(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int? = null,
    val onClick: (() -> Unit)? = null,
)

data class TopBarState(
    @param:StringRes val titleRes: Int? = null,
    val title: String? = null,
    val showBack: Boolean = false,
    val actions: List<TopBarAction> = emptyList(),
    val onBack: (() -> Unit)? = null,
)

data class BottomBarState(
    val selectedRoute: String,
    val onTabSelected: (String) -> Unit,
)

data class IntercrossAppActions(
    val launchImportLocal: () -> Unit,
    val launchExportLocal: () -> Unit,
    val onHomeBack: () -> Unit,
    val onNavigateToBrapiProjects: ((Int) -> Unit) -> Unit,
    val onNavigateToProfileSettings: (() -> Unit) -> Unit,
)

enum class BrapiDetailDestination {
    PlannedCrosses,
    PotentialParents,
    CrossImport,
    ExportSummary,
}

object BrapiMode {
    const val WISHLIST = 0
    const val PARENTS = 1
    const val IMPORT_CROSSES = 2
    const val EXPORT_CROSSES = 3
}
