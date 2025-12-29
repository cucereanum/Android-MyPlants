package com.example.myplants.presentation.settings

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
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.myplants.R
import com.example.myplants.navigation.Route
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

private data class LanguageOption(
    val id: String,
    val languageTags: String?,
    val labelResId: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var isLanguageDialogVisible by remember { mutableStateOf(false) }
    var isRestartConfirmationDialogVisible by remember { mutableStateOf(false) }
    var pendingLanguageOption by remember { mutableStateOf<LanguageOption?>(null) }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val languageOptions = remember {
        listOf(
            LanguageOption(
                id = "system",
                languageTags = null,
                labelResId = R.string.settings_language_option_system,
            ),
            LanguageOption(
                id = "en",
                languageTags = "en",
                labelResId = R.string.settings_language_option_english,
            ),
            LanguageOption(
                id = "de",
                languageTags = "de",
                labelResId = R.string.settings_language_option_german,
            ),
            LanguageOption(
                id = "ro",
                languageTags = "ro",
                labelResId = R.string.settings_language_option_romanian,
            ),
        )
    }

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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.add_edit_plant_go_back_desc)
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
            // Section: My Devices
            item { SettingsSpacer() }
            item { SettingsSectionTitle(title = stringResource(id = R.string.settings_section_devices)) }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Devices,
                    title = stringResource(id = R.string.settings_item_connected_devices),
                    onClick = { navController.navigate(Route.BLE) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Sync,
                    title = stringResource(id = R.string.settings_item_sensor_sync),
                    onClick = { /* navController.navigate(Route.SENSOR_SYNC_SETTINGS) */ }
                )
            }

            // Section: Preferences
            item { SettingsSpacer() }
            item { SettingsSectionTitle(title = stringResource(id = R.string.settings_section_preferences)) }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Language,
                    title = stringResource(id = R.string.settings_item_language),
                    onClick = { isLanguageDialogVisible = true }
                )
            }
        }
    }

    if (isLanguageDialogVisible) {
        val selectedLanguageOptionId = getSelectedLanguageOptionId()
        AlertDialog(
            onDismissRequest = { isLanguageDialogVisible = false },
            title = { Text(text = stringResource(id = R.string.settings_language_dialog_title)) },
            text = {
                Column {
                    languageOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isLanguageDialogVisible = false
                                    pendingLanguageOption = option
                                    isRestartConfirmationDialogVisible = true
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedLanguageOptionId == option.id,
                                onClick = {
                                    isLanguageDialogVisible = false
                                    pendingLanguageOption = option
                                    isRestartConfirmationDialogVisible = true
                                },
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = stringResource(id = option.labelResId))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isLanguageDialogVisible = false }) {
                    Text(text = stringResource(id = R.string.dialog_ok))
                }
            },
        )
    }

    if (isRestartConfirmationDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                isRestartConfirmationDialogVisible = false
                pendingLanguageOption = null
            },
            title = { Text(text = stringResource(id = R.string.settings_language_restart_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.settings_language_restart_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val languageTags = pendingLanguageOption?.languageTags
                        pendingLanguageOption = null
                        isRestartConfirmationDialogVisible = false
                        restartAppWithNewLanguage(activity = activity, languageTags = languageTags)
                    },
                ) {
                    Text(text = stringResource(id = R.string.settings_language_restart_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isRestartConfirmationDialogVisible = false
                        pendingLanguageOption = null
                    },
                ) {
                    Text(text = stringResource(id = R.string.dialog_cancel))
                }
            },
        )
    }
}

private fun getSelectedLanguageOptionId(): String {
    val currentLocaleList = AppCompatDelegate.getApplicationLocales()
    val currentLanguageTags = currentLocaleList.toLanguageTags()
    val primaryLanguageTag = currentLanguageTags
        .split(',')
        .firstOrNull()
        ?.trim()

    return when {
        primaryLanguageTag.isNullOrBlank() -> "system"
        primaryLanguageTag.startsWith("de") -> "de"
        primaryLanguageTag.startsWith("ro") -> "ro"
        primaryLanguageTag.startsWith("en") -> "en"
        else -> "system"
    }
}

private fun restartAppWithNewLanguage(activity: Activity?, languageTags: String?) {
    if (activity == null) return

    val newLocales = if (languageTags.isNullOrBlank()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageTags)
    }

    AppCompatDelegate.setApplicationLocales(newLocales)
    val launchIntent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        ?: return

    launchIntent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
    )

    activity.startActivity(launchIntent)
    activity.finishAffinity()
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
            .padding(top = 16.dp)
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
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SettingsSpacer() {
    Spacer(modifier = Modifier.height(8.dp))
}

private fun Context.findActivity(): Activity? {
    var currentContext: Context = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }

    return null
}