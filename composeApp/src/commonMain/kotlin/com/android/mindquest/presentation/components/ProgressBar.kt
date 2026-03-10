package com.android.mindquest.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TrackColor = Color(0xFFE2E8F0)
private val DefaultProgressColor = Color(0xFF4F46E5)

@Composable
fun MindquestProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = DefaultProgressColor,
    height: Dp = 6.dp,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    var targetProgress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "progress_animation",
    )

    LaunchedEffect(clampedProgress) {
        targetProgress = clampedProgress
    }

    val lighterColor = color.copy(
        red = (color.red + 0.2f).coerceAtMost(1f),
        green = (color.green + 0.2f).coerceAtMost(1f),
        blue = (color.blue + 0.2f).coerceAtMost(1f),
    )

    val gradientBrush = Brush.horizontalGradient(
        listOf(color, lighterColor),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(TrackColor),
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(height / 2))
                    .background(gradientBrush),
            )
        }
    }
}
