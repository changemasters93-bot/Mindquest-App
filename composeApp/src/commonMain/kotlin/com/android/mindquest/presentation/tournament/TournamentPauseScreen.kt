package com.android.mindquest.presentation.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════
// TOURNAMENT PAUSE — Dark gradient full-screen
// Matches Tournament_Journey_New.jsx TournamentPause
// ═══════════════════════════════════════════════════════════

private val PauseDark1 = Color(0xFF0F172A)
private val PauseDark2 = Color(0xFF1E1B4B)
private val PauseAccent = Color(0xFF4F46E5)
private val PauseAccentDark = Color(0xFF4338CA)
private val PauseQuitRed = Color(0xFFEF4444)

@Composable
fun TournamentPauseScreen(
    viewModel: TournamentViewModel,
    onResume: () -> Unit,
    onQuit: () -> Unit,
) {
    val playState by viewModel.playState.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PauseDark1, PauseDark2),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pause emoji
            Text(
                text = "\u23F8\uFE0F",
                fontSize = 56.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = "Tournament Paused",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Questions answered
            Text(
                text = "${playState.questionsAnswered}/${playState.totalQuestions} questions answered",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info card: timer paused message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Column {
                    Text(
                        text = "\u23F1 Timer is paused",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You can resume anytime before the tournament ends",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time remaining card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Time Remaining",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val minutes = timeLeft / 60
                    val seconds = timeLeft % 60
                    Text(
                        text = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            timeLeft <= 30 -> Color(0xFFEF4444)
                            timeLeft <= 60 -> Color(0xFFF59E0B)
                            else -> Color.White
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Resume button — gradient indigo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PauseAccent, PauseAccentDark),
                        ),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            viewModel.resumeTournament()
                            onResume()
                        },
                    )
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u25B6 Resume Tournament",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quit button — red border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.5.dp,
                        PauseQuitRed.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            viewModel.submitTournament()
                            onQuit()
                        },
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Quit & Submit Current Progress",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PauseQuitRed,
                )
            }
        }
    }
}
