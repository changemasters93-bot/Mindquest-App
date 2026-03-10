package com.android.mindquest.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.theme.MindquestColors
import com.android.mindquest.domain.model.DailyChallenge

@Composable
fun DailyChallengeCard(
    challenge: DailyChallenge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .clickable(enabled = !challenge.isDone, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isDone) {
                MindquestColors.SurfaceVariant.copy(alpha = 0.6f)
            } else {
                MindquestColors.Surface
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (challenge.isDone) 1.dp else 4.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            // Header row with icon and done badge
            Row(
                modifier = Modifier.width(192.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\uD83C\uDFAF",
                    fontSize = 28.sp,
                )

                if (challenge.isDone) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MindquestColors.Success.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "\u2714 Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MindquestColors.Success,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = challenge.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (challenge.isDone) {
                    MindquestColors.TextSecondary
                } else {
                    MindquestColors.TextPrimary
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = challenge.description ?: "",
                fontSize = 12.sp,
                color = MindquestColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Meta row: question count and time
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetaTag(
                    emoji = "\u2753",
                    text = "${challenge.questionCount} Qs",
                )
                MetaTag(
                    emoji = "\u23F1",
                    text = "${challenge.timeInMinutes} min",
                )
            }
        }
    }
}

@Composable
private fun MetaTag(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = emoji,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MindquestColors.TextSecondary,
        )
    }
}
