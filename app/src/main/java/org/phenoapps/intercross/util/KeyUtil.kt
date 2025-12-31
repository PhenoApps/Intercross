package org.phenoapps.intercross.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.phenoapps.intercross.R
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty


/**
 * Utility class for easily accessing preference keys.
 * Converts keys.xml into string fields to be accessed within a context.
 */
class KeyUtil @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {

    //explicitly state phenolib utils
    val brapiKeys by lazy {
        org.phenoapps.utils.KeyUtil(ctx)
    }

    private fun key(id: Int): ReadOnlyProperty<Any?, String> =
        ReadOnlyProperty { _, _ -> ctx.getString(id) }

    val firstRunKey by key(R.string.key_first_run)
    val loadSampleParents by key(R.string.key_load_sample_parents)
    val loadSampleWishlist by key(R.string.key_load_sample_wishlist)

    //non preference keys
    //boolean value to determine if brapi has been imported previously
    val brapiHasBeenImported by key(R.string.key_brapi_has_been_imported)

    // brapi preferences
    val brapiEnabled by key(R.string.key_pref_brapi_enabled)
    val brapiDisplayName by key(R.string.key_pref_brapi_display_name)

    // behavior preferences
    val behaviorRoot by key(R.string.root_behavior)
    val blankMaleKey by key(R.string.key_pref_behavior_blank_male)
    val crossOrderKey by key(R.string.key_pref_behavior_cross_order)
    val crossPatternKey by key(R.string.key_pref_behavior_cross_pattern)
    val collectAdditionalInfoKey by key(R.string.key_pref_behavior_collect_additional_info)
    val createMetadataKey by key(R.string.key_pref_behavior_meta_data)
    val manageMetadataKey by key(R.string.key_pref_behavior_meta_data_defaults)
    val soundNotificationKey by key(R.string.key_pref_behavior_sound_notifications)
    val openCrossAfterCreateKey by key(R.string.key_pref_behavior_open_cross_immediately)
    val commutativeCrossingKey by key(R.string.key_pref_behavior_commutative_crossing)

    // person and experiment preferences
    val profileRootKey by key(R.string.root_profile)
    val profilePersonKey by key(R.string.key_pref_profile_person)
    val profileResetKey by key(R.string.key_pref_profile_reset)
    val personFirstNameKey by key(R.string.key_pref_person_first_name)
    val personLastNameKey by key(R.string.key_pref_person_last_name)
    val experimentNameKey by key(R.string.key_pref_experiment_name)
    val personVerificationIntervalKey by key(R.string.key_pref_person_verification_interval)
    val askedSinceOpenedKey by key(R.string.key_pref_asked_since_opened)
    val lastTimeAskedKey by key(R.string.key_pref_last_time_asked)
    val modifyProfileKey by key(R.string.key_pref_modify_profile_settings)
    val personUpdateKey by key(R.string.key_pref_person_update)

    val printingRoot by key(R.string.root_printing)
    val printerConnectKey by key(R.string.key_pref_print_connect)
    val zplImportKey by key(R.string.key_pref_print_zpl_import)
    val zplCodeKey by key(R.string.key_pref_print_zpl_code)

    val databaseRoot by key(R.string.root_database)
    val dbStorageDefinerKey by key(R.string.key_pref_storage_definer)
    val dbImportKey by key(R.string.key_pref_db_import)
    val dbExportKey by key(R.string.key_pref_db_export)

    val aboutRoot by key(R.string.root_about)

    val showCompletedWishlistItems by key(R.string.key_show_completed_wishlist_items)
}
