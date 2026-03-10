package com.android.mindquest.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarRating(
    rating: Int,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    size: Dp = 28.dp,
) {
    val starFontSize = (size.value * 0.75f).sp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxStars) { index ->
            Text(
                text = if (index < rating) "\u2B50" else "\u2606",
                fontSize = starFontSize,
            )
        }
    }
}
