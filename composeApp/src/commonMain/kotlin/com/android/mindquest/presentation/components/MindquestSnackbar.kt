package com.android.mindquest.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.util.SnackbarType

private val SuccessGreen = Color(0xFF16A34A)
private val ErrorRed = Color(0xFFDC2626)
private val InfoBlue = Color(0xFF2563EB)

@Composable
fun MindquestSnackbar(
    snackbarData: SnackbarData,
    snackbarType: SnackbarType,
) {
    val (containerColor, emoji) = when (snackbarType) {
        SnackbarType.SUCCESS -> SuccessGreen to "\u2705"
        SnackbarType.ERROR -> ErrorRed to "\u274C"
        SnackbarType.INFO -> InfoBlue to "\u2139\uFE0F"
    }

    Snackbar(
        shape = RoundedCornerShape(12.dp),
        containerColor = containerColor,
        contentColor = Color.White,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = snackbarData.visuals.message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
