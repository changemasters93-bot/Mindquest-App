package com.android.mindquest.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mindquest.core.util.UiState

private val PrimaryColor = Color(0xFF4F46E5)
private val DarkIndigo = Color(0xFF4338CA)
private val TextDark = Color(0xFF111827)
private val TextMuted = Color(0xFF9CA3AF)
private val TextSubtle = Color(0xFF6B7280)
private val BorderLight = Color(0xFFE5E7EB)
private val FocusBg = Color(0xFFF8F7FF)
private val OtpFilledBg = Color(0xFFEEF2FF)
private val SuccessGreen = Color(0xFF22C55E)
private val SuccessGreenDark = Color(0xFF16A34A)

@Composable
fun PhoneOtpScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel,
) {
    val authState by viewModel.authState.collectAsState()
    val authScreen by viewModel.authScreen.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val resendTimer by viewModel.resendTimer.collectAsState()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Auto-navigate after verified
    LaunchedEffect(authScreen) {
        if (authScreen == AuthScreenState.VERIFIED) {
            kotlinx.coroutines.delay(2000)
            onVerified()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding)
                .verticalScroll(rememberScrollState()),
        ) {
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
                label = "phone_otp_transition",
            ) { screen ->
                when (screen) {
                    AuthScreenState.PHONE -> {
                        PhoneEntryContent(
                            phoneNumber = phoneNumber,
                            onPhoneChange = { viewModel.updatePhoneNumber(it) },
                            onSendOtp = { viewModel.sendOtp(phoneNumber) },
                            onBack = onBack,
                            isLoading = authState is UiState.Loading,
                        )
                    }
                    AuthScreenState.OTP -> {
                        OtpVerifyContent(
                            otpCode = otpCode,
                            onOtpChange = { viewModel.updateOtpCode(it) },
                            onVerify = { viewModel.verifyOtp(otpCode) },
                            onResend = { viewModel.resendOtp() },
                            onBack = { viewModel.navigateToPhone() },
                            resendTimer = resendTimer,
                            phoneNumber = phoneNumber,
                            isLoading = authState is UiState.Loading,
                        )
                    }
                    AuthScreenState.VERIFIED -> {
                        VerifiedContent()
                    }
                    else -> { /* No-op */ }
                }
            }
        }
    }
}

@Composable
private fun PhoneEntryContent(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
) {
    val isValid = phoneNumber.length == 10

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
    ) {
        // Back button
        Text(
            text = "\u2190 Back",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSubtle,
            modifier = Modifier
                .clickable { onBack() }
                .padding(bottom = 24.dp),
        )

        // Title
        Text(
            text = "Enter Phone Number",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
            letterSpacing = (-0.3).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "We'll send a 6-digit verification code",
            fontSize = 13.sp,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Label
        Text(
            text = "PHONE NUMBER",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSubtle,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        // Phone input with +91 prefix
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isValid) FocusBg else Color.White)
                .border(
                    width = 2.dp,
                    color = if (isValid) PrimaryColor else BorderLight,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83C\uDDEE\uD83C\uDDF3 +91",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(BorderLight),
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = phoneNumber,
                onValueChange = { value ->
                    if (value.length <= 10 && value.all { it.isDigit() }) {
                        onPhoneChange(value)
                    }
                },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = TextDark,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (phoneNumber.isEmpty()) {
                            Text(
                                text = "98765 43210",
                                color = TextMuted,
                                fontSize = 16.sp,
                                letterSpacing = 1.5.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (isValid) {
                Text(
                    text = "\u2713",
                    fontSize = 16.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Send OTP button with gradient
        GradientCTAButton(
            text = "Send OTP \u2192",
            onClick = onSendOtp,
            enabled = isValid && !isLoading,
        )
    }
}

@Composable
private fun OtpVerifyContent(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    resendTimer: Int,
    phoneNumber: String,
    isLoading: Boolean,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-verify when 6 digits entered
    LaunchedEffect(otpCode) {
        if (otpCode.length == 6) {
            onVerify()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
    ) {
        // Back button
        Text(
            text = "\u2190 Back",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSubtle,
            modifier = Modifier
                .clickable { onBack() }
                .padding(bottom = 24.dp),
        )

        // Title
        Text(
            text = "Verify OTP",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
            letterSpacing = (-0.3).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sent to +91 $phoneNumber",
            fontSize = 13.sp,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // OTP Input - 6 digit boxes
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = otpCode,
                onValueChange = { value ->
                    if (value.length <= 6 && value.all { it.isDigit() }) {
                        onOtpChange(value)
                    }
                },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        repeat(6) { index ->
                            val char = otpCode.getOrNull(index)
                            val hasFill = char != null

                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (hasFill) OtpFilledBg else Color(0xFFFAFAFA))
                                    .border(
                                        width = 2.dp,
                                        color = if (hasFill) PrimaryColor else BorderLight,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = char?.toString() ?: "",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Resend timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Didn't receive? ",
                fontSize = 13.sp,
                color = TextMuted,
            )
            if (resendTimer > 0) {
                Text(
                    text = "Resend in ${resendTimer}s",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryColor,
                )
            } else {
                Text(
                    text = "Resend",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryColor,
                    modifier = Modifier.clickable { onResend() },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Verify button with gradient
        GradientCTAButton(
            text = "Verify \u2192",
            onClick = onVerify,
            enabled = otpCode.length == 6 && !isLoading,
        )
    }
}

@Composable
private fun VerifiedContent() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "verified_scale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            ) + fadeIn(),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(SuccessGreen, SuccessGreenDark),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2713",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Verified!",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Redirecting...",
            fontSize = 13.sp,
            color = TextMuted,
        )
    }
}

@Composable
private fun GradientCTAButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = if (enabled) {
                        Brush.linearGradient(
                            listOf(PrimaryColor, DarkIndigo),
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)),
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else TextMuted,
            )
        }
    }
}
