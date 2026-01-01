package com.example.myplants.presentation.addEditPlant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myplants.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTimeDialog(
    modifier: Modifier = Modifier,
    updateTime: (Int, Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val currentTime = Calendar.getInstance()

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )
    Dialog(
        onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        content = {
            Surface(
                modifier = Modifier
                    .width(360.dp) // Adjust width here
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(bottom = 20.dp)
                        .padding(vertical = 20.dp, horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(
                        state = timePickerState,
                    )
                    Button(onClick = onDismissRequest) {
                        Text(
                            stringResource(id = R.string.select_time_dialog_dismiss),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Button(onClick = {
                        updateTime(timePickerState.hour, timePickerState.minute)
                    }) {
                        Text(
                            stringResource(id = R.string.select_time_dialog_confirm),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        },
    )
}