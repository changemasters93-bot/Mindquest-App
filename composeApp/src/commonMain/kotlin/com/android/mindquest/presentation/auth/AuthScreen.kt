package com.android.mindquest.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.util.UiState
import mindquest.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val DarkNavy = Color(0xFF0F172A)
private val DarkPurple = Color(0xFF1E1B4B)
private val DeepIndigo = Color(0xFF312E81)
private val PrimaryColor = Color(0xFF4F46E5)
private val TextDark = Color(0xFF374151)
private val TextMuted = Color(0xFF9CA3AF)
private val BorderLight = Color(0xFFE5E7EB)
private val CardShadow = Color(0x0F000000)

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel,
) {
    val authState by viewModel.authState.collectAsState()
    val authScreen by viewModel.authScreen.collectAsState()
    val isLinkingSheetVisible by viewModel.isLinkingSheetVisible.collectAsState()

    LaunchedEffect(authState) {
        if (authState is UiState.Success) {
            if (authScreen == AuthScreenState.VERIFIED || authScreen == AuthScreenState.MAIN) {
                onAuthSuccess()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = authScreen,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "auth_screen_transition",
        ) { screen ->
            when (screen) {
                AuthScreenState.MAIN -> {
                    AuthMainContent(
                        onGoogle = { viewModel.signInWithGoogle() },
                        onPhone = { viewModel.navigateToPhone() },
                        onAnonymous = { viewModel.signInAnonymously() },
                        isLoading = authState is UiState.Loading,
                    )
                }
                AuthScreenState.PHONE,
                AuthScreenState.OTP,
                AuthScreenState.VERIFIED -> {
                    PhoneOtpScreen(
                        onVerified = onAuthSuccess,
                        onBack = { viewModel.navigateToMain() },
                        viewModel = viewModel,
                    )
                }
                AuthScreenState.LINKING -> {
                    AuthMainContent(
                        onGoogle = { viewModel.signInWithGoogle() },
                        onPhone = { viewModel.navigateToPhone() },
                        onAnonymous = { viewModel.signInAnonymously() },
                        isLoading = authState is UiState.Loading,
                    )
                }
            }
        }

        if (isLinkingSheetVisible) {
            AccountLinkingSheet(
                isVisible = true,
                onDismiss = { viewModel.hideLinkingSheet() },
                onGoogle = { viewModel.linkAccount("google") },
                onPhone = {
                    viewModel.hideLinkingSheet()
                    viewModel.navigateToPhone()
                },
                reason = viewModel.linkReason.value,
            )
        }

    }
}

@Composable
fun AuthMainContent(
    onGoogle: () -> Unit,
    onPhone: () -> Unit,
    onAnonymous: () -> Unit,
    isLoading: Boolean = false,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Dark Gradient Hero Section (rounded bottom) ─────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(DarkNavy, DarkPurple, DeepIndigo),
                    ),
                )
                .padding(top = statusBarPadding + 60.dp, bottom = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative stars
            StarField()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                Text(
                    text = "\uD83D\uDE80",
                    fontSize = 56.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.auth_ready_to_start),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.auth_sign_in_subtitle),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
        }

        // ── Auth Buttons on White Background ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp),
        ) {
            // Google Sign-In Button
            OutlinedButton(
                onClick = onGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = TextDark,
                ),
                enabled = !isLoading,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "G",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.common_continue_with_google),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Phone OTP Button
            OutlinedButton(
                onClick = onPhone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = TextDark,
                ),
                enabled = !isLoading,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "\uD83D\uDCF1",
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.common_continue_with_phone),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "or" divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BorderLight,
                )
                Text(
                    text = stringResource(Res.string.common_or),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BorderLight,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Anonymous Button with gradient
            Button(
                onClick = onAnonymous,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
                enabled = !isLoading,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                listOf(PrimaryColor, Color(0xFF4338CA)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "\uD83D\uDC7B",
                            fontSize = 18.sp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(Res.string.auth_try_anonymously),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note text
            Text(
                text = stringResource(Res.string.auth_anonymous_note),
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Terms text
            Text(
                text = stringResource(Res.string.auth_terms),
                fontSize = 10.sp,
                color = Color(0xFFD1D5DB),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StarField() {
    // Decorative twinkling stars on the hero
    val starPositions = listOf(
        12 to 18, 85 to 25, 30 to 65, 72 to 45,
        50 to 12, 15 to 72, 88 to 68, 42 to 35,
        65 to 15, 22 to 42,
    )
    starPositions.forEachIndexed { index, (xPct, yPct) ->
        val starSize = 2.dp + (index % 3).dp
        Box(
            modifier = Modifier
                .offset(x = (xPct * 3.6).dp, y = (yPct * 1.5).dp)
                .size(starSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f + (index % 4) * 0.15f)),
        )
    }
}
