package com.example.myplants.ui.addEditPlant


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DatesDialog(
    modifier: Modifier = Modifier,
    selectedDays: SnapshotStateList<DayOfWeek>,
    onDismissRequest: () -> Unit,
    toggleDaySelection: (DayOfWeek) -> Unit,
) {
    val allDays = DayOfWeek.entries.toTypedArray()

    AlertDialog(
        onDismissRequest,
        title = { Text("Select Days of the Week") },
        text = {
            Column {
                allDays.forEach { day ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedDays.contains(day),
                            onCheckedChange = {
                                toggleDaySelection(day)
                            }
                        )
                        Text(text = day.dayName)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("OK")
            }
        }
    )
}