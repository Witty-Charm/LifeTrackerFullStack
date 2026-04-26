package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor =
        when {
            isError -> colors.error
            isFocused -> colors.primary
            else -> colors.outline.copy(alpha = 0.20f)
        }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape),
        interactionSource = interactionSource,
        label = label,
        singleLine = singleLine,
        maxLines = maxLines,
        readOnly = readOnly,
        isError = isError,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surface.copy(alpha = 0.6f),
            unfocusedContainerColor = colors.surface.copy(alpha = 0.4f),
            disabledContainerColor = colors.surface.copy(alpha = 0.3f),
            errorContainerColor = colors.surface.copy(alpha = 0.4f),

            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,

            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.onSurfaceVariant,

            cursorColor = colors.primary,

            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            disabledTextColor = colors.onSurface.copy(alpha = 0.38f),
            errorTextColor = colors.onSurface,
        ),
    )
}