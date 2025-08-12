package com.example.myplants.ui.addEditPlant

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    multiline: Boolean = false
) {
    Column(modifier = modifier.padding(top = 15.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(if (multiline) 100.dp else 60.dp)
                .clip(
                    RoundedCornerShape(14.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(14.dp)
                ),
            maxLines = if (multiline) 3 else 1,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                unfocusedContainerColor = MaterialTheme.colorScheme.onBackground,
                focusedTextColor = MaterialTheme.colorScheme.secondary,
                unfocusedTextColor = MaterialTheme.colorScheme.secondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            value = value, onValueChange = { it ->
                onValueChange(it)
            })
    }
}