package com.episode6.headachetracker.ui.navigation

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.episode6.headachetracker.di.AppViewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.episode6.headachetracker.R
import com.episode6.headachetracker.appGraph
import com.episode6.headachetracker.ui.calendar.CalendarScreen
import com.episode6.headachetracker.ui.calendar.CalendarViewModel
import com.episode6.headachetracker.ui.calendar.DataTransferMessage
import com.episode6.headachetracker.ui.calendar.FullYearScreen
import com.episode6.headachetracker.ui.calendar.FullYearViewModel
import com.episode6.headachetracker.ui.edit.EditScreen
import com.episode6.headachetracker.ui.edit.EditViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HeadacheTrackerNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Calendar) {
        composable<Route.Calendar> {
            AdaptiveCalendarScreen(
                onNavigateToFullYear = { year ->
                    navController.navigate(Route.FullYear(year))
                },
            )
        }
        composable<Route.FullYear> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.FullYear>()
            val context = LocalContext.current
            val viewModelFactory = remember { context.appGraph.viewModelFactory }
            val viewModel: FullYearViewModel = viewModel(
                key = route.year.toString(),
                factory = viewModelFactory,
                extras = MutableCreationExtras().apply {
                    set(AppViewModelFactory.FullYearYearKey, route.year)
                },
            )
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
) {
    val context = LocalContext.current
    val viewModelFactory = remember { context.appGraph.viewModelFactory }
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    val isSideBySide = directive.maxHorizontalPartitions > 1

    val scaffoldValue = calculateThreePaneScaffoldValue(
        maxHorizontalPartitions = if (selectedDate == null) 1 else directive.maxHorizontalPartitions,
        adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
        currentDestination = ThreePaneScaffoldDestinationItem<String>(
            if (selectedDate == null) ListDetailPaneScaffoldRole.List else ListDetailPaneScaffoldRole.Detail
        )
    )

    ListDetailPaneScaffold(
        directive = directive,
        value = scaffoldValue,
        listPane = {
            AnimatedPane {
                val viewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
                val state by viewModel.state.collectAsState()
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
                        autoExportLauncher.launch(context.getString(R.string.export_filename))
                    }
                }

                LaunchedEffect(viewModel) {
                    viewModel.dataTransferMessages.collectLatest { message ->
                        val text = when (message) {
                            is DataTransferMessage.ExportSuccess -> context.getString(
                                R.string.export_success,
                                message.entryCount,
                            )
                            is DataTransferMessage.ImportSuccess -> context.getString(
                                R.string.import_success,
                                message.entryCount,
                            )
                            is DataTransferMessage.Error -> message.message
                        }
                        snackbarHostState.showSnackbar(text)
                    }
                }

                CalendarScreen(
                    state = state,
                    onMonthChanged = { viewModel.onMonthChanged(it) },
                    onYearSelected = { viewModel.onYearSelected(it) },
                    onDayClick = { date ->
                        selectedDate = date.toString()
                    },
                    onExportClick = {
                        exportLauncher.launch(context.getString(R.string.export_filename))
                    },
                    onImportClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    onFullYearClick = {
                        onNavigateToFullYear(state.selectedMonth.year)
                    },
                    onTodayEntryClick = {
                        selectedDate = LocalDate.now().toString()
                    },
                    onAutoExportToggled = { enabled ->
                        viewModel.onAutoExportToggled(enabled)
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (selectedDate != null) {
                    val dateKey = selectedDate!!
                    val viewModel: EditViewModel = viewModel(
                        key = dateKey,
                        factory = viewModelFactory,
                        extras = MutableCreationExtras().apply {
                            set(AppViewModelFactory.EditDateKey, dateKey)
                        },
                    )
                    val state by viewModel.state.collectAsState()
                    EditScreen(
                        state = state,
                        onIntensityChanged = { viewModel.onIntensityChanged(it) },
                        onPillsTakenChanged = { viewModel.onPillsTakenChanged(it) },
                        onFirstPillTimeChanged = { viewModel.onFirstPillTimeChanged(it) },
                        onSecondPillTimeChanged = { viewModel.onSecondPillTimeChanged(it) },
                        onSave = {
                            viewModel.saveEntry {
                                if (isSideBySide) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.entry_saved_toast),
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
    )

    BackHandler(enabled = selectedDate != null) {
        selectedDate = null
    }
}
