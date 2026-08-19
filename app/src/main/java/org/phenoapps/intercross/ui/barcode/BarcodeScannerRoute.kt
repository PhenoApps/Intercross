package org.phenoapps.intercross.ui.barcode

import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.util.BarcodeScannerSettings
import org.phenoapps.intercross.util.CrossIdSettings
import org.phenoapps.intercross.util.CrossUtil
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val BARCODE_MODE_SINGLE = 0
const val BARCODE_MODE_SEARCH = 1
const val BARCODE_MODE_CONTINUOUS = 2
const val BARCODE_RESULT_KEY = "barcode_result"
const val BARCODE_SEQUENCE_RESULT_KEY = "barcode_sequence_result"

@Composable
fun BarcodeScannerRoute(
    mode: Int,
    onSingleScan: (String) -> Unit,
    onSequenceScan: (List<String>) -> Unit = {},
    onOpenEvent: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = rememberDatabase()
    val (prefs, keyUtil) = rememberPrefs()
    val eventsModel: EventListViewModel = viewModel(
        factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())),
    )
    val parentsModel: ParentsListViewModel = viewModel(
        factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())),
    )
    val wishModel: WishlistViewModel = viewModel(
        factory = WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao())),
    )
    val metadataModel: MetadataViewModel = viewModel(
        factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())),
    )
    val metaValuesModel: MetaValuesViewModel = viewModel(
        factory = MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao())),
    )
    val events by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val parents by parentsModel.parents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metadata by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val wishes by wishModel.wishes.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val mlKitFormats = remember(prefs, keyUtil.barcodeFormatsKey) {
        BarcodeScannerSettings.selectedMlKitFormats(prefs, keyUtil.barcodeFormatsKey)
    }
    var flashEnabled by remember(prefs, keyUtil.barcodeFlashKey) {
        mutableStateOf(prefs.getBoolean(keyUtil.barcodeFlashKey, false))
    }
    val maleFirst = prefs.getBoolean(keyUtil.crossOrderKey, false)
    val blankMale = prefs.getBoolean(keyUtil.blankMaleKey, false)
    fun currentCrossIdSettings() = CrossIdSettings.load(prefs)
    fun initialCrossId(): String {
        val settings = CrossIdSettings.load(prefs)
        return when {
            settings.isPattern -> settings.pattern
            settings.isUUID -> UUID.randomUUID().toString()
            else -> ""
        }
    }
    var sequenceFemale by remember(mode) { mutableStateOf("") }
    var sequenceMale by remember(mode) { mutableStateOf("") }
    var sequenceCrossId by remember(mode) { mutableStateOf(initialCrossId()) }
    var sequenceResetNonce by remember(mode) { mutableStateOf(0) }
    var sequenceSaving by remember(mode) { mutableStateOf(false) }
    var sequenceSaveCount by remember(mode) { mutableStateOf(0) }
    // Cooldown state: pause scanning briefly after a successful cross save
    var cooldownActive by remember(mode) { mutableStateOf(false) }
    var cooldownProgress by remember(mode) { mutableStateOf(0f) }
    val crossIdSettings = currentCrossIdSettings()
    val needsMale = !blankMale
    val needsCrossId = !crossIdSettings.isPattern && !crossIdSettings.isUUID

    val femaleNameRequired = stringResource(R.string.you_must_enter_female_name)
    val maleNameRequired = stringResource(R.string.you_must_enter_male_name)
    val crossNameRequired = stringResource(R.string.you_must_enter_cross_name)
    val crossExistsEvent = stringResource(R.string.cross_id_already_exists_as_event)
    val crossExistsParent = stringResource(R.string.cross_id_already_exists_as_parent)

    fun clearSequenceFields() {
        sequenceFemale = ""
        sequenceMale = ""
        sequenceCrossId = initialCrossId()
        sequenceResetNonce++
    }

    fun nextSequenceSlot(): SequenceScanSlot? {
        val orderedParentSlots = if (maleFirst) {
            listOf(SequenceScanSlot.Male, SequenceScanSlot.Female)
        } else {
            listOf(SequenceScanSlot.Female, SequenceScanSlot.Male)
        }
        return (orderedParentSlots + SequenceScanSlot.Cross).firstOrNull { slot ->
            when (slot) {
                SequenceScanSlot.Female -> !sequenceSaving && sequenceFemale.isBlank()
                SequenceScanSlot.Male -> !sequenceSaving && needsMale && sequenceMale.isBlank()
                SequenceScanSlot.Cross -> !sequenceSaving && needsCrossId && sequenceCrossId.isBlank()
            }
        }
    }

    fun saveSequenceCross(): Boolean {
        val crossIdSettings = CrossIdSettings.load(prefs)
        val female = sequenceFemale.trim()
        var male = sequenceMale.trim()
        val name = sequenceCrossId.trim()
        when {
            female.isBlank() -> {
                onShowMessage(femaleNameRequired)
                clearSequenceFields()
            }
            male.isBlank() && !blankMale -> {
                onShowMessage(maleNameRequired)
                clearSequenceFields()
            }
            name.isBlank() && !crossIdSettings.isPattern && !crossIdSettings.isUUID -> {
                onShowMessage(crossNameRequired)
                clearSequenceFields()
            }
            events.any { it.eventDbId == name } -> {
                onShowMessage(crossExistsEvent)
                clearSequenceFields()
            }
            parents.any { it.codeId == name } -> {
                onShowMessage(crossExistsParent)
                clearSequenceFields()
            }
            else -> {
                if (male.isBlank()) male = "blank"
                sequenceSaving = true
                scope.launch {
                    try {
                        val eventId = withContext(Dispatchers.IO) {
                            CrossUtil(context).submitCrossEvent(
                                activity = context as? FragmentActivity,
                                female = female,
                                male = male,
                                crossName = name,
                                crossIdSettings = crossIdSettings,
                                eventsModel = eventsModel,
                                parents = parents,
                                parentModel = parentsModel,
                                wishlistProgress = wishes,
                                metaList = metadata,
                                metaValueModel = metaValuesModel,
                                suppressDialog = true,
                            )
                        }
                        sequenceSaveCount++
                        clearSequenceFields()
                        // Start cooldown period to prevent accidental double-scans
                        cooldownActive = true
                        cooldownProgress = 0f
                        launch {
                            val cooldownDurationMs = 3000L
                            val steps = 20
                            val stepDelay = cooldownDurationMs / steps
                            for (i in 1..steps) {
                                delay(stepDelay)
                                val p = i.toFloat() / steps
                                cooldownProgress = p
                                println("Cooldown progress: $p")
                            }
                            cooldownActive = false
                            cooldownProgress = 0f
                        }
                        if (prefs.getBoolean(keyUtil.openCrossAfterCreateKey, false)) {
                            onOpenEvent(eventId)
                        }
                    } catch (e: Exception) {
                        println("Error saving cross: ${e.message}")
                        onShowMessage("Error saving cross: ${e.message}")
                        clearSequenceFields()
                    } finally {
                        sequenceSaving = false
                    }
                }
                return true
            }
        }
        return false
    }

    fun applySequenceCode(code: String) {
        val slot = nextSequenceSlot() ?: return
        when (slot) {
            SequenceScanSlot.Female -> sequenceFemale = code
            SequenceScanSlot.Male -> sequenceMale = code
            SequenceScanSlot.Cross -> sequenceCrossId = code
        }
        if (nextSequenceSlot() == null) saveSequenceCross()
    }

    BarcodeScannerScreen(
        mode = mode,
        events = events,
        parents = parents,
        mlKitFormats = mlKitFormats,
        torchEnabled = flashEnabled,
        onSingleScan = onSingleScan,
        onSequenceScan = {},
        onSequenceCode = ::applySequenceCode,
        sequenceFemaleScanned = sequenceFemale.isNotBlank(),
        sequenceMaleScanned = sequenceMale.isNotBlank(),
        sequenceCrossScanned = sequenceCrossId.isNotBlank(),
        sequenceFemaleCode = sequenceFemale,
        sequenceMaleCode = sequenceMale,
        sequenceCrossIdCode = sequenceCrossId,
        sequenceNeedsMale = needsMale,
        sequenceNeedsCrossId = needsCrossId,
        sequenceNextSlot = nextSequenceSlot(),
        sequenceResetNonce = sequenceResetNonce,
        sequenceSaveCount = sequenceSaveCount,
        cooldownActive = cooldownActive,
        cooldownProgress = cooldownProgress,
        onOpenEvent = onOpenEvent,
        onShowMessage = onShowMessage,
        topBarState = TopBarState(
            titleRes = R.string.barcode_scan_label,
            showBack = true,
            onBack = onBack,
            actions = listOf(
                TopBarAction(
                    id = "toggle_flash",
                    labelRes = if (flashEnabled) R.string.barcode_flash_off else R.string.barcode_flash_on,
                    iconRes = if (flashEnabled) R.drawable.ic_flash_off else R.drawable.ic_flash_on,
                    onClick = {
                        flashEnabled = !flashEnabled
                        prefs.edit { putBoolean(keyUtil.barcodeFlashKey, flashEnabled) }
                    },
                ),
            ),
        ),
    )
}
