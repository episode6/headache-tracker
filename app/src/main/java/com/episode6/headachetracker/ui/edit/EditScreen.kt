package com.episode6.headachetracker.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.episode6.headachetracker.R
import com.episode6.headachetracker.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    state: EditState,
    onIntensityChanged: (Int) -> Unit,
    onPillsTakenChanged: (Int) -> Unit,
    onFirstPillTimeChanged: (Long) -> Unit,
    onSecondPillTimeChanged: (Long) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val date = LocalDate.parse(state.date)
    val formattedDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Log Headache", style = MaterialTheme.typography.titleLarge) }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ButtonDefaults.MinHeight + 16.dp),
                    enabled = !state.isSaving,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ButtonDefaults.MinHeight + 16.dp),
                    enabled = !state.isSaving,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Intensity Level",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "How severe is your headache?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IntensitySelector(
                selectedIntensity = state.intensity,
                onIntensityChanged = { intensity ->
                    onIntensityChanged(intensity)
                    // reveal the pills section (and Save) after picking a severity
                    scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                }
            )

            Text(
                text = "How many pills did you take?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            PillsSelector(
                selectedPills = state.pillsTaken,
                firstPillTime = state.firstPillTime,
                secondPillTime = state.secondPillTime,
                onPillsChanged = onPillsTakenChanged,
                onFirstPillTimeChanged = onFirstPillTimeChanged,
                onSecondPillTimeChanged = onSecondPillTimeChanged
            )

            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Anything worth remembering? (optional)") },
                minLines = 3,
                maxLines = 3,
                shape = MaterialTheme.shapes.large
            )
        }
    }
}

@Composable
fun IntensitySelector(
    selectedIntensity: Int,
    onIntensityChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val options = listOf(
            0 to "None (Feeling Great)",
            1 to "Mild (Noticeable but okay)",
            2 to "Moderate (Interferes with activities)",
            3 to "Severe (Needs rest immediately)"
        )

        options.forEach { (intensity, label) ->
            val isSelected = selectedIntensity == intensity
            val color = when (intensity) {
                1 -> Intensity1
                2 -> Intensity2
                3 -> Intensity3
                else -> Intensity0
            }

            Surface(
                onClick = { onIntensityChanged(intensity) },
                shape = MaterialTheme.shapes.large,
                color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, color)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    // Focusable even in touch mode so the pane's initial focus
                    // lands on a row instead of the notes field (which would
                    // scroll to the bottom and pop the keyboard)
                    .focusProperties { canFocus = true }
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null, // Handled by surface
                        colors = RadioButtonDefaults.colors(selectedColor = color)
                    )
                    
                    Column {
                        Text(
                            text = "Level $intensity",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private val PillRowMinHeight = 64.dp

@Composable
fun PillsSelector(
    selectedPills: Int,
    firstPillTime: Long?,
    secondPillTime: Long?,
    onPillsChanged: (Int) -> Unit,
    onFirstPillTimeChanged: (Long) -> Unit,
    onSecondPillTimeChanged: (Long) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val options = listOf(
            0 to "None",
            1 to "1 pill",
            2 to "2 pills"
        )

        options.forEach { (count, label) ->
            val isSelected = selectedPills == count
            val pillTime = when (count) {
                1 -> firstPillTime
                2 -> secondPillTime
                else -> null
            }
            val onPillTimeChanged = when (count) {
                1 -> onFirstPillTimeChanged
                else -> onSecondPillTimeChanged
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PillRowMinHeight)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onPillsChanged(count) },
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.primary
                        )
                    } else {
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Touch-mode focusable, same as the intensity rows
                        .focusProperties { canFocus = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        PillIcons(
                            count = count,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (count > 0) {
                    // Keep showing the last known time while the chip animates out
                    var displayTime by remember { mutableStateOf(pillTime ?: 0L) }
                    if (pillTime != null) displayTime = pillTime

                    AnimatedVisibility(
                        visible = selectedPills >= count && pillTime != null,
                        enter = expandHorizontally(expandFrom = Alignment.Start) +
                            slideInHorizontally(initialOffsetX = { it }) +
                            fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) +
                            shrinkHorizontally(shrinkTowards = Alignment.Start) +
                            fadeOut(),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        PillTimeChip(
                            label = if (count == 1) "First pill" else "Second pill",
                            time = displayTime,
                            onTimeChanged = onPillTimeChanged,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillTimeChip(
    label: String,
    time: Long,
    onTimeChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPicker = true },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Taken at",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatPillTime(time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showPicker) {
        PillTimePickerDialog(
            title = "$label time",
            initialTime = time,
            onConfirm = { newTime ->
                onTimeChanged(newTime)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillTimePickerDialog(
    title: String,
    initialTime: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = remember(initialTime) {
        Instant.ofEpochMilli(initialTime).atZone(ZoneId.systemDefault())
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        initial.withHour(timePickerState.hour)
                            .withMinute(timePickerState.minute)
                            .withSecond(0)
                            .withNano(0)
                            .toInstant()
                            .toEpochMilli()
                    )
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatPillTime(time: Long): String =
    Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Composable
private fun PillIcons(count: Int, tint: Color, modifier: Modifier = Modifier) {
    when (count) {
        0 -> {}
        1 -> Icon(
            painter = painterResource(R.drawable.ic_pill),
            contentDescription = null,
            tint = tint,
            modifier = modifier.size(24.dp)
        )
        2 -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pill),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Icon(
                painter = painterResource(R.drawable.ic_pill),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditScreenPreview() {
    HeadacheTrackerTheme {
        EditScreen(
            state = EditState(
                date = "2023-10-27",
                intensity = 2,
                pillsTaken = 1,
                firstPillTime = 1698417000000L
            ),
            onIntensityChanged = {},
            onPillsTakenChanged = {},
            onFirstPillTimeChanged = {},
            onSecondPillTimeChanged = {},
            onNotesChanged = {},
            onSave = {},
            onBack = {}
        )
    }
}
