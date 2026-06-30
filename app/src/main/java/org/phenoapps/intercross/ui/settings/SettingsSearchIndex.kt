package org.phenoapps.intercross.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.app.SettingsPage

/**
 * A searchable settings item that maps to a specific settings page.
 */
data class SettingsSearchItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val page: String,
    @StringRes val categoryRes: Int,
)

/**
 * index of all individual settings items for search.
 */
val AllSettingsItems: List<SettingsSearchItem> = listOf(
    // Profile
    SettingsSearchItem(R.string.profile_person, R.drawable.ic_prefs_profile_person, SettingsPage.PROFILE, R.string.prefs_profile_title),
    SettingsSearchItem(R.string.profile_add_person, R.drawable.ic_add_black_24dp, SettingsPage.PROFILE, R.string.prefs_profile_title),
    SettingsSearchItem(R.string.profile_manage_persons, R.drawable.account_group_outline, SettingsPage.PROFILE, R.string.prefs_profile_title),
    SettingsSearchItem(R.string.profile_verification_interval, R.drawable.ic_hours_24, SettingsPage.PROFILE, R.string.prefs_profile_title),
    SettingsSearchItem(R.string.profile_reset, R.drawable.ic_prefs_profile_delete, SettingsPage.PROFILE, R.string.prefs_profile_title),

    // Appearance
    SettingsSearchItem(R.string.appearance_theme_title, R.drawable.ic_pref_appearance, SettingsPage.APPEARANCE, R.string.prefs_appearance_title),

    // Layout
    SettingsSearchItem(R.string.profile_show_person_input, R.drawable.form_dropdown, SettingsPage.LAYOUT, R.string.prefs_layout_title),

    // Behavior
    SettingsSearchItem(R.string.prefs_behavior_allow_blank_male_title, R.drawable.ic_setting_pattern_create, SettingsPage.BEHAVIOR, R.string.prefs_behavior_title),

    // Printing
    SettingsSearchItem(R.string.prefs_zebra_device_title, R.drawable.ic_setting_print_connect, SettingsPage.PRINTING, R.string.prefs_printing_title),
    SettingsSearchItem(R.string.prefs_zebra_print_connect_title, R.drawable.ic_setting_print_connect, SettingsPage.PRINTING, R.string.prefs_printing_title),
    SettingsSearchItem(R.string.zpl_label_setup_title, R.drawable.ic_receipt_long_black_24dp, SettingsPage.PRINTING, R.string.prefs_printing_title),
    SettingsSearchItem(R.string.prefs_cross_zpl_template_title, R.drawable.ic_receipt_long_black_24dp, SettingsPage.PRINTING, R.string.prefs_printing_title),
    SettingsSearchItem(R.string.prefs_parent_zpl_template_title, R.drawable.ic_receipt_long_black_24dp, SettingsPage.PRINTING, R.string.prefs_printing_title),

    // Database
    SettingsSearchItem(R.string.storage_definer_title, R.drawable.ic_folder_lock, SettingsPage.DATABASE, R.string.prefs_database_title),
    SettingsSearchItem(R.string.prefs_database_import_title, R.drawable.ic_database_import, SettingsPage.DATABASE, R.string.prefs_database_title),
    SettingsSearchItem(R.string.prefs_database_export_title, R.drawable.ic_database_export, SettingsPage.DATABASE, R.string.prefs_database_title),
    SettingsSearchItem(R.string.prefs_database_reload_title, R.drawable.ic_database_reload, SettingsPage.DATABASE, R.string.prefs_database_title),
    SettingsSearchItem(R.string.prefs_database_reset_title, R.drawable.ic_database_reset, SettingsPage.DATABASE, R.string.prefs_database_title),

    // BrAPI
    SettingsSearchItem(R.string.preferences_brapi_enable_title, R.drawable.ic_adv_brapi, SettingsPage.BRAPI, R.string.prefs_brapi_title),
    SettingsSearchItem(R.string.brapi_base_url, R.drawable.ic_adv_brapi, SettingsPage.BRAPI, R.string.prefs_brapi_title),
    SettingsSearchItem(R.string.brapi_display_name, R.drawable.ic_pref_brapi_name, SettingsPage.BRAPI, R.string.prefs_brapi_title),
    SettingsSearchItem(R.string.preferences_brapi_version, R.drawable.ic_pref_brapi_version, SettingsPage.BRAPI, R.string.prefs_brapi_title),
    SettingsSearchItem(R.string.brapi_pagination, R.drawable.ic_pref_brapi_pagination, SettingsPage.BRAPI, R.string.prefs_brapi_title),
    SettingsSearchItem(R.string.brapi_timeout, R.drawable.ic_pref_brapi_timeout, SettingsPage.BRAPI, R.string.prefs_brapi_title),
    SettingsSearchItem(R.string.menu_action_brapi_pref_title, R.drawable.ic_adv_brapi, SettingsPage.BRAPI, R.string.prefs_brapi_title),
)
