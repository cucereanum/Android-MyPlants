package com.example.myplants.ui.plantList

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplants.ui.util.FilterType

@Composable
fun FilterRow(
    modifier: Modifier = Modifier,
    filterList: List<FilterType>,
    selectFilter: (FilterType) -> Unit,
    selectedFilterType: FilterType
) {
    val density = LocalDensity.current
    // Store each chip's bounds (in parent coordinates)
    val boundsByType = remember { mutableStateMapOf<FilterType, Rect>() }

    val selectedRect = boundsByType[selectedFilterType]
    val indicatorX by animateDpAsState(
        targetValue = with(density) { (selectedRect?.left ?: 0f).toDp() },
        label = "indicatorX"
    )
    val indicatorW by animateDpAsState(
        targetValue = with(density) { (selectedRect?.width ?: 0f).toDp() } - 10.dp,
        label = "indicatorW"
    )

    Box(modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterList.forEach { type ->
                FilterRowListItem(
                    name = type,
                    isSelected = selectedFilterType == type,
                    onClick = { selectFilter(type) },
                    modifier = Modifier
                        // report this item's bounds so the indicator can slide to it
                        .onGloballyPositioned { c -> boundsByType[type] = c.boundsInParent() }
                        .padding(end = 30.dp)
                )
            }
        }

        if (selectedRect != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorX)
                        .width(indicatorW)
                        .align(Alignment.BottomStart)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
fun FilterRowListItem(
    modifier: Modifier = Modifier,
    name: FilterType,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = name.displayName,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 16.sp,
        )
        // keep some space for the moving underline
        Spacer(Modifier.height(8.dp))
    }
}