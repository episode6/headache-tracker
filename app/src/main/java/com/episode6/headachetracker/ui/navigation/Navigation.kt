package com.episode6.headachetracker.ui.navigation

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.episode6.headachetracker.R
import com.episode6.headachetracker.ui.calendar.CalendarScreen
import com.episode6.headachetracker.ui.calendar.CalendarViewModel
import com.episode6.headachetracker.ui.calendar.FullYearScreen
import com.episode6.headachetracker.ui.calendar.FullYearViewModel
import com.episode6.headachetracker.ui.edit.EditScreen
import com.episode6.headachetracker.ui.licenses.LicensesScreen
import com.episode6.headachetracker.ui.edit.EditViewModel
import com.episode6.headachetracker.ui.notes.NotesSummaryScreen
import com.episode6.headachetracker.ui.notes.NotesSummaryViewModel
import com.episode6.headachetracker.ui.settings.DataTransferMessage
import com.episode6.headachetracker.ui.settings.SettingsScreen
import com.episode6.headachetracker.ui.settings.SettingsViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HeadacheTrackerNavigation(initialEditDate: String? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Calendar) {
        composable<Route.Calendar> {
            AdaptiveCalendarScreen(
                initialSelectedDate = initialEditDate,
                onNavigateToFullYear = { year ->
                    navController.navigate(Route.FullYear(year))
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings)
                },
            )
        }
        composable<Route.Settings> {
            val context = LocalContext.current
            val resources = LocalResources.current
            val viewModel: SettingsViewModel = metroViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportTo(uri)
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    viewModel.importFrom(uri)
                }
            }

            val autoExportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri != null) {
                    viewModel.onAutoExportFileSelected(uri)
                }
            }

            LaunchedEffect(viewModel) {
                viewModel.triggerAutoExportFilePicker.collectLatest {
                    autoExportLauncher.launch(resources.getString(R.string.export_filename))
                }
            }

            LaunchedEffect(viewModel) {
                viewModel.dataTransferMessages.collectLatest { message ->
                    val text = when (message) {
                        is DataTransferMessage.ExportSuccess -> resources.getString(
                            R.string.export_success,
                            message.entryCount,
                        )
                        is DataTransferMessage.ImportSuccess -> resources.getString(
                            R.string.import_success,
                            message.entryCount,
                        )
                        is DataTransferMessage.Error -> message.message
                    }
                    snackbarHostState.showSnackbar(text)
                }
            }

            SettingsScreen(
                state = state,
                snackbarHostState = snackbarHostState,
                onReminderMinutesChanged = { viewModel.onReminderMinutesChanged(it) },
                onMorningCheckInToggled = { viewModel.onMorningCheckInToggled(it) },
                onMorningCheckInTimeChanged = { hour, minute ->
                    viewModel.onMorningCheckInTimeChanged(hour, minute)
                },
                onOpenNotificationSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                },
                onExportClick = {
                    exportLauncher.launch(resources.getString(R.string.export_filename))
                },
                onImportClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                onAutoExportToggled = { enabled ->
                    viewModel.onAutoExportToggled(enabled)
                },
                onLicensesClick = { navController.navigate(Route.Licenses) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.Licenses> {
            LicensesScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.FullYear> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.FullYear>()
            val viewModel: FullYearViewModel =
                assistedMetroViewModel<FullYearViewModel, FullYearViewModel.Factory>(
                    key = route.year.toString(),
                ) { create(route.year) }
            val state by viewModel.state.collectAsStateWithLifecycle()

            FullYearScreen(
                state = state,
                onYearChanged = { viewModel.onYearChanged(it) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveCalendarScreen(
    onNavigateToFullYear: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    initialSelectedDate: String? = null,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var selectedDate by rememberSaveable { mutableStateOf(initialSelectedDate) }
    // The detail pane shows either the edit screen (selectedDate != null) or the notes
    // summary; the two are mutually exclusive and every event that opens one clears the other.
    var showNotesSummary by rememberSaveable { mutableStateOf(false) }
    val detailOpen = selectedDate != null || showNotesSummary
    // Retains the last detail content so the pane keeps rendering while animating out
    // (selectedDate/showNotesSummary reset immediately on back/save, before the exit
    // transition finishes).
    var lastEditDate by rememberSaveable { mutableStateOf(initialSelectedDate) }
    var lastDetailWasNotes by rememberSaveable { mutableStateOf(false) }
    selectedDate?.let { lastEditDate = it }
    if (detailOpen) {
        lastDetailWasNotes = showNotesSummary
    }
    // Set when a notes-summary row is tapped side-by-side; consumed by the calendar
    // once it has animated to that month.
    var revealMonth by remember { mutableStateOf<YearMonth?>(null) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    val isSideBySide = directive.maxHorizontalPartitions > 1

    val scaffoldValue = calculateThreePaneScaffoldValue(
        maxHorizontalPartitions = if (!detailOpen) 1 else directive.maxHorizontalPartitions,
        adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
        currentDestination = ThreePaneScaffoldDestinationItem<String>(
            if (!detailOpen) ListDetailPaneScaffoldRole.List else ListDetailPaneScaffoldRole.Detail
        )
    )

    ListDetailPaneScaffold(
        directive = directive,
        value = scaffoldValue,
        listPane = {
            AnimatedPane {
                val viewModel: CalendarViewModel = metroViewModel()
                val state by viewModel.state.collectAsState()

                CalendarScreen(
                    state = state,
                    onMonthChanged = { viewModel.onMonthChanged(it) },
                    onYearSelected = { viewModel.onYearSelected(it) },
                    onDayClick = { date ->
                        showNotesSummary = false
                        selectedDate = date.toString()
                    },
                    onFullYearClick = {
                        onNavigateToFullYear(state.selectedMonth.year)
                    },
                    onNotesSummaryClick = {
                        selectedDate = null
                        showNotesSummary = true
                    },
                    onSettingsClick = onNavigateToSettings,
                    onTodayEntryClick = {
                        showNotesSummary = false
                        selectedDate = LocalDate.now().toString()
                    },
                    highlightNotedDays = isSideBySide && showNotesSummary,
                    smoothScrollToMonth = revealMonth,
                    onSmoothScrollHandled = { revealMonth = null },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (lastDetailWasNotes) {
                    val viewModel: NotesSummaryViewModel = metroViewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    NotesSummaryScreen(
                        state = state,
                        onBack = { showNotesSummary = false },
                        onEntryClick = if (isSideBySide) {
                            { date -> revealMonth = YearMonth.from(date) }
                        } else null,
                    )
                } else {
                    val dateKey = lastEditDate
                    if (dateKey != null) {
                        val viewModel: EditViewModel =
                            assistedMetroViewModel<EditViewModel, EditViewModel.Factory>(
                                key = dateKey,
                            ) { create(dateKey) }
                        val state by viewModel.state.collectAsState()
                        EditScreen(
                            state = state,
                            onIntensityChanged = { viewModel.onIntensityChanged(it) },
                            onPillsTakenChanged = { viewModel.onPillsTakenChanged(it) },
                            onFirstPillTimeChanged = { viewModel.onFirstPillTimeChanged(it) },
                            onSecondPillTimeChanged = { viewModel.onSecondPillTimeChanged(it) },
                            onNotesChanged = { viewModel.onNotesChanged(it) },
                            onSave = {
                                viewModel.saveEntry {
                                    if (isSideBySide) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.entry_saved_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        selectedDate = null
                                    }
                                }
                            },
                            onBack = {
                                selectedDate = null
                            },
                        )
                    }
                }
            }
        }
    )

    BackHandler(enabled = detailOpen) {
        selectedDate = null
        showNotesSummary = false
    }
}
