package com.example.myplants.ui.addEditPlant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One field that can work as:
 *  - Editable text input, OR
 *  - Read-only selector (dropdown-like) that triggers onClick
 *
 * You control which mode via [readOnly] and [onClick]:
 *  - If readOnly = true and onClick != null → behaves like a selector
 *  - Otherwise → normal editable text field
 */
@Composable
fun AppFormField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,          // when provided with readOnly=true → behaves like dropdown
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable (() -> Unit))? = null,
    leadingIcon: (@Composable (() -> Unit))? = null,
    supportingText: String? = null,         // error/help text
    isError: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }

    // If it's a selector, we listen for taps on the field
    if (readOnly && onClick != null) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { inter ->
                if (inter is PressInteraction.Release) onClick()
            }
        }
    }

    Column(modifier = modifier.padding(top = 15.dp)) {
        if (label != null) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(10.dp))
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = maxLines,
            readOnly = readOnly,
            interactionSource = interactionSource,
            leadingIcon = leadingIcon,
            trailingIcon = {
                when {
                    trailingIcon != null -> trailingIcon()
                    readOnly && onClick != null -> Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            },
            placeholder = {
                if (placeholder != null) Text(placeholder)
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.onSecondary, shape)
                // Optional extra click surface for selectors (makes the whole field tappable)
                .then(
                    if (readOnly && onClick != null) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = interactionSource
                        ) { onClick() }
                    } else Modifier
                ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                unfocusedContainerColor = MaterialTheme.colorScheme.onBackground,
                disabledContainerColor = MaterialTheme.colorScheme.onBackground,
                errorContainerColor = MaterialTheme.colorScheme.onBackground,
                focusedTextColor = MaterialTheme.colorScheme.secondary,
                unfocusedTextColor = MaterialTheme.colorScheme.secondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            isError = isError
        )

        if (supportingText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = supportingText,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}