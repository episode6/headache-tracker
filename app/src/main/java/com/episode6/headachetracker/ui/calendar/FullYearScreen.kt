package com.episode6.headachetracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.episode6.headachetracker.R
import com.episode6.headachetracker.model.HeadacheEntry
import com.episode6.headachetracker.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

private const val DAYS_PER_MONTH_MAX = 31
private const val MONTHS_PER_YEAR = 12
private const val YEAR_PAGES_IN_PAST = 50
private const val YEAR_CENTER_PAGE = YEAR_PAGES_IN_PAST
private const val YEAR_PAGE_COUNT = YEAR_PAGES_IN_PAST + 1
private val CompactWidthBreakpoint = 600.dp
private val MonthLabelWidth = 36.dp
private val CellSpacing = 4.dp
private val MinCellSize = 24.dp

private fun yearIndexFor(initialYear: Int, year: Int): Int {
    return YEAR_CENTER_PAGE + (year - initialYear)
}

private fun yearAtIndex(initialYear: Int, index: Int): Int {
    return initialYear + (index - YEAR_CENTER_PAGE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullYearScreen(
    state: FullYearState,
    onYearChanged: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val initialYear = remember { YearMonth.now().year }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onYearChanged(LocalDate.now().year) }) {
                            Icon(Icons.Rounded.Today, contentDescription = "Go to Today")
                        }
                        Text(
                            text = state.selectedYear.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.full_year_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val isNarrowPortrait = maxHeight > maxWidth && maxWidth < CompactWidthBreakpoint
            val fitToWidth = !isNarrowPortrait

            FullYearVerticalYearList(
                initialYear = initialYear,
                selectedYear = state.selectedYear,
                entries = state.entries,
                onYearChanged = onYearChanged,
                fitToWidth = fitToWidth,
                containerWidth = maxWidth,
            )
        }
    }
}

@Composable
private fun FullYearVerticalYearList(
    initialYear: Int,
    selectedYear: Int,
    entries: Map<String, HeadacheEntry>,
    onYearChanged: (Int) -> Unit,
    fitToWidth: Boolean,
    containerWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = yearIndexFor(initialYear, selectedYear),
    )

    LaunchedEffect(selectedYear) {
        val targetIndex = yearIndexFor(initialYear, selectedYear)
        if (listState.firstVisibleItemIndex != targetIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleYear = yearAtIndex(initialYear, listState.firstVisibleItemIndex)
        if (selectedYear != visibleYear) {
            onYearChanged(visibleYear)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        items(YEAR_PAGE_COUNT) { index ->
            val year = yearAtIndex(initialYear, index)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FullYearYearBlock(
                    year = year,
                    entries = entries,
                    fitToWidth = fitToWidth,
                    containerWidth = containerWidth,
                )
            }

            if (index < YEAR_PAGE_COUNT - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FullYearYearBlock(
    year: Int,
    entries: Map<String, HeadacheEntry>,
    fitToWidth: Boolean,
    containerWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()
    val contentWidth = containerWidth - 16.dp
    val availableWidth = contentWidth - MonthLabelWidth
    val fitCellSize = (availableWidth - CellSpacing * (DAYS_PER_MONTH_MAX - 1)) / DAYS_PER_MONTH_MAX
    val cellSize = if (fitToWidth) fitCellSize else MinCellSize

    Column(modifier = modifier.fillMaxWidth()) {
        FullYearHeaderRow(
            cellSize = cellSize,
            fitToWidth = fitToWidth,
            horizontalScrollState = horizontalScrollState,
        )

        Row {
            FullYearMonthColumn(cellSize = cellSize)

            FullYearDataGrid(
                year = year,
                entries = entries,
                cellSize = cellSize,
                fitToWidth = fitToWidth,
                horizontalScrollState = horizontalScrollState,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FullYearHeaderRow(
    cellSize: Dp,
    fitToWidth: Boolean,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(MonthLabelWidth))

        val scrollModifier = if (fitToWidth) {
            Modifier.weight(1f)
        } else {
            Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        }

        Row(
            modifier = scrollModifier,
            horizontalArrangement = Arrangement.spacedBy(CellSpacing),
        ) {
            for (day in 1..DAYS_PER_MONTH_MAX) {
                Text(
                    text = day.toString(),
                    modifier = Modifier
                        .size(cellSize)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun FullYearMonthColumn(cellSize: Dp) {
    val locale = LocalLocale.current.platformLocale
    Column(
        modifier = Modifier.width(MonthLabelWidth),
        verticalArrangement = Arrangement.spacedBy(CellSpacing),
    ) {
        for (month in 1..MONTHS_PER_YEAR) {
            val monthName = java.time.Month.of(month)
                .getDisplayName(TextStyle.SHORT, locale)
                .take(2)

            Text(
                text = monthName,
                modifier = Modifier
                    .width(MonthLabelWidth)
                    .height(cellSize)
                    .wrapContentHeight(Alignment.CenterVertically),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun FullYearDataGrid(
    year: Int,
    entries: Map<String, HeadacheEntry>,
    cellSize: Dp,
    fitToWidth: Boolean,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
) {
    val scrollModifier = if (fitToWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.horizontalScroll(horizontalScrollState)
    }

    Column(
        modifier = modifier.then(scrollModifier),
        verticalArrangement = Arrangement.spacedBy(CellSpacing),
    ) {
        for (month in 1..MONTHS_PER_YEAR) {
            val daysInMonth = YearMonth.of(year, month).lengthOfMonth()

            Row(
                horizontalArrangement = Arrangement.spacedBy(CellSpacing),
                modifier = if (fitToWidth) Modifier.fillMaxWidth() else Modifier,
            ) {
                for (day in 1..DAYS_PER_MONTH_MAX) {
                    if (day <= daysInMonth) {
                        val date = LocalDate.of(year, month, day)
                        val entry = entries[date.toString()]
                        CompactDayCell(
                            date = date,
                            intensity = entry?.intensity ?: 0,
                            pillsTaken = entry?.pillsTaken ?: 0,
                            modifier = Modifier.size(cellSize),
                        )
                    } else {
                        Spacer(modifier = Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}

@Composable
fun CompactDayCell(
    date: LocalDate,
    intensity: Int,
    pillsTaken: Int,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (intensity) {
        1 -> Intensity1
        2 -> Intensity2
        3 -> Intensity3
        else -> Intensity0
    }

    val today = LocalDate.now()
    val isToday = date == today
    val isFuture = date > today

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (isFuture) backgroundColor.copy(alpha = 0.3f) else backgroundColor)
            .then(
                if (isToday) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.small,
                        )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (pillsTaken > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(pillsTaken) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isFuture) Color.White.copy(alpha = 0.5f) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isFuture) PillDotRing.copy(alpha = 0.5f) else PillDotRing,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FullYearScreenPreview() {
    HeadacheTrackerTheme {
        FullYearScreen(
            state = FullYearState(selectedYear = LocalDate.now().year),
            onYearChanged = {},
            onBack = {},
        )
    }
}
