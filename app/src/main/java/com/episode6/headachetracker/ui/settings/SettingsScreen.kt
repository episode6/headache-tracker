package com.episode6.headachetracker.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.episode6.headachetracker.R
import com.episode6.headachetracker.ui.theme.HeadacheTrackerTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    snackbarHostState: SnackbarHostState,
    onReminderMinutesChanged: (String) -> Unit,
    onMorningCheckInToggled: (Boolean) -> Unit,
    onMorningCheckInTimeChanged: (hour: Int, minute: Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onAutoExportToggled: (Boolean) -> Unit,
    onLicensesClick: () -> Unit,
    onBack: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection(title = stringResource(R.string.settings_section_reminders)) {
                SettingsToggleRow(
                    icon = Icons.Rounded.WbSunny,
                    title = stringResource(R.string.morning_check_in_setting_label),
                    supporting = stringResource(R.string.morning_check_in_setting_supporting),
                    checked = state.morningCheckInEnabled,
                    onCheckedChange = onMorningCheckInToggled,
                    enabled = state.isLoaded,
                )
                AnimatedVisibility(visible = state.morningCheckInEnabled) {
                    SettingsRow(
                        icon = Icons.Rounded.Schedule,
                        title = stringResource(R.string.morning_check_in_time_label),
                        enabled = state.isLoaded,
                        onClick = { showTimePicker = true },
                        trailing = {
                            Text(
                                text = formatTimeOfDay(state.morningCheckInTimeMinutes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SettingsRowIcon(Icons.Rounded.Medication)
                    OutlinedTextField(
                        value = state.reminderMinutesText,
                        onValueChange = onReminderMinutesChanged,
                        enabled = state.isLoaded,
                        label = { Text(stringResource(R.string.second_pill_reminder_minutes_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                SettingsRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.notification_settings_button),
                    supporting = stringResource(R.string.notification_settings_supporting),
                    onClick = onOpenNotificationSettings,
                    trailing = { TrailingChevron() },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_data)) {
                SettingsRow(
                    icon = Icons.Rounded.Upload,
                    title = stringResource(R.string.export_data),
                    supporting = stringResource(R.string.export_data_supporting),
                    enabled = !state.isTransferInProgress,
                    onClick = onExportClick,
                    trailing = { if (state.isTransferInProgress) TrailingProgress() },
                )
                SettingsRow(
                    icon = Icons.Rounded.Download,
                    title = stringResource(R.string.import_data),
                    supporting = stringResource(R.string.import_data_supporting),
                    enabled = !state.isTransferInProgress,
                    onClick = onImportClick,
                    trailing = { if (state.isTransferInProgress) TrailingProgress() },
                )
                SettingsToggleRow(
                    icon = Icons.Rounded.CloudUpload,
                    title = stringResource(R.string.auto_export),
                    supporting = if (state.autoExportEnabled) {
                        stringResource(
                            R.string.last_auto_export,
                            state.lastAutoExportStatus ?: stringResource(R.string.never),
                        )
                    } else {
                        stringResource(R.string.auto_export_supporting)
                    },
                    checked = state.autoExportEnabled,
                    onCheckedChange = onAutoExportToggled,
                    enabled = !state.isTransferInProgress,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.licenses_menu_item),
                    onClick = onLicensesClick,
                    trailing = { TrailingChevron() },
                )
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.morningCheckInTimeMinutes / 60,
            initialMinute = state.morningCheckInTimeMinutes % 60,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.morning_check_in_time_label)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        onMorningCheckInTimeChanged(timePickerState.hour, timePickerState.minute)
                    },
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled) { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsRowIcon(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    SettingsRow(
        icon = icon,
        title = title,
        supporting = supporting,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun SettingsRowIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TrailingChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TrailingProgress() {
    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
}

private const val DISABLED_ALPHA = 0.38f

private fun formatTimeOfDay(minutesOfDay: Int): String =
    LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HeadacheTrackerTheme {
        SettingsScreen(
            state = SettingsState(
                reminderMinutesText = "60",
                isLoaded = true,
                autoExportEnabled = true,
                lastAutoExportStatus = "7/17/26, 9:41 AM",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onReminderMinutesChanged = {},
            onMorningCheckInToggled = {},
            onMorningCheckInTimeChanged = { _, _ -> },
            onOpenNotificationSettings = {},
            onExportClick = {},
            onImportClick = {},
            onAutoExportToggled = {},
            onLicensesClick = {},
            onBack = {},
        )
    }
}
