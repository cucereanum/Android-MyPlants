package com.example.myplants.presentation.settings.language

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.example.myplants.R

private data class LanguageOption(
    val id: String,
    val languageTags: String?,
    val labelResId: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLanguageScreen(navController: NavController) {
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

    var selectedLanguageOptionId by remember { mutableStateOf(getSelectedLanguageOptionId()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_language_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.add_edit_plant_go_back_desc),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.settings_language_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.padding(top = 12.dp))

            languageOptions.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguageOptionId = option.id }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedLanguageOptionId == option.id,
                        onClick = { selectedLanguageOptionId = option.id },
                    )
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text(text = stringResource(id = option.labelResId))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val selectedOption =
                        languageOptions.firstOrNull { it.id == selectedLanguageOptionId }
                    restartAppWithNewLanguage(
                        activity = activity,
                        languageTags = selectedOption?.languageTags
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text(text = stringResource(id = R.string.settings_language_apply_restart))
            }
        }
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
                Intent.FLAG_ACTIVITY_CLEAR_TASK,
    )

    activity.startActivity(launchIntent)
    activity.finishAffinity()
}

private fun Context.findActivity(): Activity? {
    var currentContext: Context = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }

    return null
}
