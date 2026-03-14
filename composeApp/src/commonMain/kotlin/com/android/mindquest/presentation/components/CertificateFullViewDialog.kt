package com.android.mindquest.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.domain.model.TournamentResult

// ── Color tokens per rank ─────────────────────────────────────────────
private val GoldAccent = Color(0xFFF59E0B)
private val SilverAccent = Color(0xFF9CA3AF)
private val BronzeAccent = Color(0xFFCD7F32)
private val IndigoAccent = Color(0xFF4F46E5)

private fun accentForRank(rank: Int): Color = when (rank) {
    1 -> GoldAccent
    2 -> SilverAccent
    3 -> BronzeAccent
    else -> IndigoAccent
}

/**
 * Full-screen modal dialog that renders a beautiful certificate template
 * for a tournament result. Displayed as a bottom-sheet overlay.
 */
@Composable
fun CertificateFullViewDialog(
    result: TournamentResult,
    userName: String,
    onDismiss: () -> Unit,
) {
    val accent = accentForRank(result.rank)
    val isAchievement = result.rank <= 3
    var showDownloadHint by remember { mutableStateOf(false) }

    // Full-screen scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Bottom-sheet card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)),
                    ),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f)),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Certificate card ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(2.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Top decorative accent bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.1f),
                                        accent,
                                        accent.copy(alpha = 0.1f),
                                    ),
                                ),
                            ),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Brand
                    Text(
                        text = "MINDQUEST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 3.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ornamental line
                    OrnamentalDivider(accent = accent)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Certificate type
                    Text(
                        text = if (isAchievement) "Certificate of Achievement"
                        else "Certificate of Participation",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.3).sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OrnamentalDivider(accent = accent)

                    Spacer(modifier = Modifier.height(20.dp))

                    // "Awarded to"
                    Text(
                        text = "Awarded to",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // User name
                    Text(
                        text = userName.ifEmpty { "Student" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text(
                        text = if (isAchievement)
                            "for outstanding performance in"
                        else
                            "for participating in",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tournament title
                    Text(
                        text = result.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Score
                        StatChip(
                            emoji = "\uD83D\uDCCA",
                            label = "Score",
                            value = "${result.score} / ${result.totalQuestions}",
                            accent = accent,
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Rank
                        val rankEmoji = when (result.rank) {
                            1 -> "\uD83C\uDFC6"
                            2 -> "\uD83E\uDD48"
                            3 -> "\uD83E\uDD49"
                            else -> "\uD83C\uDFC5"
                        }
                        StatChip(
                            emoji = rankEmoji,
                            label = "Rank",
                            value = "#${result.rank} of ${result.participantCount}",
                            accent = accent,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Date (if available)
                    if (!result.date.isNullOrBlank()) {
                        Text(
                            text = result.date,
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bottom decorative bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.1f),
                                        accent,
                                        accent.copy(alpha = 0.1f),
                                    ),
                                ),
                            ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer
                    Text(
                        text = "Mindquest\u2122",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD1D5DB),
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Download button
            Box(
                modifier = Modifier
                    .semantics { contentDescription = "Download certificate" }
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent, accent.copy(alpha = 0.85f)),
                        ),
                    )
                    .clickable { showDownloadHint = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (showDownloadHint) "Coming soon!" else "\u2B07\uFE0F  Download Certificate",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dismiss text
            Text(
                text = "Done",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .semantics { contentDescription = "Close certificate view" }
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

// ── Ornamental divider ──────────────────────────────────────────────────

@Composable
private fun OrnamentalDivider(accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = accent.copy(alpha = 0.15f),
            thickness = 1.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.3f)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = accent.copy(alpha = 0.15f),
            thickness = 1.dp,
        )
    }
}

// ── Stat chip (Score / Rank) ────────────────────────────────────────────

@Composable
private fun StatChip(
    emoji: String,
    label: String,
    value: String,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9CA3AF),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
        )
    }
}
