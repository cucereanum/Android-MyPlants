package com.example.myplants.ui.plantList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterRow(
    modifier: Modifier = Modifier,
    filterList: List<String>,
    selectFilter: (String) -> Unit,
    selectedFilterType: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 20.dp,
            )
    ) {

        filterList.forEachIndexed { _, name ->
            FilterRowListItem(name = name, isSelected = selectedFilterType === name,
                onClick = { selectFilter(name) }
            )
        }
    }
}

@Composable
fun FilterRowListItem(
    modifier: Modifier = Modifier,
    name: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable {
            onClick()
        },

        ) {
        Text(
            modifier = Modifier.padding(end = 10.dp),
            text = name,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 16.sp,
        )
        if (isSelected) {
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {}
        }
    }
}