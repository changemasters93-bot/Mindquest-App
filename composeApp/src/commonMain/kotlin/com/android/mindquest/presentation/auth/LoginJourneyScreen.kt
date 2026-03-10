package com.android.mindquest.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.android.mindquest.core.util.UiState
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// COLORS
// ═══════════════════════════════════════════════════════════════════
private val DarkNavy = Color(0xFF0F172A)
private val DarkPurple = Color(0xFF1E1B4B)
private val DeepIndigo = Color(0xFF312E81)
private val PrimaryColor = Color(0xFF4F46E5)
private val IndigoLight = Color(0xFF6366F1)
private val IndigoDark = Color(0xFF4338CA)
private val TextDark = Color(0xFF111827)
private val TextMuted = Color(0xFF9CA3AF)
private val TextSubtle = Color(0xFF6B7280)
private val TextLabel = Color(0xFF374151)
private val BorderLight = Color(0xFFE5E7EB)
private val SuccessGreen = Color(0xFF22C55E)
private val SuccessGreenDark = Color(0xFF16A34A)
private val XpGold = Color(0xFFFBBF24)
private val OtpFilledBg = Color(0xFFEEF2FF)
private val FocusBg = Color(0xFFF8F7FF)

// ═══════════════════════════════════════════════════════════════════
// DATA
// ═══════════════════════════════════════════════════════════════════
private data class AvatarData(
    val id: Int,
    val name: String,
    val emoji: String,
    val bgFrom: Color,
    val bgTo: Color,
)

private val AVATARS = listOf(
    AvatarData(1, "Sunny", "\uD83C\uDF1E", Color(0xFFFF6B9D), Color(0xFFFF8DC7)),
    AvatarData(2, "Blaze", "\uD83D\uDD25", Color(0xFF4FACFE), Color(0xFF2B86E8)),
    AvatarData(3, "Coco", "\uD83D\uDC35", Color(0xFFA78BFA), Color(0xFF7C3AED)),
    AvatarData(4, "Rio", "\uD83E\uDD81", Color(0xFF34D399), Color(0xFF059669)),
    AvatarData(5, "Luna", "\uD83C\uDF19", Color(0xFFF59E0B), Color(0xFFD97706)),
    AvatarData(6, "Zack", "\u26A1", Color(0xFFF87171), Color(0xFFDC2626)),
    AvatarData(7, "Nova", "\u2B50", Color(0xFF8B5CF6), Color(0xFF6D28D9)),
    AvatarData(8, "Miko", "\uD83E\uDD13", Color(0xFF06B6D4), Color(0xFF0891B2)),
)

private val GRADES = listOf("Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7", "Grade 8")
private val COUNTRIES = listOf("India", "United States", "United Kingdom", "Canada", "Australia", "Singapore", "UAE")
private val CITIES_MAP = mapOf(
    "India" to listOf("Delhi", "Mumbai", "Bangalore", "Chennai", "Hyderabad", "Pune", "Kolkata"),
    "United States" to listOf("New York", "Los Angeles", "Chicago", "Houston", "Phoenix"),
    "United Kingdom" to listOf("London", "Manchester", "Birmingham", "Edinburgh"),
    "Canada" to listOf("Toronto", "Vancouver", "Montreal", "Ottawa"),
    "Australia" to listOf("Sydney", "Melbourne", "Brisbane", "Perth"),
    "Singapore" to listOf("Singapore"),
    "UAE" to listOf("Dubai", "Abu Dhabi", "Sharjah"),
)

private data class SlideData(
    val emoji: String,
    val title: String,
    val titleAccent: String,
    val tag: String,
    val bgColors: List<Color>,
    val accent: Color,
)

private val SLIDES = listOf(
    SlideData(
        emoji = "\uD83E\uDDE0",
        title = "Smarter Learning.",
        titleAccent = "Happier Kids.",
        tag = "ADAPTIVE QUIZZES & CHALLENGES",
        bgColors = listOf(DarkNavy, DarkPurple, DeepIndigo),
        accent = Color(0xFF818CF8),
    ),
    SlideData(
        emoji = "\u26A1",
        title = "Build Intelligence",
        titleAccent = "the Right Way",
        tag = "LOGIC \u00B7 MEMORY \u00B7 REASONING",
        bgColors = listOf(Color(0xFF0C1F12), Color(0xFF0F3D1F), Color(0xFF166534)),
        accent = Color(0xFF34D399),
    ),
    SlideData(
        emoji = "\uD83C\uDFC6",
        title = "Weekly Live",
        titleAccent = "Competitions",
        tag = "GLOBAL \u00B7 COUNTRY \u00B7 CITY RANKS",
        bgColors = listOf(Color(0xFF1A0A00), Color(0xFF3D1200), Color(0xFF7C2D00)),
        accent = Color(0xFFFBBF24),
    ),
)

private data class ProfileData(
    val name: String = "",
    val country: String = "India",
    val city: String = "Delhi",
    val avatarId: Int = 4,
    val grade: String = "Grade 5",
    val school: String = "",
)

private enum class JourneyStep {
    SPLASH, ONBOARDING, STEP1, STEP2, STEP3, PHONE_AUTH, DONE
}

// ═══════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════
@Composable
fun LoginJourneyScreen(
    viewModel: AuthViewModel,
    onComplete: () -> Unit,
) {
    var currentStep by remember { mutableStateOf(JourneyStep.SPLASH) }
    var profileData by remember { mutableStateOf(ProfileData()) }

    val authState by viewModel.authState.collectAsState()
    val authScreen by viewModel.authScreen.collectAsState()

    // When phone auth reaches VERIFIED → show verified 2s → DONE
    LaunchedEffect(authScreen) {
        if (authScreen == AuthScreenState.VERIFIED && currentStep == JourneyStep.PHONE_AUTH) {
            delay(2000)
            currentStep = JourneyStep.DONE
        }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { it } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally { -it } + fadeOut(tween(300)))
            } else {
                (slideInHorizontally { -it } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally { it } + fadeOut(tween(300)))
            }
        },
        label = "journey_transition",
    ) { step ->
        when (step) {
            JourneyStep.SPLASH -> SplashScreen(
                onDone = { currentStep = JourneyStep.ONBOARDING },
            )
            JourneyStep.ONBOARDING -> OnboardingScreen(
                onDone = { currentStep = JourneyStep.STEP1 },
            )
            JourneyStep.STEP1 -> ProfileStep1(
                data = profileData,
                onUpdate = { profileData = it },
                onNext = { currentStep = JourneyStep.STEP2 },
            )
            JourneyStep.STEP2 -> ProfileStep2(
                data = profileData,
                onUpdate = { profileData = it },
                onNext = { currentStep = JourneyStep.STEP3 },
                onBack = { currentStep = JourneyStep.STEP1 },
            )
            JourneyStep.STEP3 -> WelcomeStep3(
                data = profileData,
                onBack = { currentStep = JourneyStep.STEP2 },
                onGoogle = { viewModel.signInWithGoogle() },
                onAnonymous = { viewModel.signInAnonymously() },
                onPhone = {
                    viewModel.navigateToPhone()
                    currentStep = JourneyStep.PHONE_AUTH
                },
                authState = authState,
                onDone = { currentStep = JourneyStep.DONE },
            )
            JourneyStep.PHONE_AUTH -> PhoneAuthFlow(
                viewModel = viewModel,
                onBack = {
                    viewModel.navigateToMain()
                    currentStep = JourneyStep.STEP3
                },
            )
            JourneyStep.DONE -> DoneScreen(
                name = profileData.name,
                onStart = onComplete,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SPLASH SCREEN  —  twinkling stars, bouncing logo, loading dots
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun SplashScreen(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(2200)
        onDone()
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "splash_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(650),
        label = "splash_alpha",
    )

    // Floating bob
    val bobTransition = rememberInfiniteTransition(label = "bob")
    val bobY by bobTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob_y",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(DarkNavy, DarkPurple, DeepIndigo))),
        contentAlignment = Alignment.Center,
    ) {
        TwinklingStarField(count = 30)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .offset(y = bobY.dp),
        ) {
            // Logo orb
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(IndigoLight, PrimaryColor))),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "\uD83E\uDDE0", fontSize = 42.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 300)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MindQuest",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Smarter Learning. Happier Kids.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC7D2FE).copy(alpha = 0.6f),
                    )
                }
            }
        }

        // Loading dots
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, delayMillis = 600)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
        ) {
            PulsingDots()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ONBOARDING  —  3 slides, swipeable HorizontalPager, twinkling stars
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { SLIDES.size })
    val scope = rememberCoroutineScope()
    val currentSlide = SLIDES[pagerState.currentPage]
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Floating bob for emoji
    val bobTransition = rememberInfiniteTransition(label = "onboard_bob")
    val bobY by bobTransition.animateFloat(
        initialValue = 0f, targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "onboard_bob_y",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(currentSlide.bgColors)),
    ) {
        TwinklingStarField(count = 20)

        Column(modifier = Modifier.fillMaxSize()) {
            // Skip button
            if (pagerState.currentPage < SLIDES.size - 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarPadding + 14.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .clickable { onDone() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Skip",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(statusBarPadding + 44.dp))
            }

            // ── Swipeable Pager ─────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val slide = SLIDES[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Emoji with glow + floating
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .offset(y = if (page == pagerState.currentPage) bobY.dp else 0.dp)
                            .clip(CircleShape)
                            .background(slide.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = slide.emoji, fontSize = 60.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Brand on first slide
                    if (page == 0) {
                        Row {
                            Text("mind", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp)
                            Text("quest", fontSize = 22.sp, fontWeight = FontWeight.Black, color = slide.accent, letterSpacing = (-0.5).sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Tag pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(slide.accent.copy(alpha = 0.12f))
                            .border(1.dp, slide.accent.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = slide.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = slide.accent,
                            letterSpacing = 1.2.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = slide.title,
                        fontSize = 26.sp, fontWeight = FontWeight.Black,
                        color = Color.White, textAlign = TextAlign.Center,
                        letterSpacing = (-0.8).sp, lineHeight = 32.sp,
                    )
                    Text(
                        text = slide.titleAccent,
                        fontSize = 26.sp, fontWeight = FontWeight.Black,
                        color = slide.accent, textAlign = TextAlign.Center,
                        letterSpacing = (-0.8).sp, lineHeight = 32.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Bottom: dots + button ───────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Animated dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 20.dp),
                ) {
                    SLIDES.forEachIndexed { i, _ ->
                        val isActive = i == pagerState.currentPage
                        val dotWidth by animateFloatAsState(
                            targetValue = if (isActive) 24f else 8f,
                            animationSpec = tween(300),
                            label = "dot_$i",
                        )
                        Box(
                            modifier = Modifier
                                .size(width = dotWidth.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isActive) currentSlide.accent
                                    else Color.White.copy(alpha = 0.2f),
                                ),
                        )
                    }
                }

                // Next / Get Started button
                JourneyGradientButton(
                    text = if (pagerState.currentPage == SLIDES.size - 1) "Let's Get Started! \uD83D\uDE80" else "Next",
                    onClick = {
                        if (pagerState.currentPage < SLIDES.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onDone()
                        }
                    },
                    accentFrom = currentSlide.accent.copy(alpha = 0.85f),
                    accentTo = currentSlide.accent,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// STEP 1 — Name + Country/City + Avatar picker
// ═══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStep1(
    data: ProfileData,
    onUpdate: (ProfileData) -> Unit,
    onNext: () -> Unit,
) {
    val isValid = data.name.trim().length >= 2
    val avatar = AVATARS.find { it.id == data.avatarId } ?: AVATARS[3]
    val cities = CITIES_MAP[data.country] ?: listOf("Capital City", "Main City")

    // Floating bob for avatar preview
    val bobTransition = rememberInfiniteTransition(label = "av_bob")
    val bobY by bobTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "av_bob_y",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        StepProgressBar(step = 1, total = 3)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
        ) {
            Text(
                text = "Tell us about you \uD83D\uDC4B",
                fontSize = 24.sp, fontWeight = FontWeight.Black,
                color = TextDark, letterSpacing = (-0.6).sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Let's set up your profile in seconds", fontSize = 13.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(24.dp))

            JourneyInputField(
                label = "Your Name",
                value = data.name,
                onValueChange = { onUpdate(data.copy(name = it)) },
                placeholder = "e.g. Neha",
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Country selector (styled dialog)
            SelectorField(
                label = "Country",
                value = data.country,
                options = COUNTRIES,
                onSelect = {
                    val newCities = CITIES_MAP[it] ?: listOf("Capital City")
                    onUpdate(data.copy(country = it, city = newCities.first()))
                },
                hint = "\uD83D\uDCCD Detected: ${data.country}",
            )

            Spacer(modifier = Modifier.height(20.dp))

            SelectorField(
                label = "City",
                value = data.city,
                options = cities,
                onSelect = { onUpdate(data.copy(city = it)) },
                hint = "\uD83D\uDCCD Detected: ${data.city}",
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Avatar picker
            Text(
                text = "Choose your avatar",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = TextLabel,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            // Avatar preview with floating animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(avatar.bgFrom.copy(alpha = 0.08f), avatar.bgTo.copy(alpha = 0.05f)),
                        ),
                    )
                    .border(1.5.dp, avatar.bgFrom.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = bobY.dp),
                ) {
                    Text(text = avatar.emoji, fontSize = 52.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(avatar.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Avatar grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AVATARS.forEach { av ->
                    val isSelected = data.avatarId == av.id
                    val avatarScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                        label = "av_scale_${av.id}",
                    )
                    val avatarOffsetY by animateFloatAsState(
                        targetValue = if (isSelected) -4f else 0f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                        label = "av_offset_${av.id}",
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onUpdate(data.copy(avatarId = av.id)) }
                            .padding(horizontal = 4.dp)
                            .scale(avatarScale)
                            .offset(y = avatarOffsetY.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(av.bgFrom, av.bgTo)))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, av.bgFrom, CircleShape)
                                    else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = av.emoji, fontSize = 32.sp)
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            av.name, fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) av.bgFrom else TextMuted,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            JourneyGradientButton(
                text = "Next \u2192",
                onClick = onNext,
                enabled = isValid,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// STEP 2 — Grade + School
// ═══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStep2(
    data: ProfileData,
    onUpdate: (ProfileData) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val isValid = data.grade.isNotEmpty() && data.school.trim().length >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        StepProgressBar(step = 2, total = 3)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
        ) {
            BackButton(onClick = onBack)

            Text(
                "Your school info \uD83C\uDF93",
                fontSize = 24.sp, fontWeight = FontWeight.Black,
                color = TextDark, letterSpacing = (-0.6).sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("This helps us personalise your quizzes", fontSize = 13.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(24.dp))

            Text("Grade", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLabel)
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GRADES.forEach { grade ->
                    val isSel = data.grade == grade
                    val pillScale by animateFloatAsState(
                        targetValue = if (isSel) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f),
                        label = "grade_$grade",
                    )

                    Box(
                        modifier = Modifier
                            .scale(pillScale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSel) Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF)))
                                else Brush.linearGradient(listOf(Color(0xFFF9FAFB), Color(0xFFF9FAFB))),
                            )
                            .border(
                                2.dp,
                                if (isSel) IndigoLight else BorderLight,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onUpdate(data.copy(grade = grade)) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            grade.replace("Grade ", "G"),
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSel) PrimaryColor else TextSubtle,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            JourneyInputField(
                label = "School Name",
                value = data.school,
                onValueChange = { onUpdate(data.copy(school = it)) },
                placeholder = "e.g. Delhi Public School",
            )

            Spacer(modifier = Modifier.height(32.dp))

            JourneyGradientButton(
                text = "Next \u2192",
                onClick = onNext,
                enabled = isValid,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// STEP 3 — Welcome Card  +  Google / Phone / Anonymous auth
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun WelcomeStep3(
    data: ProfileData,
    onBack: () -> Unit,
    onGoogle: () -> Unit,
    onAnonymous: () -> Unit,
    onPhone: () -> Unit,
    authState: UiState<*>,
    onDone: () -> Unit,
) {
    val avatar = AVATARS.find { it.id == data.avatarId } ?: AVATARS[3]
    var isLoading by remember { mutableStateOf(false) }
    var loadPct by remember { mutableStateOf(0) }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Floating bob
    val bobTransition = rememberInfiniteTransition(label = "step3_bob")
    val bobY by bobTransition.animateFloat(
        initialValue = 0f, targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "step3_bob_y",
    )

    // Drive progress bar locally, complete when authState is success
    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (loadPct < 80) {
                delay(120)
                loadPct = (loadPct + (4..12).random()).coerceAtMost(80)
            }
        }
    }
    // When auth succeeds, finish progress → go to done
    LaunchedEffect(authState) {
        if (authState is UiState.Success && isLoading) {
            loadPct = 100
            delay(400)
            onDone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(DarkNavy, DarkPurple, DeepIndigo))),
    ) {
        TwinklingStarField(count = 28)

        // Ambient glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(avatar.bgFrom.copy(alpha = 0.12f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Back
            if (!isLoading) {
                BackButton(onClick = onBack, dark = true)
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Welcome headline with bob
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "\uD83D\uDC4B", fontSize = 32.sp,
                    modifier = Modifier.offset(y = bobY.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Welcome aboard,",
                    fontSize = 24.sp, fontWeight = FontWeight.Black,
                    color = Color.White, letterSpacing = (-0.6).sp,
                )
                Text(
                    "${data.name}!",
                    fontSize = 24.sp, fontWeight = FontWeight.Black,
                    color = avatar.bgFrom, letterSpacing = (-0.6).sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Your MindQuest profile is ready",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = Color(0xFFC7D2FE).copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Profile card ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
            ) {
                // Avatar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = bobY.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(avatar.bgFrom, avatar.bgTo)))
                            .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(avatar.emoji, fontSize = 40.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(data.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(avatar.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFC7D2FE).copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // XP pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(XpGold.copy(alpha = 0.12f))
                            .border(1.dp, XpGold.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u26A1", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("0 XP \u00B7 Level 1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = XpGold)
                        }
                    }
                }

                // Divider
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .height(1.dp).background(Color.White.copy(alpha = 0.07f)),
                )

                // Detail list — single column for clean layout on all screen sizes
                val details = listOf(
                    "\uD83C\uDF0D" to ("Country" to data.country),
                    "\uD83C\uDFD8\uFE0F" to ("City" to data.city),
                    "\uD83C\uDF93" to ("Grade" to data.grade),
                    "\uD83C\uDFEB" to ("School" to data.school),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    details.forEach { (icon, pair) ->
                        val (label, value) = pair
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    label.uppercase(), fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC7D2FE).copy(alpha = 0.4f),
                                    letterSpacing = 0.8.sp,
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    value, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(11.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Auth Buttons / Loading ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                if (isLoading) {
                    // Loading progress
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Setting up your profile\u2026",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(loadPct / 100f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(avatar.bgFrom, avatar.bgTo))),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            when {
                                loadPct < 40 -> "Creating your account\u2026"
                                loadPct < 75 -> "Personalising your quizzes\u2026"
                                else -> "Almost there!"
                            },
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC7D2FE).copy(alpha = 0.4f),
                        )
                    }
                } else {
                    // Google
                    Button(
                        onClick = { isLoading = true; onGoogle() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    ) {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF1A73E8)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phone
                    OutlinedButton(
                        onClick = onPhone,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\uD83D\uDCF1", fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("Continue with Phone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Anonymous
                    OutlinedButton(
                        onClick = { isLoading = true; onAnonymous() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\uD83D\uDC7B", fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("Try Anonymously", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.55f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sign in later to save progress & enter tournaments",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// PHONE AUTH FLOW  —  within the Journey (white bg, matches JSX)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun PhoneAuthFlow(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()
    val authScreen by viewModel.authScreen.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val resendTimer by viewModel.resendTimer.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
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
                label = "phone_transition",
            ) { screen ->
                when (screen) {
                    AuthScreenState.PHONE -> JourneyPhoneEntry(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { viewModel.updatePhoneNumber(it) },
                        onSendOtp = { viewModel.sendOtp(phoneNumber) },
                        onBack = onBack,
                        isLoading = authState is UiState.Loading,
                    )
                    AuthScreenState.OTP -> JourneyOtpVerify(
                        otpCode = otpCode,
                        onOtpChange = { viewModel.updateOtpCode(it) },
                        onVerify = { viewModel.verifyOtp(otpCode) },
                        onResend = { viewModel.resendOtp() },
                        onBack = { viewModel.navigateToPhone() },
                        resendTimer = resendTimer,
                        phoneNumber = phoneNumber,
                        isLoading = authState is UiState.Loading,
                    )
                    AuthScreenState.VERIFIED -> JourneyVerified()
                    else -> { onBack() }
                }
            }
        }
    }
}

@Composable
private fun JourneyPhoneEntry(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
) {
    val isValid = phoneNumber.length == 10

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp),
    ) {
        BackButton(onClick = onBack)

        Text("Enter Phone Number", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Text("We'll send a 6-digit verification code", fontSize = 13.sp, color = TextMuted)

        Spacer(modifier = Modifier.height(28.dp))

        Text("PHONE NUMBER", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isValid) FocusBg else Color.White)
                .border(2.dp, if (isValid) PrimaryColor else BorderLight, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("\uD83C\uDDEE\uD83C\uDDF3 +91", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.width(10.dp))
            Box(Modifier.width(1.dp).height(22.dp).background(BorderLight))
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = phoneNumber,
                onValueChange = { v -> if (v.length <= 10 && v.all { it.isDigit() }) onPhoneChange(v) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 16.sp, color = TextDark, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (phoneNumber.isEmpty()) Text("98765 43210", color = TextMuted, fontSize = 16.sp, letterSpacing = 1.5.sp)
                        inner()
                    }
                },
            )
            if (isValid) {
                Text("\u2713", fontSize = 16.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        JourneyGradientButton(
            text = "Send OTP \u2192",
            onClick = onSendOtp,
            enabled = isValid && !isLoading,
        )
    }
}

@Composable
private fun JourneyOtpVerify(
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
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(otpCode) { if (otpCode.length == 6) onVerify() }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp),
    ) {
        BackButton(onClick = onBack)

        Text("Verify OTP", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Sent to +91 $phoneNumber", fontSize = 13.sp, color = TextMuted)

        Spacer(modifier = Modifier.height(32.dp))

        // OTP boxes
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BasicTextField(
                value = otpCode,
                onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) onOtpChange(v) },
                modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                        repeat(6) { i ->
                            val c = otpCode.getOrNull(i)
                            val hasFill = c != null
                            Box(
                                modifier = Modifier
                                    .size(48.dp, 56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (hasFill) OtpFilledBg else Color(0xFFFAFAFA))
                                    .border(2.dp, if (hasFill) PrimaryColor else BorderLight, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(c?.toString() ?: "", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                            }
                        }
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Didn't receive? ", fontSize = 13.sp, color = TextMuted)
            if (resendTimer > 0) {
                Text("Resend in ${resendTimer}s", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryColor)
            } else {
                Text("Resend", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryColor, modifier = Modifier.clickable { onResend() })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        JourneyGradientButton(
            text = "Verify \u2192",
            onClick = onVerify,
            enabled = otpCode.length == 6 && !isLoading,
        )
    }
}

@Composable
private fun JourneyVerified() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "verified_scale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        AnimatedVisibility(
            visible = true,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
        ) {
            Box(
                modifier = Modifier.size(72.dp).scale(scale).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(SuccessGreen, SuccessGreenDark))),
                contentAlignment = Alignment.Center,
            ) {
                Text("\u2713", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text("Verified!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Redirecting...", fontSize = 13.sp, color = TextMuted)
    }
}

// ═══════════════════════════════════════════════════════════════════
// DONE SCREEN  —  celebration with floating emoji
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun DoneScreen(name: String, onStart: () -> Unit) {
    val bobTransition = rememberInfiniteTransition(label = "done_bob")
    val bobY by bobTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "done_bob_y",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            "\uD83C\uDF89", fontSize = 64.sp,
            modifier = Modifier.offset(y = bobY.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "Welcome, $name!",
            fontSize = 26.sp, fontWeight = FontWeight.Black,
            color = TextDark, textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your journey to the top of the\nleaderboard starts now.",
            fontSize = 14.sp, color = TextMuted,
            textAlign = TextAlign.Center, lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(32.dp))

        JourneyGradientButton(
            text = "Start Learning \uD83D\uDE80",
            onClick = onStart,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════
//  S H A R E D   C O M P O N E N T S
// ═══════════════════════════════════════════════════════════════════

// ── Twinkling star field (single transition drives all stars) ────
@Composable
private fun TwinklingStarField(count: Int = 20) {
    val transition = rememberInfiniteTransition(label = "stars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "star_phase",
    )

    val positions = remember(count) {
        (0 until count).map { i ->
            val x = ((i * 37 + 5) % 95).toFloat()
            val y = ((i * 53 + 3) % 90).toFloat()
            val size = 1.5f + (i % 3) * 0.7f
            val offset = i * 0.45f
            Triple(x, y, size to offset)
        }
    }

    positions.forEach { (xPct, yPct, sizeOffset) ->
        val (size, offset) = sizeOffset
        val alpha = 0.12f + 0.55f * ((sin(phase.toDouble() + offset) + 1.0) / 2.0).toFloat()
        Box(
            modifier = Modifier
                .offset(x = (xPct * 3.8f).dp, y = (yPct * 8f).dp)
                .size(size.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = alpha)),
        )
    }
}

// ── Pulsing loading dots ────────────────────────────────────────
@Composable
private fun PulsingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_$index",
            )
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_alpha_$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha)),
            )
        }
    }
}

// ── Progress bar ────────────────────────────────────────────────
@Composable
private fun StepProgressBar(step: Int, total: Int) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fraction = step.toFloat() / total
    val animFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(500),
        label = "progress",
    )

    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = statusBarPadding + 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Step $step of $total", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            Text("${(fraction * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BorderLight),
        ) {
            Box(
                Modifier.fillMaxWidth(animFraction).height(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(IndigoLight, PrimaryColor))),
            )
        }
    }
}

// ── Back button ─────────────────────────────────────────────────
@Composable
private fun BackButton(onClick: () -> Unit, dark: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .padding(start = if (dark) 24.dp else 0.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, "Back",
            modifier = Modifier.size(18.dp),
            tint = if (dark) Color.White.copy(alpha = 0.4f) else TextSubtle,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Back", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (dark) Color.White.copy(alpha = 0.4f) else TextSubtle,
        )
    }
}

// ── Styled input field ──────────────────────────────────────────
@Composable
private fun JourneyInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLabel)
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(2.dp, if (value.isNotEmpty()) PrimaryColor else BorderLight, RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp),
            textStyle = TextStyle(fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.Medium),
            singleLine = true,
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = TextMuted, fontSize = 15.sp)
                    inner()
                }
            },
        )
    }
}

// ── Styled selector field → opens dialog ────────────────────────
@Composable
private fun SelectorField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    hint: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLabel)
            if (hint != null) {
                Text(hint, fontSize = 11.sp, color = PrimaryColor)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(2.dp, BorderLight, RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable { showDialog = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(20.dp), tint = TextMuted)
        }

        if (showDialog) {
            SelectorDialog(
                title = label,
                options = options,
                selected = value,
                onSelect = {
                    onSelect(it)
                    showDialog = false
                },
                onDismiss = { showDialog = false },
            )
        }
    }
}

// ── Selector dialog (premium feel) ──────────────────────────────
@Composable
private fun SelectorDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp),
        ) {
            // Header
            Text(
                "Select $title",
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = TextDark, letterSpacing = (-0.3).sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap to choose",
                fontSize = 12.sp, color = TextMuted,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Options list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height((options.size * 56).coerceAtMost(350).dp),
            ) {
                items(options) { option ->
                    val isSel = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSel) Color(0xFFEEF2FF) else Color(0xFFF9FAFB),
                            )
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) IndigoLight else BorderLight,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            option,
                            fontSize = 15.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) PrimaryColor else TextDark,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSel) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Gradient CTA button ─────────────────────────────────────────
@Composable
private fun JourneyGradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    accentFrom: Color = IndigoLight,
    accentTo: Color = PrimaryColor,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        enabled = enabled,
    ) {
        Box(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (enabled) Brush.linearGradient(listOf(accentFrom, accentTo))
                    else Brush.linearGradient(listOf(BorderLight, BorderLight)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = if (enabled) Color.White else TextMuted,
            )
        }
    }
}
