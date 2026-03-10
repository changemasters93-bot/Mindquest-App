package com.android.mindquest.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryColor = Color(0xFF4F46E5)
private val BorderDefault = Color(0xFFE2E8F0)
private val BorderFocused = PrimaryColor
private val BorderError = Color(0xFFEF4444)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF94A3B8)
private val BackgroundColor = Color(0xFFF8FAFC)

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = when {
                isError -> BorderError
                isFocused -> PrimaryColor
                else -> TextPrimary
            },
            modifier = Modifier.padding(bottom = 6.dp),
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = TextSecondary,
                    fontSize = 15.sp,
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) BorderError else BorderFocused,
                unfocusedBorderColor = if (isError) BorderError else BorderDefault,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = BackgroundColor,
                errorBorderColor = BorderError,
                errorContainerColor = Color(0xFFFEF2F2),
                cursorColor = PrimaryColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
        )

        AnimatedVisibility(
            visible = isError && errorText != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = errorText ?: "",
                color = BorderError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}
