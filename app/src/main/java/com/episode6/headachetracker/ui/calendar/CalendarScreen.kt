package com.episode6.headachetracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.episode6.headachetracker.R
import com.episode6.headachetracker.model.HeadacheEntry
import com.episode6.headachetracker.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

private const val PAGER_MONTHS_IN_PAST = 50 * 12
private const val PAGER_CENTER_PAGE = PAGER_MONTHS_IN_PAST
private const val PAGER_PAGE_COUNT = PAGER_CENTER_PAGE + 2
private val CompactWidthBreakpoint = 600.dp

private fun monthIndexFor(initialMonth: YearMonth, month: YearMonth): Int {
    return PAGER_CENTER_PAGE +
        (month.year - initialMonth.year) * 12 +
        (month.monthValue - initialMonth.monthValue)
}

private fun monthAtIndex(initialMonth: YearMonth, index: Int): YearMonth {
    return initialMonth.plusMonths((index - PAGER_CENTER_PAGE).toLong())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarState,
    onMonthChanged: (YearMonth) -> Unit,
    onYearSelected: (Int) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onFullYearClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTodayEntryClick: () -> Unit,
    onAutoExportToggled: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val initialMonth = remember { YearMonth.now() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        IconButton(onClick = { onMonthChanged(YearMonth.now()) }) {
                            Icon(Icons.Rounded.Today, contentDescription = "Go to Today")
                        }
                        YearSelectionDropdown(
                            currentYear = state.selectedMonth.year,
                            years = state.years,
                            onYearSelected = onYearSelected
                        )
                    }
                },
                actions = {
                    val availableMonths = remember(state.selectedMonth.year) {
                        val maxAllowed = YearMonth.now().plusMonths(1)
                        if (state.selectedMonth.year == maxAllowed.year) {
                            java.time.Month.entries.filter { it.value <= maxAllowed.monthValue }
                        } else {
                            java.time.Month.entries
                        }
                    }
                    MonthSelectionDropdown(
                        selectedMonth = state.selectedMonth.month,
                        availableMonths = availableMonths,
                        onMonthSelected = { month ->
                            onMonthChanged(state.selectedMonth.withMonth(month.value))
                        }
                    )
                    DataTransferMenu(
                        enabled = !state.isTransferInProgress,
                        autoExportEnabled = state.autoExportEnabled,
                        lastAutoExportStatus = state.lastAutoExportStatus,
                        onExportClick = onExportClick,
                        onImportClick = onImportClick,
                        onFullYearClick = onFullYearClick,
                        onSettingsClick = onSettingsClick,
                        onAutoExportToggled = onAutoExportToggled,
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onTodayEntryClick,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_head_circuit),
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.log_today)) },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                val useVerticalMonthScroll = maxHeight > maxWidth && maxWidth < CompactWidthBreakpoint

                if (useVerticalMonthScroll) {
                    CalendarVerticalMonthList(
                        initialMonth = initialMonth,
                        selectedMonth = state.selectedMonth,
                        entries = state.entries,
                        onMonthChanged = onMonthChanged,
                        onDayClick = onDayClick,
                    )
                } else {
                    CalendarHorizontalPager(
                        initialMonth = initialMonth,
                        selectedMonth = state.selectedMonth,
                        entries = state.entries,
                        onMonthChanged = onMonthChanged,
                        onDayClick = onDayClick,
                    )
                }
            }

            if (state.isTransferInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun CalendarHorizontalPager(
    initialMonth: YearMonth,
    selectedMonth: YearMonth,
    entries: Map<String, HeadacheEntry>,
    onMonthChanged: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = monthIndexFor(initialMonth, selectedMonth),
        pageCount = { PAGER_PAGE_COUNT },
    )

    LaunchedEffect(selectedMonth) {
        val targetPage = monthIndexFor(initialMonth, selectedMonth)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetMonth = monthAtIndex(initialMonth, pagerState.currentPage)
        if (selectedMonth != targetMonth) {
            onMonthChanged(targetMonth)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DaysOfWeekHeader()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val month = monthAtIndex(initialMonth, page)
            MonthView(
                month = month,
                entries = entries,
                onDayClick = onDayClick,
            )
        }
    }
}

@Composable
private fun CalendarVerticalMonthList(
    initialMonth: YearMonth,
    selectedMonth: YearMonth,
    entries: Map<String, HeadacheEntry>,
    onMonthChanged: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = monthIndexFor(initialMonth, selectedMonth),
    )

    LaunchedEffect(selectedMonth) {
        val targetIndex = monthIndexFor(initialMonth, selectedMonth)
        if (listState.firstVisibleItemIndex != targetIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleMonth = monthAtIndex(initialMonth, listState.firstVisibleItemIndex)
        if (selectedMonth != visibleMonth) {
            onMonthChanged(visibleMonth)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(PAGER_PAGE_COUNT) { index ->
            val month = monthAtIndex(initialMonth, index)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                Text(
                    text = month.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale) +
                        " ${month.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                DaysOfWeekHeader()
                Spacer(modifier = Modifier.height(4.dp))
                MonthView(
                    month = month,
                    entries = entries,
                    onDayClick = onDayClick,
                )
            }

            if (index < PAGER_PAGE_COUNT - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun DataTransferMenu(
    enabled: Boolean,
    autoExportEnabled: Boolean,
    lastAutoExportStatus: String?,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onFullYearClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAutoExportToggled: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.export_data))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.full_year_view)) },
                onClick = {
                    expanded = false
                    onFullYearClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_data)) },
                onClick = {
                    expanded = false
                    onExportClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.import_data)) },
                onClick = {
                    expanded = false
                    onImportClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                onClick = {
                    expanded = false
                    onSettingsClick()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = autoExportEnabled,
                            onCheckedChange = null // Handled by onClick
                        )
                        Text(stringResource(R.string.auto_export))
                    }
                },
                onClick = {
                    onAutoExportToggled(!autoExportEnabled)
                    expanded = false
                }
            )

            val lastExportStr = lastAutoExportStatus ?: stringResource(R.string.never)

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.last_auto_export, lastExportStr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {},
                enabled = false
            )
        }
    }
}

@Composable
fun MonthSelectionDropdown(
    selectedMonth: java.time.Month,
    availableMonths: List<java.time.Month>,
    onMonthSelected: (java.time.Month) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(end = 16.dp)) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedMonth.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select Month")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableMonths.forEach { month ->
                DropdownMenuItem(
                    text = {
                        Text(month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale))
                    },
                    onClick = {
                        onMonthSelected(month)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun YearSelectionDropdown(
    currentYear: Int,
    years: List<Int>,
    onYearSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentYear.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select Year")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun MonthView(
    month: YearMonth,
    entries: Map<String, HeadacheEntry>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
    val today = LocalDate.now()

    val days = buildList {
        repeat(firstDayOfWeek - 1) { add(null) }
        for (i in 1..daysInMonth) {
            add(month.atDay(i))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date != null) {
                            val entry = entries[date.toString()]
                            val isFuture = date > today
                            DayCell(
                                date = date,
                                intensity = entry?.intensity ?: 0,
                                pillsTaken = entry?.pillsTaken ?: 0,
                                onClick = { if (!isFuture) onDayClick(date) },
                                isFuture = isFuture,
                            )
                        } else {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    intensity: Int,
    pillsTaken: Int,
    isFuture: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when (intensity) {
        1 -> Intensity1
        2 -> Intensity2
        3 -> Intensity3
        else -> Intensity0
    }

    val isToday = date == LocalDate.now()

    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isFuture) backgroundColor.copy(alpha = 0.3f) else backgroundColor)
            .then(
                if (!isFuture) Modifier.clickable(onClick = onClick) else Modifier
            )
            .then(
                if (isToday) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.medium
                        )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val asteriskSize = with(density) { (minWidth * 0.22f).toSp() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFuture) Color.White.copy(alpha = 0.5f) else Color.White,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold
            )
            if (pillsTaken > 0) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(pillsTaken) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(width = 1.5.dp, color = PillDotRing, shape = CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    HeadacheTrackerTheme {
        CalendarScreen(
            state = CalendarState(),
            onMonthChanged = {},
            onYearSelected = {},
            onDayClick = {},
            onExportClick = {},
            onImportClick = {},
            onFullYearClick = {},
            onSettingsClick = {},
            onTodayEntryClick = {},
            onAutoExportToggled = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
