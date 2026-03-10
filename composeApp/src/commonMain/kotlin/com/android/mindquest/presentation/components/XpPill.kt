package com.android.mindquest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GoldLight = Color(0xFFFEF3C7)
private val GoldBorder = Color(0xFFFDE68A)
private val GoldDark = Color(0xFFF59E0B)
private val AmberText = Color(0xFF92400E)
private val AmberSecondary = Color(0xFFB45309)

@Composable
fun XpPill(
    xp: Int,
    level: Int,
    maxXpForLevel: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = GoldBorder,
                shape = RoundedCornerShape(20.dp),
            )
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFFFBEB),
                        Color(0xFFFEF3C7),
                    ),
                ),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "\u26A1",
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$xp XP",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AmberText,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "\u00B7",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AmberSecondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Lvl $level \u00B7 $xp/$maxXpForLevel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AmberSecondary,
            )
        }
    }
}
