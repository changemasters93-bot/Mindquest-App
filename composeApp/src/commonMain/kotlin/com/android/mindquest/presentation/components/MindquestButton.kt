package com.android.mindquest.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryColor = Color(0xFF4F46E5)
private val PrimaryDark = Color(0xFF4338CA)
private val DisabledGray = Color(0xFFBDBDBD)
private val DisabledGrayDark = Color(0xFF9E9E9E)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val gradientBrush = if (enabled && !isLoading) {
        Brush.horizontalGradient(listOf(PrimaryColor, PrimaryDark))
    } else {
        Brush.horizontalGradient(listOf(DisabledGray, DisabledGrayDark))
    }

    Button(
        onClick = {
            if (!isLoading) onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = if (enabled && !isLoading) 8.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = PrimaryColor.copy(alpha = 0.3f),
                spotColor = PrimaryColor.copy(alpha = 0.3f),
            ),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    brush = gradientBrush,
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, PrimaryColor.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = PrimaryColor,
        ),
    ) {
        Text(
            text = text,
            color = PrimaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}
