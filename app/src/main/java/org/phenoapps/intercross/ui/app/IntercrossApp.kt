package org.phenoapps.intercross.ui.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.ui.barcode.BARCODE_MODE_SINGLE
import org.phenoapps.intercross.ui.barcode.BARCODE_RESULT_KEY
import org.phenoapps.intercross.ui.barcode.BARCODE_SEQUENCE_RESULT_KEY
import org.phenoapps.intercross.ui.barcode.BarcodeScannerRoute
import org.phenoapps.intercross.ui.brapi.BrapiCrossImportRoute
import org.phenoapps.intercross.ui.brapi.BrapiExportSummaryRoute
import org.phenoapps.intercross.ui.brapi.BrapiPlannedCrossesRoute
import org.phenoapps.intercross.ui.brapi.BrapiPotentialParentsRoute
import org.phenoapps.intercross.ui.brapi.BrapiProjectsRoute
import org.phenoapps.intercross.ui.crosstracker.CrossBlockRoute
import org.phenoapps.intercross.ui.crosstracker.CrossTrackerRoute
import org.phenoapps.intercross.ui.crosstracker.WishlistDetailRoute
import org.phenoapps.intercross.ui.events.EventDetailRoute
import org.phenoapps.intercross.ui.events.EventsRoute
import org.phenoapps.intercross.ui.parents.ParentCreatorRoute
import org.phenoapps.intercross.ui.parents.ParentsRoute
import org.phenoapps.intercross.ui.pollenmanager.PollenManagerRoute
import org.phenoapps.intercross.ui.settings.AboutSettingsRoute
import org.phenoapps.intercross.ui.settings.AppearanceSettingsRoute
import org.phenoapps.intercross.ui.settings.BehaviorSettingsRoute
import org.phenoapps.intercross.ui.settings.BrapiAdvancedSettingsRoute
import org.phenoapps.intercross.ui.settings.BrapiSettingsRoute
import org.phenoapps.intercross.ui.settings.DatabaseSettingsRoute
import org.phenoapps.intercross.ui.settings.LayoutSettingsRoute
import org.phenoapps.intercross.ui.settings.MetadataSettingsRoute
import org.phenoapps.intercross.ui.settings.PatternSettingsRoute
import org.phenoapps.intercross.ui.settings.PrintingSettingsRoute
import org.phenoapps.intercross.ui.settings.ProfileSettingsRoute
import org.phenoapps.intercross.ui.settings.SettingsRoute
import org.phenoapps.intercross.ui.summary.SummaryRoute
import org.phenoapps.intercross.ui.wishlist.WishlistFactoryRoute
import org.phenoapps.intercross.util.FileUtil

@Composable
fun IntercrossApp(actions: IntercrossAppActions) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(navController) {
        actions.onNavigateToBrapiProjects { mode ->
            navController.navigate(IntercrossRoute.BrapiProjects.create(mode))
        }
        actions.onNavigateToProfileSettings {
            navController.navigate(IntercrossRoute.ProfileSettings.route)
        }
    }

    IntercrossScaffold(
        navController = navController,
        snackbarHostState = snackbarHostState,
        actions = actions,
    ) { modifier ->
        IntercrossNavHost(
            navController = navController,
            modifier = modifier,
            onShowMessage = { message ->
                coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            },
            actions = actions,
        )
    }
}

@Composable
private fun IntercrossScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    actions: IntercrossAppActions,
    content: @Composable (Modifier) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    BackHandler {
        if (route == IntercrossRoute.Events.route) {
            actions.onHomeBack()
        } else if (!navController.popBackStack()) {
            actions.onHomeBack()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(Modifier)
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun IntercrossNavHost(
    navController: NavHostController,
    modifier: Modifier,
    onShowMessage: (String) -> Unit,
    actions: IntercrossAppActions,
) {
    var brapiPotentialParentsArgs by remember { mutableStateOf<Bundle?>(null) }
    var brapiPlannedCrossesArgs by remember { mutableStateOf<Bundle?>(null) }
    var brapiCrossImportArgs by remember { mutableStateOf<Bundle?>(null) }
    var brapiExportSummaryArgs by remember { mutableStateOf<Bundle?>(null) }

    // â”€â”€â”€ Person Verification Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val (prefs, keyUtil) = rememberPrefs()
    var showVerificationDialog by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf("") }

    val verifyCollectorTemplate = stringResource(R.string.collect_dialog_verify_collector)
    val newCollectorText = stringResource(R.string.collect_dialog_new_collector)

    LaunchedEffect(Unit) {
        val lastOpen = prefs.getLong(keyUtil.lastTimeAskedKey, 0L)
        val alreadyAsked = prefs.getBoolean(keyUtil.askedSinceOpenedKey, false)
        val systemTime = System.nanoTime()
        val interval = when (prefs.getString(keyUtil.personVerificationIntervalKey, "0")) {
            "0" -> 0
            "1" -> 12
            "2" -> 24
            else -> -1
        }
        val nanosToWait = 1_000_000_000L * 3600L * interval

        if (!alreadyAsked && interval >= 0) {
            if (interval == 0 || (lastOpen != 0L && (systemTime - lastOpen > nanosToWait))) {
                val firstName = prefs.getString(keyUtil.personFirstNameKey, "") ?: ""
                val lastName = prefs.getString(keyUtil.personLastNameKey, "") ?: ""
                verificationMessage = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                    "$verifyCollectorTemplate $firstName $lastName?"
                } else {
                    newCollectorText
                }
                showVerificationDialog = true
                prefs.edit().putLong(keyUtil.lastTimeAskedKey, System.nanoTime()).apply()
                prefs.edit().putBoolean(keyUtil.askedSinceOpenedKey, true).apply()
            }
        }
    }

    if (showVerificationDialog) {
        val firstName = prefs.getString(keyUtil.personFirstNameKey, "") ?: ""
        val lastName = prefs.getString(keyUtil.personLastNameKey, "") ?: ""
        val hasName = firstName.isNotEmpty() || lastName.isNotEmpty()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showVerificationDialog = false },
            title = { Text(verificationMessage) },
            confirmButton = {
                TextButton(onClick = { showVerificationDialog = false }) {
                    Text(
                        if (hasName) stringResource(R.string.collect_dialog_verify_yes_button)
                        else stringResource(R.string.collect_dialog_verify_no_button)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVerificationDialog = false
                    navController.navigate(IntercrossRoute.ProfileSettings.route)
                }) {
                    Text(
                        if (hasName) stringResource(R.string.collect_dialog_verify_no_button)
                        else stringResource(R.string.collect_dialog_verify_yes_button)
                    )
                }
            },
        )
    }

    NavHost(
        navController = navController,
        startDestination = IntercrossRoute.Events.route,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(IntercrossRoute.Events.route) {
            val scanResult by it.savedStateHandle
                .getStateFlow<String?>(BARCODE_RESULT_KEY, null)
                .collectAsStateWithLifecycle()
            val scanSequenceResult by it.savedStateHandle
                .getStateFlow<ArrayList<String>?>(BARCODE_SEQUENCE_RESULT_KEY, null)
                .collectAsStateWithLifecycle()
            val prefillFemale by it.savedStateHandle
                .getStateFlow<String?>(NavKeys.PREFILL_FEMALE, null)
                .collectAsStateWithLifecycle()
            val prefillMale by it.savedStateHandle
                .getStateFlow<String?>(NavKeys.PREFILL_MALE, null)
                .collectAsStateWithLifecycle()
            val bottomBarState = remember {
                BottomBarState(
                    selectedRoute = IntercrossRoute.Events.route,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            EventsRoute(
                onOpenEvent = { navController.navigate(IntercrossRoute.EventDetail.create(it)) },
                onOpenScanner = { navController.navigate(IntercrossRoute.BarcodeScanner.create(it)) },
                scannerResult = scanResult,
                onScannerResultConsumed = { it.savedStateHandle[BARCODE_RESULT_KEY] = null },
                scannerSequenceResult = scanSequenceResult,
                onScannerSequenceResultConsumed = { it.savedStateHandle[BARCODE_SEQUENCE_RESULT_KEY] = null },
                onShowMessage = onShowMessage,
                prefillFemale = prefillFemale,
                prefillMale = prefillMale,
                onPrefillConsumed = {
                    it.savedStateHandle[NavKeys.PREFILL_FEMALE] = null
                    it.savedStateHandle[NavKeys.PREFILL_MALE] = null
                },
                onImportLocal = { actions.launchImportLocal() },
                onImportBrapi = { navController.navigate(IntercrossRoute.BrapiProjects.create(BrapiMode.IMPORT_CROSSES)) },
                onExportLocal = { actions.launchExportLocal() },
                onExportBrapi = { navController.navigate(IntercrossRoute.BrapiProjects.create(BrapiMode.EXPORT_CROSSES)) },
                bottomBarState = bottomBarState,
            )
        }
        composable(IntercrossRoute.CrossTracker.route) {
            val bottomBarState = remember {
                BottomBarState(
                    selectedRoute = IntercrossRoute.CrossTracker.route,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            CrossTrackerRoute(
                bottomBarState = bottomBarState,
                onOpenEvent = { navController.navigate(IntercrossRoute.EventDetail.create(it)) },
                onOpenCrossBlock = { navController.navigate(IntercrossRoute.CrossBlock.route) },
                onCreateWishlist = { navController.navigate(IntercrossRoute.WishlistFactory.route) },
                onImportLocal = { actions.launchImportLocal() },
                onImportBrapi = { navController.navigate(IntercrossRoute.BrapiProjects.create(BrapiMode.WISHLIST)) },
                onMakeCross = { femaleId, maleId ->
                    navController.getBackStackEntry(IntercrossRoute.Events.route).savedStateHandle[NavKeys.PREFILL_FEMALE] = femaleId
                    navController.getBackStackEntry(IntercrossRoute.Events.route).savedStateHandle[NavKeys.PREFILL_MALE] = maleId
                    navController.navigate(IntercrossRoute.Events.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenWishlistDetail = { fid, mid, fname, mname ->
                    navController.navigate(IntercrossRoute.WishlistDetail.create(fid, mid, fname, mname))
                },
            )
        }
        composable(IntercrossRoute.CrossBlock.route) {
            CrossBlockRoute(
                onOpenEvent = { navController.navigate(IntercrossRoute.EventDetail.create(it)) },
                onCreateWishlist = { navController.navigate(IntercrossRoute.WishlistFactory.route) },
                onMakeCross = { femaleId, maleId ->
                    navController.getBackStackEntry(IntercrossRoute.Events.route).savedStateHandle[NavKeys.PREFILL_FEMALE] = femaleId
                    navController.getBackStackEntry(IntercrossRoute.Events.route).savedStateHandle[NavKeys.PREFILL_MALE] = maleId
                    navController.navigate(IntercrossRoute.Events.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = IntercrossRoute.WishlistDetail.route,
            arguments = listOf(
                navArgument("female") { defaultValue = "" },
                navArgument("male") { defaultValue = "" },
                navArgument("fname") { defaultValue = "" },
                navArgument("mname") { defaultValue = "" },
            ),
        ) {
            val femaleId = it.arguments?.getString("female").orEmpty()
            val maleId = it.arguments?.getString("male").orEmpty()
            WishlistDetailRoute(
                femaleId = femaleId,
                maleId = maleId,
                femaleName = it.arguments?.getString("fname").orEmpty(),
                maleName = it.arguments?.getString("mname").orEmpty(),
                onOpenEvent = { eventId -> navController.navigate(IntercrossRoute.EventDetail.create(eventId)) },
                onMakeCross = {
                    navController.getBackStackEntry(IntercrossRoute.Events.route).savedStateHandle[NavKeys.PREFILL_FEMALE] = femaleId
                    navController.getBackStackEntry(IntercrossRoute.Events.route).savedStateHandle[NavKeys.PREFILL_MALE] = maleId
                    navController.navigate(IntercrossRoute.Events.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(IntercrossRoute.Parents.route) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val db = rememberDatabase()
            val parentsImportModel: ParentsListViewModel = viewModel(
                factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
            )
            val importSuccessText = stringResource(R.string.database_import_success)
            val parentImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val tables = FileUtil(context).parseInputFile(it)
                            if (tables[1].isNotEmpty()) {
                                parentsImportModel.insert(*tables[1].filterIsInstance<Parent>().toTypedArray())
                            }
                        }
                        onShowMessage(importSuccessText)
                    }
                }
            }
            val bottomBarState = remember {
                BottomBarState(
                    selectedRoute = IntercrossRoute.Parents.route,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            ParentsRoute(
                bottomBarState = bottomBarState,
                onCreateParent = { navController.navigate(IntercrossRoute.ParentCreator.create(it)) },
                onShowMessage = onShowMessage,
                onImportFile = { parentImportLauncher.launch("*/*") },
                onImportBrapi = { navController.navigate(IntercrossRoute.BrapiProjects.create(1)) },
            )
        }
        composable(
            route = IntercrossRoute.ParentCreator.route,
            arguments = listOf(navArgument("mode") { type = NavType.IntType }),
        ) {
            val mode = it.arguments?.getInt("mode") ?: 0
            ParentCreatorRoute(
                mode = mode,
                onDone = { navController.navigate(IntercrossRoute.Parents.route) { popUpTo(IntercrossRoute.Parents.route) } },
                onOpenPollenGroup = { code, name -> navController.navigate(IntercrossRoute.PollenManager.create(code, name)) },
                onShowMessage = onShowMessage,
            )
        }
        composable(
            route = IntercrossRoute.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
        ) {
            EventDetailRoute(
                eventId = it.arguments?.getLong("eventId") ?: -1L,
                onBack = { navController.popBackStack() },
                onShowMessage = onShowMessage,
                onNavigateToEvent = { eid -> navController.navigate(IntercrossRoute.EventDetail.create(eid)) },
            )
        }
        composable(IntercrossRoute.Summary.route) {
            val bottomBarState = remember {
                BottomBarState(
                    selectedRoute = IntercrossRoute.Summary.route,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            SummaryRoute(bottomBarState = bottomBarState)
        }
        composable(IntercrossRoute.Settings.route) {
            val bottomBarState = remember {
                BottomBarState(
                    selectedRoute = IntercrossRoute.Settings.route,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            SettingsRoute(
                bottomBarState = bottomBarState,
                onOpenPage = { page ->
                    when (page) {
                        SettingsPage.PROFILE -> navController.navigate(IntercrossRoute.ProfileSettings.route)
                        SettingsPage.LAYOUT -> navController.navigate(IntercrossRoute.LayoutSettings.route)
                        SettingsPage.BEHAVIOR -> navController.navigate(IntercrossRoute.BehaviorSettings.route)
                        SettingsPage.PRINTING -> navController.navigate(IntercrossRoute.PrintingSettings.route)
                        SettingsPage.DATABASE -> navController.navigate(IntercrossRoute.DatabaseSettings.route)
                        SettingsPage.BRAPI -> navController.navigate(IntercrossRoute.BrapiSettings.route)
                        SettingsPage.ABOUT -> navController.navigate(IntercrossRoute.AboutSettings.route)
                        SettingsPage.APPEARANCE -> navController.navigate(IntercrossRoute.AppearanceSettings.route)
                    }
                },
            )
        }
        composable(IntercrossRoute.ProfileSettings.route) {
            ProfileSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.LayoutSettings.route) {
            LayoutSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.BehaviorSettings.route) {
            BehaviorSettingsRoute(
                onOpenPattern = { navController.navigate(IntercrossRoute.PatternSettings.route) },
                onOpenMetadata = { navController.navigate(IntercrossRoute.MetadataSettings.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(IntercrossRoute.PrintingSettings.route) {
            PrintingSettingsRoute(
                onOpenLabelDesigner = { navController.navigate(IntercrossRoute.LabelTemplateEditor.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(IntercrossRoute.LabelTemplateEditor.route) {
            LabelTemplateEditorRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.PatternSettings.route) {
            PatternSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.DatabaseSettings.route) {
            DatabaseSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.BrapiSettings.route) {
            val scanResult by it.savedStateHandle
                .getStateFlow<String?>(BARCODE_RESULT_KEY, null)
                .collectAsStateWithLifecycle()
            BrapiSettingsRoute(
                onShowMessage = onShowMessage,
                onBack = { navController.popBackStack() },
                onNavigateToAdvancedSettings = { navController.navigate(IntercrossRoute.BrapiAdvancedSettings.route) },
                onOpenScanner = { navController.navigate(IntercrossRoute.BarcodeScanner.create(BARCODE_MODE_SINGLE)) },
                scanResult = scanResult,
                onScanResultConsumed = { it.savedStateHandle[BARCODE_RESULT_KEY] = null },
            )
        }
        composable(IntercrossRoute.BrapiAdvancedSettings.route) {
            BrapiAdvancedSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.MetadataSettings.route) {
            MetadataSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.AboutSettings.route) {
            AboutSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.AppearanceSettings.route) {
            AppearanceSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(IntercrossRoute.WishlistFactory.route) {
            WishlistFactoryRoute(
                onDone = {
                    navController.navigate(IntercrossRoute.CrossTracker.route) {
                        popUpTo(IntercrossRoute.CrossTracker.route)
                        launchSingleTop = true
                    }
                },
                onShowMessage = onShowMessage,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = IntercrossRoute.BarcodeScanner.route,
            arguments = listOf(navArgument("mode") { type = NavType.IntType }),
        ) {
            val mode = it.arguments?.getInt("mode") ?: BARCODE_MODE_SINGLE
            BarcodeScannerRoute(
                mode = mode,
                onSingleScan = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(BARCODE_RESULT_KEY, result)
                    navController.popBackStack()
                },
                onSequenceScan = { results ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        BARCODE_SEQUENCE_RESULT_KEY,
                        ArrayList(results),
                    )
                },
                onOpenEvent = { eventId ->
                    navController.navigate(IntercrossRoute.EventDetail.create(eventId)) {
                        popUpTo(IntercrossRoute.Events.route)
                    }
                },
                onShowMessage = onShowMessage,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = IntercrossRoute.BrapiProjects.route,
            arguments = listOf(navArgument("mode") { type = NavType.IntType }),
        ) {
            BrapiProjectsRoute(
                mode = it.arguments?.getInt("mode") ?: BrapiMode.IMPORT_CROSSES,
                onBack = { navController.popBackStack() },
                onOpenDetail = { destination, args ->
                    when (destination) {
                        BrapiDetailDestination.PotentialParents -> {
                            brapiPotentialParentsArgs = args
                            navController.navigate(IntercrossRoute.BrapiPotentialParents.route)
                        }
                        BrapiDetailDestination.PlannedCrosses -> {
                            brapiPlannedCrossesArgs = args
                            navController.navigate(IntercrossRoute.BrapiPlannedCrosses.route)
                        }
                        BrapiDetailDestination.CrossImport -> {
                            brapiCrossImportArgs = args
                            navController.navigate(IntercrossRoute.BrapiCrossImport.route)
                        }
                        BrapiDetailDestination.ExportSummary -> {
                            brapiExportSummaryArgs = args
                            navController.navigate(IntercrossRoute.BrapiExportSummary.route)
                        }
                    }
                },
                onShowMessage = onShowMessage,
            )
        }
        composable(IntercrossRoute.BrapiPotentialParents.route) {
            val args = brapiPotentialParentsArgs
            if (args == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                BrapiPotentialParentsRoute(
                    args = args,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(IntercrossRoute.Parents.route) {
                            popUpTo(IntercrossRoute.Parents.route)
                            launchSingleTop = true
                        }
                    },
                    onShowMessage = onShowMessage,
                )
            }
        }
        composable(IntercrossRoute.BrapiPlannedCrosses.route) {
            val args = brapiPlannedCrossesArgs
            if (args == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                BrapiPlannedCrossesRoute(
                    args = args,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(IntercrossRoute.CrossTracker.route) {
                            popUpTo(IntercrossRoute.CrossTracker.route)
                            launchSingleTop = true
                        }
                    },
                    onShowMessage = onShowMessage,
                )
            }
        }
        composable(IntercrossRoute.BrapiCrossImport.route) {
            val args = brapiCrossImportArgs
            if (args == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                BrapiCrossImportRoute(
                    args = args,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(IntercrossRoute.Events.route) {
                            popUpTo(IntercrossRoute.Events.route)
                            launchSingleTop = true
                        }
                    },
                    onShowMessage = onShowMessage,
                )
            }
        }
        composable(IntercrossRoute.BrapiExportSummary.route) {
            val args = brapiExportSummaryArgs
            if (args == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                BrapiExportSummaryRoute(
                    args = args,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(IntercrossRoute.Events.route) {
                            popUpTo(IntercrossRoute.Events.route)
                            launchSingleTop = true
                        }
                    },
                    onShowMessage = onShowMessage,
                )
            }
        }
        composable(
            route = IntercrossRoute.PollenManager.route,
            arguments = listOf(
                navArgument("code") { defaultValue = "" },
                navArgument("name") { defaultValue = "" },
            ),
        ) {
            PollenManagerRoute(
                codeId = it.arguments?.getString("code").orEmpty(),
                readableName = it.arguments?.getString("name").orEmpty(),
                onDone = {
                    navController.navigate(IntercrossRoute.Parents.route) {
                        popUpTo(IntercrossRoute.Parents.route)
                        launchSingleTop = true
                    }
                },
                onShowMessage = onShowMessage,
            )
        }
    }
}
