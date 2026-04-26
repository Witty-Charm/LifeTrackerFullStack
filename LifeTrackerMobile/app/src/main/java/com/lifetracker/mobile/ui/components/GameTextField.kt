package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
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
        shape = RoundedCornerShape(18.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface.copy(alpha = 0.6f),
                unfocusedContainerColor = colors.surface.copy(alpha = 0.4f),
                disabledContainerColor = colors.surface.copy(alpha = 0.3f),
                errorContainerColor = colors.surface.copy(alpha = 0.4f),
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outline.copy(alpha = 0.20f),
                disabledBorderColor = colors.outline.copy(alpha = 0.10f),
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.onSurfaceVariant,
                cursorColor = colors.primary,
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
            ),
    )
}
