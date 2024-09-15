package com.example.myplants.ui.addEditPlant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myplants.data.PlantSizeType

@Composable
fun PlantSizeDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    togglePlantSizeSelection: (String) -> Unit,
    selectedPlant: PlantSizeType,
) {
    val plantSizes = PlantSizeType.entries.toTypedArray()

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
                        .background(Color.White)
                        .padding(bottom = 20.dp)
                        .padding(vertical = 20.dp, horizontal = 20.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 20.dp),
                        text = "Plant Size",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp
                    )
                    plantSizes.forEach { plant ->
                        Row(
                            modifier = Modifier.padding(top = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = if (plant == selectedPlant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary, // Set the border color
                                        shape = CircleShape
                                    )
                                    .background(if (plant == selectedPlant) MaterialTheme.colorScheme.primary else Color.White)
                                    .clickable {
                                        togglePlantSizeSelection(plant.toString())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Check Day",
                                    tint = Color.White,
                                )
                            }
                            Text(
                                modifier = Modifier.padding(start = 16.dp),
                                text = plant.plantSize,
                                color = if (plant != selectedPlant) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(end = 10.dp)
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    width = 1.dp
                                )
                                .clickable {
                                    onDismissRequest()
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally

                        ) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(end = 10.dp)
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .border(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    width = 1.dp
                                )
                                .clickable {
                                    onDismissRequest()
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Got it",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        },
    )
}