package com.example.myplants.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myplants.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.settings_title),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 32.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Changed to AutoMirrored
                            contentDescription = "Go Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Section: Insights & Data
            item { SettingsSectionTitle(title = stringResource(id = R.string.settings_section_insights)) }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Analytics,
                    title = stringResource(id = R.string.settings_item_analytics),
                    onClick = { /* navController.navigate(Route.PLANT_ANALYTICS) */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.History,
                    title = stringResource(id = R.string.settings_item_watering_history),
                    onClick = { /* navController.navigate(Route.DETAILED_HISTORY) */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.DataObject,
                    title = stringResource(id = R.string.settings_item_export_data),
                    onClick = { /* Handle data export */ }
                )
            }

            // Section: My Devices
            item { SettingsSpacer() }
            item { SettingsSectionTitle(title = stringResource(id = R.string.settings_section_devices)) }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Devices,
                    title = stringResource(id = R.string.settings_item_connected_devices),
                    onClick = { /* navController.navigate(Route.CONNECTED_DEVICES) */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Sync,
                    title = stringResource(id = R.string.settings_item_sensor_sync),
                    onClick = { /* navController.navigate(Route.SENSOR_SYNC_SETTINGS) */ }
                )
            }

            // Section: App Preferences
            item { SettingsSpacer() }
            item { SettingsSectionTitle(title = stringResource(id = R.string.settings_section_preferences)) }
            item {
                SettingsItem(
                    icon = Icons.Outlined.NotificationsActive,
                    title = stringResource(id = R.string.settings_item_notification_prefs),
                    onClick = { /* Open notification settings or in-app prefs */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(id = R.string.settings_item_theme),
                    onClick = { /* Show theme selection dialog/screen */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(id = R.string.settings_item_reminder_defaults),
                    onClick = { /* navController.navigate(Route.REMINDER_DEFAULTS) */ }
                )
            }

            // Section: Support
            item { SettingsSpacer() }
            item { SettingsSectionTitle(title = stringResource(id = R.string.settings_section_about)) }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(id = R.string.settings_item_about_app),
                    onClick = { /* navController.navigate(Route.ABOUT_APP) */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.HelpOutline,
                    title = stringResource(id = R.string.settings_item_help_faq),
                    onClick = { /* navController.navigate(Route.HELP_FAQ) */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Feedback,
                    title = stringResource(id = R.string.settings_item_feedback),
                    onClick = { /* Handle send feedback action */ }
                )
            }
            item { SettingsSpacer() } // Extra space at the bottom
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 16.dp) // Extra top padding for section title
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title, // Content description for accessibility
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary // Or MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface, // Standard text color
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = null, // Decorative
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline // Subtle color for the arrow
        )
    }
}

@Composable
fun SettingsSpacer() {
    Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer for tighter section grouping if preferred
    // Or use Divider() for a visual line
}