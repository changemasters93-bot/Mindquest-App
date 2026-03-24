package com.android.mindquest.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryColor = Color(0xFF4F46E5)
private val TextDark = Color(0xFF111827)
private val TextMuted = Color(0xFF6B7280)
private val TextLight = Color(0xFF9CA3AF)
private val BorderLight = Color(0xFFE5E7EB)
private val BenefitBg = Color(0xFFF8F9FC)
private val BenefitText = Color(0xFF4B5563)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountLinkingSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onGoogle: () -> Unit,
    onPhone: () -> Unit,
    reason: String = "general",
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val subtitle = when (reason) {
        "tournament" -> "Tournament entry requires a linked account. Your progress will be saved!"
        "leaderboard" -> "Link your account to appear on the leaderboard with your name."
        else -> "Sign in to save your progress across devices and unlock all features."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BorderLight),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Lock icon
            Text(
                text = "\uD83D\uDD10",
                fontSize = 36.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Link Your Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Google Link Button
            OutlinedButton(
                onClick = onGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "G",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Link with Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151),
                    )
                }
            }

            // Phone Link — hidden for now (Google-only launch)
            // TODO: Uncomment when phone auth is enabled

            Spacer(modifier = Modifier.height(16.dp))

            // "What you unlock" section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BenefitBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "WHAT YOU UNLOCK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor,
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                BenefitItem(text = "\uD83C\uDFC6 Enter weekly tournaments")
                Spacer(modifier = Modifier.height(4.dp))
                BenefitItem(text = "\uD83D\uDCCA Appear on leaderboard with your name")
                Spacer(modifier = Modifier.height(4.dp))
                BenefitItem(text = "\uD83D\uDCBE Progress saved to cloud (safe on any device)")
                Spacer(modifier = Modifier.height(4.dp))
                BenefitItem(text = "\uD83D\uDCDC Earn certificates and trophies")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Maybe later
            Text(
                text = "Maybe later",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextLight,
                modifier = Modifier.clickable { onDismiss() },
            )
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = BenefitText,
        lineHeight = 18.sp,
    )
}
