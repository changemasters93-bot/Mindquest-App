package com.android.mindquest.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryColor = Color(0xFF4F46E5)
private val ShimmerBase = Color(0xFFE2E8F0)
private val ShimmerHighlight = Color(0xFFF1F5F9)

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    message: String = "Loading...",
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = PrimaryColor,
                strokeWidth = 4.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            ShimmerBase,
            ShimmerHighlight,
            ShimmerBase,
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim + 200f, 0f),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBrush),
    )
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(height * 0.5f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
}
