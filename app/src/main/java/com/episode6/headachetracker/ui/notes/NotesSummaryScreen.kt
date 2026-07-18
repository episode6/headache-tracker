package com.episode6.headachetracker.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.episode6.headachetracker.R
import com.episode6.headachetracker.model.HeadacheEntry
import com.episode6.headachetracker.ui.calendar.CompactDayCell
import com.episode6.headachetracker.ui.theme.HeadacheTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSummaryScreen(
    state: NotesSummaryState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notes_summary_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.notes_summary_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.entries.isEmpty()) {
            if (!state.isLoading) {
                NotesSummaryEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                itemsIndexed(state.entries, key = { _, entry -> entry.date }) { index, entry ->
                    NotesSummaryRow(entry = entry)
                    if (index < state.entries.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesSummaryRow(
    entry: HeadacheEntry,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEE, MMM d, uuuu", locale)
    }
    val date = remember(entry.date) { LocalDate.parse(entry.date) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CompactDayCell(
            date = date,
            intensity = entry.intensity,
            pillsTaken = entry.pillsTaken,
            modifier = Modifier.size(40.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = entry.notes.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun NotesSummaryEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.notes_summary_empty),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.notes_summary_empty_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesSummaryScreenPreview() {
    HeadacheTrackerTheme {
        NotesSummaryScreen(
            state = NotesSummaryState(
                entries = listOf(
                    HeadacheEntry(
                        date = "2026-07-16",
                        intensity = 2,
                        pillsTaken = 1,
                        notes = "Woke up with a headache, took a pill after breakfast.",
                    ),
                    HeadacheEntry(
                        date = "2026-07-10",
                        intensity = 3,
                        pillsTaken = 2,
                        notes = "Bad one. Second pill in the afternoon helped a little.",
                    ),
                ),
                isLoading = false,
            ),
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesSummaryScreenEmptyPreview() {
    HeadacheTrackerTheme {
        NotesSummaryScreen(
            state = NotesSummaryState(isLoading = false),
            onBack = {},
        )
    }
}
