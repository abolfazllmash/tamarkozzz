package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import com.example.data.*
import com.example.game.*
import com.example.ui.GameViewModel
import com.example.ui.TrialScreen
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.SgkaraFamily
import java.util.Random

// Shadow system FontFamily defaults to dynamically use our beautiful Persian/Arabic custom font
private object FontFamily {
    val Monospace = SgkaraFamily
    val SansSerif = SgkaraFamily
    val Default = SgkaraFamily
    val Serif = SgkaraFamily
}

private fun String.toPersian(): String {
    var result = this
    val englishDigits = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
    val persianDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    for (i in 0..9) {
        result = result.replace(englishDigits[i], persianDigits[i])
    }
    return result
}

@Composable
private fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = SgkaraFamily,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    textDecoration: androidx.compose.ui.text.style.TextDecoration? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    androidx.compose.material3.Text(
        text = text.toPersian(),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style.copy(fontFamily = fontFamily)
    )
}

// Neon Theme Colors
object CyberTheme {
    val CharcoalDeep = Color(0xFF0D0D0D)
    val PanelDark = Color(0xFF1A1A1A)
    val GreenNeon = Color(0xFF39FF14)
    val RedDanger = Color(0xFFFF2E63)
    val BlueElectric = Color(0xFF00D2FF)
    val GoldCoin = Color(0xFFFFD700)
    val OrangeRetro = Color(0xFFFF7300)
    val PinkNeon = Color(0xFFFF00D2)
    val GrayMuted = Color(0xFF6B6E76)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenState by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val dailyDialogShow by viewModel.showDailyRewardDialog.collectAsStateWithLifecycle()

    // کنترل دکمه بک گوشی برای حرکت بین صفحات به جای خروج از برنامه
    BackHandler(enabled = screenState != TrialScreen.Menu) {
        viewModel.handleBackPress()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberTheme.CharcoalDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Main Screen Router
        Crossfade(
            targetState = screenState,
            animationSpec = tween(250),
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {
                TrialScreen.Menu -> MenuLayout(viewModel, profile)
                TrialScreen.LevelSelect -> LevelSelectLayout(viewModel)
                TrialScreen.Upgrades -> UpgradesLayout(viewModel, profile)
                TrialScreen.Achievements -> AchievementsLayout(viewModel)
                TrialScreen.Quests -> QuestsLayout(viewModel)
                TrialScreen.Gameplay -> GameplayLayout(viewModel)
                TrialScreen.GameOver -> GameOverLayout(viewModel, profile)
                TrialScreen.GameSuccess -> GameSuccessLayout(viewModel, profile)
            }
        }

        // Daily Login Celebration Dialog (Day 1 - Day 7 Streak Carousel)
        if (dailyDialogShow && profile != null) {
            DailyRewardDialog(
                streak = profile!!.streak,
                onClaim = { day, reward -> 
                    viewModel.claimDailyRewardCoins(day, reward)
                }
            )
        }

    }
}

@Composable
fun MenuHeader(profile: UserProfileEntity?, title: String) {
    if (profile == null) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
        border = BorderStroke(1.dp, CyberTheme.GrayMuted.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                // نوار میزان تمرکز (جایگزین XP)
                val focusProgress = (profile.level.toFloat() / 100f).coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "تمرکز ${profile.level}٪",
                        color = CyberTheme.GreenNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(focusProgress)
                                .background(CyberTheme.GreenNeon)
                        )
                    }
                    Text(
                        text = "${profile.level}٪ از ۱۰۰",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Coins counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coins",
                    tint = CyberTheme.GoldCoin,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${profile.coins}",
                    color = CyberTheme.GoldCoin,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TabNavigationRow(
    activeTab: String,
    onNavigate: (TrialScreen) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("صفحه تمرکز", Icons.Default.PlayArrow, TrialScreen.Menu),
                Triple("فروشگاه", Icons.Default.ShoppingCart, TrialScreen.Upgrades),
                Triple("مأموریت‌ها", Icons.Default.Assignment, TrialScreen.Quests),
                Triple("افتخارات", Icons.Default.EmojiEvents, TrialScreen.Achievements)
            )

            tabs.forEach { (name, icon, route) ->
                val selected = activeTab == name
                val animColor by animateColorAsState(
                    targetValue = if (selected) CyberTheme.GreenNeon else Color.Gray,
                    animationSpec = tween(200)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = animColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = name,
                        color = animColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// 1. MENU LAYOUT (Home / Level Select Trigger)
@Composable
fun MenuLayout(viewModel: GameViewModel, profile: UserProfileEntity?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuHeader(profile, "سنجش تمرکز")

        // Glowing Hero Arena Preview
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Animated pulse rings around logo
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind {
                        drawCircle(
                            color = CyberTheme.GreenNeon.copy(alpha = 0.15f * pulseScale),
                            radius = size.minDimension / 2f
                        )
                        drawCircle(
                            color = CyberTheme.GreenNeon,
                            radius = size.minDimension / 2.3f * pulseScale,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield logo",
                        tint = CyberTheme.GreenNeon,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "آماده تمرکز",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Play Action
            Button(
                onClick = { viewModel.navigateTo(TrialScreen.LevelSelect) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.GreenNeon),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(58.dp)
                    .testTag("play_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "می‌خوام تمرکزم رو بسنجم!",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // فقط بخش تداوم روزانه (مثل تصویر)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = CyberTheme.OrangeRetro,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تداوم تمرکز",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${profile?.streak ?: 1} روز متوالی",
                        color = CyberTheme.OrangeRetro,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        TabNavigationRow("صفحه تمرکز", onNavigate = { viewModel.navigateTo(it) })
    }
}

// 2. LEVEL SELECT LAYOUT (100 progressive levels selection)
@Composable
fun LevelSelectLayout(viewModel: GameViewModel) {
    val listLevels = remember { (1..100).toList() }
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(TrialScreen.Menu) }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "مأموریت‌های تمرکز",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(48.dp)) // padding
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listLevels) { lvl ->
                val config = remember(lvl) { DynamicLevelConfig.generate(lvl) }
                val score = viewModel.getHighScoreForLevel(lvl)
                
                val borderPaint = when (config.difficultyRating) {
                    "آموزشی" -> CyberTheme.GreenNeon.copy(alpha = 0.4f)
                    "آسان" -> CyberTheme.BlueElectric.copy(alpha = 0.4f)
                    "متوسط" -> CyberTheme.OrangeRetro.copy(alpha = 0.4f)
                    "سخت" -> CyberTheme.PinkNeon.copy(alpha = 0.4f)
                    else -> CyberTheme.RedDanger.copy(alpha = 0.5f)
                }

                val isUnlocked = viewModel.isLevelUnlocked(lvl)

                Card(
                    onClick = { 
                        if (isUnlocked) {
                            viewModel.selectLevel(lvl) 
                        } else {
                            android.widget.Toast.makeText(context, "این مرحله قفل است! ابتدا مراحل قبلی را با موفقیت تمام کنید.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("level_card_$lvl"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) CyberTheme.PanelDark else CyberTheme.PanelDark.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isUnlocked) borderPaint else Color.DarkGray.copy(alpha = 0.4f)
                    ),
                    enabled = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "مرحله $lvl",
                            color = if (isUnlocked) Color.White else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!isUnlocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "قفل",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = config.difficultyRating,
                                color = borderPaint.copy(alpha = 1.0f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${config.survivalTargetSecs} ثانیه",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Best",
                                    tint = if (score >= config.survivalTargetSecs) CyberTheme.GoldCoin else Color.DarkGray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format("%.1f ثانیه", score),
                                    color = if (score > 0) CyberTheme.GoldCoin else Color.DarkGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}

// 3. UPGRADES / SHOP LAYOUT (Permanent Upgrades + Cosmetics Unlocks)
data class BuyableAbility(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val cost: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val desc: String
)

@Composable
fun UpgradesLayout(viewModel: GameViewModel, profile: UserProfileEntity?) {
    val context = LocalContext.current
    val chargesMap by viewModel.abilityChargesMap.collectAsStateWithLifecycle()
    val upgradesKeys = listOf(
        Triple("shield_duration", "موندگاری بیشتر سپر!", Icons.Default.Shield),
        Triple("slow_mo_duration", "حرکت لاک‌پشتی خطرها!", Icons.Default.HourglassEmpty)
    )

    val customSkins = listOf(
        Triple("neon_green", "سبز زمردی", CyberTheme.GreenNeon),
        Triple("neon_cyan", "آبی", CyberTheme.BlueElectric),
        Triple("neon_pink", "صورتی", CyberTheme.PinkNeon),
        Triple("retro_orange", "نارنجی", CyberTheme.OrangeRetro),
        Triple("royal_gold", "طلایی", CyberTheme.GoldCoin)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MenuHeader(profile, "فروشگاه")

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "ارتقای ویژگی‌ها",
                    color = CyberTheme.BlueElectric,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(upgradesKeys) { item ->
                val (key, title, icon) = item
                val lvl = profile?.getUpgradeLevel(key) ?: 0
                val cost = profile?.getUpgradeCost(key) ?: 0
                val capped = lvl >= 3

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = CyberTheme.BlueElectric,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (key == "shield_duration") "یه ثانیه طولانی‌ترش کن" else "یک ثانیه بیشتر کندشون کن",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Indicator level dots
                                Row {
                                    for (i in 1..3) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (i <= lvl) CyberTheme.GreenNeon else Color.Gray.copy(alpha = 0.3f)
                                                )
                                                .padding(horizontal = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.executeUpgradeStats(key) },
                            enabled = !capped && (profile?.coins ?: 0) >= cost,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTheme.GreenNeon,
                                disabledContainerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (capped) "حداکثر" else "$cost سکه",
                                color = if (capped) Color.Gray else Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ظاهر مهره",
                    color = CyberTheme.PinkNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(customSkins) { item ->
                val (skinId, name, color) = item
                val isUnlocked = profile?.isSkinUnlocked(skinId) ?: false
                val isEquipped = profile?.equippedSkin == skinId
                val cost = when (skinId) {
                    "neon_cyan" -> 500
                    "neon_pink" -> 1200
                    "retro_orange" -> 2500
                    "royal_gold" -> 5000
                    else -> 0
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
                    border = BorderStroke(
                        1.dp,
                        if (isEquipped) color else Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isEquipped) "انتخاب‌شده" else if (isUnlocked) "باز شده" else "قیمت: $cost سکه",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (!isUnlocked) {
                            Button(
                                onClick = {
                                    if (skinId == "royal_gold" && (profile?.streak ?: 0) < 7) {
                                        android.widget.Toast.makeText(context, "لازمه حداقل هفت روز پیاپی بازی کنی", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.executePurchaseSkin(skinId, cost)
                                    }
                                },
                                enabled = (skinId == "royal_gold" && (profile?.streak ?: 0) < 7) || (profile?.coins ?: 0) >= cost,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.PinkNeon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "خرید", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        } else if (!isEquipped) {
                            Button(
                                onClick = {
                                    if (skinId == "royal_gold" && (profile?.streak ?: 0) < 7) {
                                        android.widget.Toast.makeText(context, "لازمه حداقل هفت روز پیاپی بازی کنی", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.selectSkin(skinId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.BlueElectric),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "انتخاب", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = CyberTheme.GreenNeon,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "خرید مهارت‌های فعال",
                    color = CyberTheme.GreenNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            val buyableAbilities = listOf(
                BuyableAbility("shield", "Shield Barrier", "سپر حفاظتی", 150, Icons.Default.Shield, "یه سپر انرژی برات می‌سازه که جلوی یکی از برخوردها رو می‌گیره"),
                BuyableAbility("dash", "Warp Dash", "جهش سریع", 200, Icons.Default.FlashOn, "با سرعت بالا به سمتی که مشخص کردی می‌پره"),
                BuyableAbility("time_slow", "Temporal Slow", "آهسته‌ساز زمان", 250, Icons.Default.HourglassEmpty, "سرعت همه‌ی موانع و خطرها رو تا ۶۵٪ کمتر می‌کنه"),
                BuyableAbility("ghost", "Ghost Phase", "حالت شبح", 300, Icons.Default.VisibilityOff, "می‌تونی خیلی راحت و بی‌خطر از بین همه‌ی مانع‌ها رد بشی"),
                BuyableAbility("magnet", "Data Magnet", "آهنربای گوی", 180, Icons.Default.FilterCenterFocus, "همه‌ی سکه‌ها و انرژی‌های دور و برت رو مثل آهنربا جذب تو می‌کنه")
            )

            items(buyableAbilities) { ability ->
                val charges = chargesMap[ability.id] ?: 3
                val cost = ability.cost

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
                    border = BorderStroke(
                        1.dp,
                        if (charges > 0) CyberTheme.GreenNeon.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = ability.icon,
                                contentDescription = ability.nameEn,
                                tint = if (charges > 0) CyberTheme.GreenNeon else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = ability.nameFa,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "موجودی: $charges بار استفاده",
                                    color = if (charges > 0) CyberTheme.GreenNeon else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = ability.desc,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.executePurchaseAbility(ability.id, cost) },
                            enabled = (profile?.coins ?: 0) >= cost,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.GreenNeon),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "خرید (+۳) با $cost سکه",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

        }

        TabNavigationRow("فروشگاه", onNavigate = { viewModel.navigateTo(it) })
    }
}

// 4. QUESTS LAYOUT (Missions lists)
@Composable
fun QuestsLayout(viewModel: GameViewModel) {
    val questsList by viewModel.quests.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "چالش‌های تمرکز",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(onClick = { viewModel.navigateTo(TrialScreen.Menu) }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "مأموریت‌ها رو کامل کن تا کلی سکه خوشگل به جیب بزنی!",
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(questsList) { quest ->
                val progressFrac = quest.currentProgress.toFloat() / quest.target.coerceAtLeast(1)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
                    border = BorderStroke(
                        1.dp,
                        if (quest.isCompleted) CyberTheme.GreenNeon.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quest.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (quest.isCompleted) "ایول، حلّه!" else "${quest.currentProgress}/${quest.target}",
                                color = if (quest.isCompleted) CyberTheme.GreenNeon else CyberTheme.BlueElectric,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = quest.description,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Progress linear slider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFrac)
                                    .background(
                                        if (quest.isCompleted) CyberTheme.GreenNeon else CyberTheme.BlueElectric
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "سکه",
                                    tint = CyberTheme.GoldCoin,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "+${quest.rewardCoins} سکه",
                                    color = CyberTheme.GoldCoin,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Text(
                                text = quest.questType,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }

        TabNavigationRow("مأموریت‌ها", onNavigate = { viewModel.navigateTo(it) })
    }
}

// 5. ACHIEVEMENTS / AWARDS LAYOUT (100 milestones categorization)
@Composable
fun AchievementsLayout(viewModel: GameViewModel) {
    val itemsList by viewModel.achievements.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("همه") }
    val categories = listOf("همه", "پایداری", "مهارت", "اکتشاف", "تداوم", "استادی")

    val filteredList = remember(itemsList, selectedCategory) {
        if (selectedCategory == "همه") itemsList
        else itemsList.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تالار افتخارات",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(onClick = { viewModel.navigateTo(TrialScreen.Menu) }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "مجموع افتخارات: ${itemsList.size} (${itemsList.count { it.isUnlocked }} باز شده)",
            color = Color.Gray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        // Categories chip line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .background(
                            if (active) CyberTheme.PinkNeon else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (active) Color.White else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredList) { achieve ->
                val progressFrac = achieve.currentProgress.toFloat() / achieve.target.coerceAtLeast(1)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
                    border = BorderStroke(
                        1.dp,
                        if (achieve.isUnlocked) CyberTheme.PinkNeon.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (achieve.isUnlocked) Icons.Default.Verified else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (achieve.isUnlocked) CyberTheme.PinkNeon else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = achieve.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = achieve.description,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Linear indicator status
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressFrac)
                                        .background(
                                            if (achieve.isUnlocked) CyberTheme.PinkNeon else Color.Gray
                                        )
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${achieve.currentProgress}/${achieve.target}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Reward",
                                    tint = CyberTheme.GoldCoin,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "+${achieve.rewardCoins}",
                                    color = CyberTheme.GoldCoin,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        TabNavigationRow("افتخارات", onNavigate = { viewModel.navigateTo(it) })
    }
}

// 6. GAMEPLAY ARENA SCREEN
@Composable
fun GameplayLayout(viewModel: GameViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val promptPurchaseAbility by viewModel.promptPurchaseAbility.collectAsStateWithLifecycle()
    val survivalSec by viewModel.survivalTime.collectAsStateWithLifecycle()
    val targetLvl = viewModel.selectedLevel.collectAsStateWithLifecycle()

    val coinsByRun by viewModel.coinsCollected.collectAsStateWithLifecycle()
    val rawXpByRun by viewModel.xpEarned.collectAsStateWithLifecycle()
    val nearMissesByRun by viewModel.nearMisses.collectAsStateWithLifecycle()

    val pOffset by viewModel.playerPos.collectAsStateWithLifecycle()
    val scoreLimit = targetLvl.value.survivalTargetSecs

    // Ability Cooldown UI bindings
    val shieldCo by viewModel.shieldCooldown.collectAsStateWithLifecycle()
    val dashCo by viewModel.dashCooldown.collectAsStateWithLifecycle()
    val slowMoCo by viewModel.slowMoCooldown.collectAsStateWithLifecycle()
    val ghostCo by viewModel.ghostCooldown.collectAsStateWithLifecycle()
    val magnetCo by viewModel.magnetCooldown.collectAsStateWithLifecycle()

    // Buff trackers rings
    val shTime by viewModel.shieldTimeLeft.collectAsStateWithLifecycle()
    val spTime by viewModel.speedBoostTimeLeft.collectAsStateWithLifecycle()
    val slTime by viewModel.slowMoTimeLeft.collectAsStateWithLifecycle()
    val maTime by viewModel.magnetTimeLeft.collectAsStateWithLifecycle()
    val ghTime by viewModel.ghostTimeLeft.collectAsStateWithLifecycle()

    // Canvas coordinate scale sizes
    var canvasSizeWidth by remember { mutableStateOf(0f) }
    var canvasSizeHeight by remember { mutableStateOf(0f) }

    // Screen Shake effect calculation
    val screenShakeVal by viewModel.screenShakeAmount.collectAsStateWithLifecycle()
    val shakeX = if (screenShakeVal > 0) (Math.random() - 0.5f).toFloat() * screenShakeVal else 0f
    val shakeY = if (screenShakeVal > 0) (Math.random() - 0.5f).toFloat() * screenShakeVal else 0f

    // Dynamic scale ratios for dynamic background lines
    val speedMultiplier = 1.0f + (survivalSec / 5f).toInt() * 0.10f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .offset(x = shakeX.dp, y = shakeY.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HUD BAR
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مرحله ${targetLvl.value.levelNumber}",
                            color = CyberTheme.PinkNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "زمان پایداری: ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format("%.1f / %d ثانیه", survivalSec.coerceAtMost(scoreLimit.toFloat()), scoreLimit),
                                color = CyberTheme.GreenNeon,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Exit Match button
                    IconButton(
                        onClick = { viewModel.navigateTo(TrialScreen.Menu) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Quit", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Progress time bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((survivalSec / scoreLimit).coerceIn(0f, 1f))
                            .background(CyberTheme.GreenNeon)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Stats rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = CyberTheme.GoldCoin, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "$coinsByRun سکه", color = CyberTheme.GoldCoin, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = CyberTheme.PinkNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "$nearMissesByRun جاخالی", color = CyberTheme.PinkNeon, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "سرعت:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = String.format("%.1f برابر", speedMultiplier), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }

        // نوار قابلیت‌های فعال حذف شد تا کادر بازی کوچک نشود (طبق درخواست)

        // PHYSICAL ARENA CANVAS
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .onSizeChanged {
                    canvasSizeWidth = it.width.toFloat()
                    canvasSizeHeight = it.height.toFloat()
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (canvasSizeWidth > 0 && canvasSizeHeight > 0) {
                                val vx = (offset.x / canvasSizeWidth) * VirtualArena.SIZE
                                val vy = (offset.y / canvasSizeHeight) * VirtualArena.SIZE
                                viewModel.onUserTouchedArena(Offset(vx, vy))
                            }
                        },
                        onDrag = { change, _ ->
                            if (canvasSizeWidth > 0 && canvasSizeHeight > 0) {
                                val vx = (change.position.x / canvasSizeWidth) * VirtualArena.SIZE
                                val vy = (change.position.y / canvasSizeHeight) * VirtualArena.SIZE
                                viewModel.onUserTouchedArena(
                                    Offset(
                                        vx.coerceIn(40f, 960f),
                                        vy.coerceIn(40f, 960f)
                                    )
                                )
                            }
                        }
                    )
                }
                .testTag("game_arena_canvas")
        ) {
            val liveEnemies by viewModel.enemies.collectAsStateWithLifecycle()
            val liveOrbs by viewModel.orbs.collectAsStateWithLifecycle()
            val activeBlade by viewModel.rotatingBlade.collectAsStateWithLifecycle()
            val freezeSpot by viewModel.freezingZonePos.collectAsStateWithLifecycle()
            val freezeRad by viewModel.freezingZoneRadius.collectAsStateWithLifecycle()
            val activeLasers by viewModel.lasers.collectAsStateWithLifecycle()
            val darknessFog by viewModel.darknessActive.collectAsStateWithLifecycle()
            val arenaRadius by viewModel.currentArenaRadius.collectAsStateWithLifecycle()

            val activeParticles by viewModel.particles.collectAsStateWithLifecycle()
            val activeTextPopups by viewModel.textPopups.collectAsStateWithLifecycle()

            val pSkin = viewModel.userProfile.collectAsStateWithLifecycle().value?.equippedSkin

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width == 0f || size.height == 0f) return@Canvas

                val scaleX = size.width / VirtualArena.SIZE
                val scaleY = size.height / VirtualArena.SIZE

                // 1. Draw Dot Grid background (Geometric Balance)
                val dotSpacing = 50f
                for (x in 0..(VirtualArena.SIZE / dotSpacing).toInt()) {
                    for (y in 0..(VirtualArena.SIZE / dotSpacing).toInt()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            center = Offset(x * dotSpacing * scaleX, y * dotSpacing * scaleY),
                            radius = 1.2f * scaleX
                        )
                    }
                }

                // 2. Rectangular play environment layout & ticks ruler marking (geometric boundary per user's requests)
                val leftPx = (500f - arenaRadius) * scaleX
                val rightPx = (500f + arenaRadius) * scaleX
                val topPx = (500f - arenaRadius) * scaleY
                val bottomPx = (500f + arenaRadius) * scaleY
                val environmentWidth = rightPx - leftPx
                val environmentHeight = bottomPx - topPx

                val arenaCenter = Offset(500f * scaleX, 500f * scaleY)
                val physRadius = arenaRadius * scaleX

                // draw semi-ambient backdrop box
                drawRect(
                    color = CyberTheme.PanelDark.copy(alpha = 0.35f),
                    topLeft = Offset(leftPx, topPx),
                    size = androidx.compose.ui.geometry.Size(environmentWidth, environmentHeight)
                )

                // draw elegant high-contrast neon environment perimeter borders
                drawRect(
                    color = if (arenaRadius < 440f) CyberTheme.RedDanger else CyberTheme.BlueElectric,
                    topLeft = Offset(leftPx, topPx),
                    size = androidx.compose.ui.geometry.Size(environmentWidth, environmentHeight),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Add ruler marking tick marks around all edges of the rectangular boundary to represent measurement
                val tickInterval = 50f * scaleX
                var tempX = leftPx
                while (tempX <= rightPx) {
                    // draw top edge tick ticks
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(tempX, topPx),
                        end = Offset(tempX, topPx + 8f),
                        strokeWidth = 1.dp.toPx()
                    )
                    // draw bottom edge tick ticks
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(tempX, bottomPx),
                        end = Offset(tempX, bottomPx - 8f),
                        strokeWidth = 1.dp.toPx()
                    )
                    tempX += tickInterval
                }

                var tempY = topPx
                while (tempY <= bottomPx) {
                    // draw left edge tick ticks
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(leftPx, tempY),
                        end = Offset(leftPx + 8f, tempY),
                        strokeWidth = 1.dp.toPx()
                    )
                    // draw right edge tick ticks
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(rightPx, tempY),
                        end = Offset(rightPx - 8f, tempY),
                        strokeWidth = 1.dp.toPx()
                    )
                    tempY += tickInterval
                }

                // 3. Draw Freezing icy spots if active
                val activeFreeze = freezeSpot
                if (activeFreeze != null && freezeRad > 0f) {
                    drawCircle(
                        color = CyberTheme.BlueElectric.copy(alpha = 0.12f),
                        center = Offset(activeFreeze.x * scaleX, activeFreeze.y * scaleY),
                        radius = freezeRad * scaleX
                    )
                    drawCircle(
                        color = CyberTheme.BlueElectric.copy(alpha = 0.3f),
                        center = Offset(activeFreeze.x * scaleX, activeFreeze.y * scaleY),
                        radius = freezeRad * scaleX,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // 4. Draw Gravity swirl if active
                val isGravity = targetLvl.value.modifiersAllowed.contains(ActiveModifierType.GRAVITY_WELL)
                if (isGravity) {
                    drawCircle(
                        color = CyberTheme.PinkNeon.copy(alpha = 0.06f),
                        center = Offset(viewModel.gravityWellPos.x * scaleX, viewModel.gravityWellPos.y * scaleY),
                        radius = 120f * scaleX
                    )
                }

                // 5. Draw Particles
                activeParticles.forEach { p ->
                    drawCircle(
                        color = Color(p.color).copy(alpha = 1.0f - (p.curLife.toFloat() / p.maxLife)),
                        center = Offset(p.position.x * scaleX, p.position.y * scaleY),
                        radius = p.size * scaleX
                    )
                }

                // 6. Draw Collectible Orbs
                liveOrbs.forEach { orb ->
                    val color = when (orb.type) {
                        OrbType.COIN -> CyberTheme.GoldCoin
                        OrbType.XP -> CyberTheme.GreenNeon
                        OrbType.SHIELD -> CyberTheme.BlueElectric
                        OrbType.MAGNET -> CyberTheme.GoldCoin
                        OrbType.SLOW_MO -> Color.White
                        OrbType.SPEED_BOOST -> CyberTheme.PinkNeon
                    }
                    drawCircle(
                        color = color.copy(alpha = orb.life),
                        center = Offset(orb.position.x * scaleX, orb.position.y * scaleY),
                        radius = VirtualArena.ORB_RADIUS_DEFAULT * scaleX
                    )
                    // Draw outer beacon rings
                    drawCircle(
                        color = color.copy(alpha = orb.life * 0.3f),
                        center = Offset(orb.position.x * scaleX, orb.position.y * scaleY),
                        radius = (VirtualArena.ORB_RADIUS_DEFAULT + 8f) * scaleX,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // 7. Draw Active lasers
                activeLasers.forEach { laser ->
                    if (laser.chargeTimer > 0) {
                        // Drawing warning line (dashing / transparent)
                        val alpha = if (laser.chargeTimer % 10 < 5) 0.8f else 0.2f
                        if (laser.isVertical) {
                            drawLine(
                                color = CyberTheme.RedDanger.copy(alpha = alpha),
                                start = Offset(laser.coord * scaleX, 500f * scaleY - physRadius),
                                end = Offset(laser.coord * scaleX, 500f * scaleY + physRadius),
                                strokeWidth = 2.dp.toPx()
                            )
                        } else {
                            drawLine(
                                color = CyberTheme.RedDanger.copy(alpha = alpha),
                                start = Offset(500f * scaleX - physRadius, laser.coord * scaleY),
                                end = Offset(500f * scaleX + physRadius, laser.coord * scaleY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    } else if (laser.isFiring) {
                        // Thick lethal energy laser bar!
                        if (laser.isVertical) {
                            drawLine(
                                color = CyberTheme.RedDanger,
                                start = Offset(laser.coord * scaleX, 500f * scaleY - physRadius),
                                end = Offset(laser.coord * scaleX, 500f * scaleY + physRadius),
                                strokeWidth = 10.dp.toPx()
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(laser.coord * scaleX, 500f * scaleY - physRadius),
                                end = Offset(laser.coord * scaleX, 500f * scaleY + physRadius),
                                strokeWidth = 3.dp.toPx()
                            )
                        } else {
                            drawLine(
                                color = CyberTheme.RedDanger,
                                start = Offset(500f * scaleX - physRadius, laser.coord * scaleY),
                                end = Offset(500f * scaleX + physRadius, laser.coord * scaleY),
                                strokeWidth = 10.dp.toPx()
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(500f * scaleX - physRadius, laser.coord * scaleY),
                                end = Offset(500f * scaleX + physRadius, laser.coord * scaleY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                }

                // 8. Draw Rotating Blade modifier
                activeBlade?.let { blade ->
                    val angleRad = Math.toRadians(blade.angle.toDouble())
                    val bEndX = 500f + Math.cos(angleRad).toFloat() * blade.length
                    val bEndY = 500f + Math.sin(angleRad).toFloat() * blade.length

                    drawLine(
                        color = CyberTheme.RedDanger,
                        start = arenaCenter,
                        end = Offset(bEndX * scaleX, bEndY * scaleY),
                        strokeWidth = blade.thickness * scaleX
                    )
                    // glowing tip
                    drawCircle(
                        color = Color.White,
                        center = Offset(bEndX * scaleX, bEndY * scaleY),
                        radius = 8f * scaleX
                    )
                }

                // 9. Draw Menacing Spiky Obstacles/Enemies Blocks (Geometric Balance)
                liveEnemies.forEach { enemy ->
                    val bodyColor = if (enemy.colorType == 1) CyberTheme.GoldCoin else CyberTheme.RedDanger
                    val rad = enemy.radius * enemy.scale * scaleX
                    val sizeSp = rad * 2f
                    val left = enemy.position.x * scaleX - rad
                    val top = enemy.position.y * scaleY - rad
                    
                    val cx = enemy.position.x * scaleX
                    val cy = enemy.position.y * scaleY
                    
                    // Draw outer menacing 4-point warning cross-spikes
                    drawLine(
                        color = bodyColor,
                        start = Offset(cx - rad * 1.5f, cy),
                        end = Offset(cx + rad * 1.5f, cy),
                        strokeWidth = 3f * scaleX
                    )
                    drawLine(
                        color = bodyColor,
                        start = Offset(cx, cy - rad * 1.5f),
                        end = Offset(cx, cy + rad * 1.5f),
                        strokeWidth = 3f * scaleX
                    )

                    // Draw rotated diamond frame (spiky star vibe)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, cy - rad * 1.2f)
                        lineTo(cx + rad * 1.2f, cy)
                        lineTo(cx, cy + rad * 1.2f)
                        lineTo(cx - rad * 1.2f, cy)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = bodyColor.copy(alpha = 0.35f)
                    )
                    drawPath(
                        path = path,
                        color = bodyColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // inner core body
                    drawCircle(
                        color = Color.DarkGray,
                        center = Offset(cx, cy),
                        radius = rad * 0.5f
                    )
                    drawCircle(
                        color = Color.White,
                        center = Offset(cx, cy),
                        radius = rad * 0.3f
                    )
                }

                // 10. Draw FLASHLIGHT Dark Mode fog if active
                if (darknessFog) {
                    val pPhys = Offset(pOffset.x * scaleX, pOffset.y * scaleY)
                    // Radial mask centered on player
                    val fogBrush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.94f)),
                        center = pPhys,
                        radius = 180f * scaleX
                    )
                    drawRect(
                        brush = fogBrush,
                        size = size
                    )
                }

                // 11. Draw Player ship core (Geometric Balance Rotated-45 Shape)
                val playerPaintColor = when (pSkin) {
                    "neon_cyan" -> CyberTheme.BlueElectric
                    "neon_pink" -> CyberTheme.PinkNeon
                    "royal_gold" -> CyberTheme.GoldCoin
                    "retro_orange" -> CyberTheme.OrangeRetro
                    else -> CyberTheme.GreenNeon
                }

                val pPhysOffset = Offset(pOffset.x * scaleX, pOffset.y * scaleY)
                val isImmune = shTime > 0 || ghTime > 0
                val pr = viewModel.playerRadius * scaleX
                val pSize = pr * 2f

                // Outer ambient pulse/feedback ring
                drawCircle(
                    color = playerPaintColor.copy(alpha = 0.18f),
                    center = pPhysOffset,
                    radius = pr * 2.2f,
                    style = Stroke(width = 1.dp.toPx())
                )

                rotate(45f, pPhysOffset) {
                    // Translucent body fill
                    drawRoundRect(
                        color = (if (isImmune) Color.White else playerPaintColor).copy(alpha = 0.35f),
                        topLeft = Offset(pPhysOffset.x - pr, pPhysOffset.y - pr),
                        size = androidx.compose.ui.geometry.Size(pSize, pSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    // High-contrast clean outline
                    drawRoundRect(
                        color = if (isImmune) Color.White else playerPaintColor,
                        topLeft = Offset(pPhysOffset.x - pr, pPhysOffset.y - pr),
                        size = androidx.compose.ui.geometry.Size(pSize, pSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Accent border line
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = Offset(pPhysOffset.x - pr + 1.dp.toPx(), pPhysOffset.y - pr + 1.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(pSize - 2.dp.toPx(), pSize - 2.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Glowing immunity ring overlay
                if (shTime > 0) {
                    drawCircle(
                        color = CyberTheme.BlueElectric,
                        center = pPhysOffset,
                        radius = (viewModel.playerRadius + 14f) * scaleX,
                        style = Stroke(width = 3.dp.toPx())
                    )
                } else if (ghTime > 0) {
                    drawCircle(
                        color = CyberTheme.PinkNeon,
                        center = pPhysOffset,
                        radius = (viewModel.playerRadius + 11f) * scaleX,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Centered white power dot
                drawCircle(
                    color = if (isImmune) playerPaintColor else Color.White,
                    center = pPhysOffset,
                    radius = 4f * scaleX
                )
            }

            // Draw popups text "+Coins" "+Near Miss" overlay onto the canvas directly
            activeTextPopups.forEach { (coord, text) ->
                val scaleX = canvasSizeWidth / VirtualArena.SIZE
                val scaleY = canvasSizeHeight / VirtualArena.SIZE
                if (scaleX > 0 && scaleY > 0) {
                    Box(
                        modifier = Modifier.offset(
                            x = (coord.x * scaleX).dp,
                            y = (coord.y * scaleY).dp
                        )
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // HUD Overlays & Pre-game motion gates
            val isWaitingToStart by viewModel.isWaitingToStart.collectAsStateWithLifecycle()
            val countdownTextVal by viewModel.countdownValue.collectAsStateWithLifecycle()

            if (isWaitingToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "آماده",
                            color = CyberTheme.GreenNeon,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لمس کن و بکش تا بازی شروع بشه",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (countdownTextVal >= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    val label = if (countdownTextVal == 0) "برو!" else countdownTextVal.toString()
                    val color = when (countdownTextVal) {
                        3 -> CyberTheme.RedDanger
                        2 -> CyberTheme.OrangeRetro
                        1 -> CyberTheme.GoldCoin
                        else -> CyberTheme.GreenNeon
                    }
                    Text(
                        text = label,
                        color = color,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        modifier = Modifier.testTag("arcade_countdown_timer")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // FLOATING ACTION COOLDOWNS PANEL (ACTIVE ABILITIES TRIGGER)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ability list selectors mapped to actions
                val chargesMap by viewModel.abilityChargesMap.collectAsStateWithLifecycle()
                val shieldCharges = chargesMap["shield"] ?: 3
                val dashCharges = chargesMap["dash"] ?: 3
                val slowMoCharges = chargesMap["time_slow"] ?: 3
                val ghostCharges = chargesMap["ghost"] ?: 3
                val magnetCharges = chargesMap["magnet"] ?: 3

                AbilityBtn("سپر ($shieldCharges)", Icons.Default.Shield, shieldCo, chargesCount = shieldCharges, onUse = { viewModel.activateAbility("shield") })
                AbilityBtn("جهش ($dashCharges)", Icons.Default.FlashOn, dashCo, chargesCount = dashCharges, onUse = { viewModel.activateAbility("dash") })
                AbilityBtn("آهسته ($slowMoCharges)", Icons.Default.HourglassEmpty, slowMoCo, chargesCount = slowMoCharges, onUse = { viewModel.activateAbility("time_slow") })
                AbilityBtn("شبح ($ghostCharges)", Icons.Default.VisibilityOff, ghostCo, chargesCount = ghostCharges, onUse = { viewModel.activateAbility("ghost") })
                AbilityBtn("آهنربا ($magnetCharges)", Icons.Default.FilterCenterFocus, magnetCo, chargesCount = magnetCharges, onUse = { viewModel.activateAbility("magnet") })
            }
        }

        if (promptPurchaseAbility != null) {
            val abilityId = promptPurchaseAbility!!
            val abilityCost = when (abilityId) {
                "shield" -> 150
                "dash" -> 200
                "time_slow" -> 250
                "ghost" -> 300
                "magnet" -> 180
                else -> 100
            }
            val abilityNameFa = when (abilityId) {
                "shield" -> "سپر حفاظتی"
                "dash" -> "جهش سریع"
                "time_slow" -> "آهسته‌ساز زمان"
                "ghost" -> "حالت شبح"
                "magnet" -> "آهنربای گوی"
                else -> "قابلیت فعال"
            }
            val hasEnough = (profile?.coins ?: 0) >= abilityCost

            AlertDialog(
                onDismissRequest = { viewModel.promptPurchaseAbility.value = null },
                title = {
                    Text(
                        text = "مهارتت ته کشید!",
                        color = CyberTheme.PinkNeon,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "شارژ این قابلیتت تمام شده! می‌خوای با پرداخت $abilityCost تا سکه، ۳ بار شارژ اضافی براش بخری تا بتونی دوباره ازش استفاده کنی؟",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "قابلیتی که می‌خوای: $abilityNameFa",
                            color = CyberTheme.BlueElectric,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "قیمتش: $abilityCost تا سکه (واسه ۳ بار استفاده)",
                            color = CyberTheme.GoldCoin,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "موجودی سکه‌هات: ${profile?.coins ?: 0} تا سکه",
                            color = if (hasEnough) CyberTheme.GreenNeon else CyberTheme.RedDanger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.executePurchaseAbility(abilityId, abilityCost, onSuccess = {
                                viewModel.promptPurchaseAbility.value = null
                            })
                        },
                        enabled = hasEnough,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberTheme.GreenNeon,
                            disabledContainerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "آره، بخر برام", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { viewModel.promptPurchaseAbility.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "بی‌خیال", color = Color.White, fontFamily = FontFamily.SansSerif)
                    }
                },
                containerColor = CyberTheme.PanelDark,
                tonalElevation = 6.dp
            )
        }

        // دیالوگ توضیح چالش‌های تازه‌بازشده پیش از شروع مرحله
        val challengeInfo by viewModel.pendingChallengeInfo.collectAsStateWithLifecycle()
        if (challengeInfo.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissChallengeInfo() },
                title = {
                    Text(
                        text = "یه چالش جدید پیش روته!",
                        color = CyberTheme.GreenNeon,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "تو این مرحله این چیزای جدید اضافه شدن. حواست بهشون باشه:",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        challengeInfo.forEach { (title, desc) ->
                            Text(
                                text = "● $title",
                                color = CyberTheme.OrangeRetro,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = desc,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissChallengeInfo() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.GreenNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "فهمیدم، بزن بریم!", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                    }
                },
                containerColor = CyberTheme.PanelDark,
                tonalElevation = 6.dp
            )
        }
    }
}

@Composable
fun BuffTag(name: String, color: Color, t: Float) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$name ${String.format("%.1f ثانیه", t)}",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AbilityBtn(
    label: String,
    icon: ImageVector,
    cooldownSec: Float,
    chargesCount: Int,
    onUse: () -> Unit
) {
    val isCooled = cooldownSec <= 0f
    val infiniteTransition = rememberInfiniteTransition()
    val glowFraction by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (chargesCount <= 0) CyberTheme.RedDanger.copy(alpha = 0.1f)
                    else if (isCooled) CyberTheme.GreenNeon.copy(alpha = 0.15f * glowFraction)
                    else Color.White.copy(alpha = 0.05f)
                )
                .border(
                    2.dp,
                    if (chargesCount <= 0) CyberTheme.RedDanger.copy(alpha = 0.5f)
                    else if (isCooled) CyberTheme.GreenNeon else Color.Gray.copy(alpha = 0.3f),
                    CircleShape
                )
                .clickable { onUse() }
        ) {
            if (chargesCount <= 0) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = label,
                    tint = CyberTheme.RedDanger,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isCooled) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = CyberTheme.GreenNeon,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = String.format("%.0f", cooldownSec),
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (chargesCount <= 0) CyberTheme.RedDanger else if (isCooled) Color.White else Color.Gray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// 7. GAME OVER LAYOUT (Failure State detailed breakdown metrics)
@Composable
fun GameOverLayout(viewModel: GameViewModel, profile: UserProfileEntity?) {
    val secFraction by viewModel.survivalTime.collectAsStateWithLifecycle()
    val coinsEarnedFraction by viewModel.coinsCollected.collectAsStateWithLifecycle()
    val xpEarnedFraction by viewModel.xpEarned.collectAsStateWithLifecycle()
    val activeLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val nearMissFraction by viewModel.nearMisses.collectAsStateWithLifecycle()
    
    val rewardsList by viewModel.recentlyUnlockedRewards.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
            border = BorderStroke(1.dp, CyberTheme.RedDanger)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Destroyed",
                    tint = CyberTheme.RedDanger,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val loseTitles = remember {
                    listOf(
                        "شکست در مأموریت",
                        "باختی!",
                        "تمرکزت شکست",
                        "همین؟ نشد که",
                        "از پا افتادی"
                    )
                }
                val loseSubs = remember {
                    listOf(
                        "باورم نمیشه به همین راحتی باختی! بیا دوباره شانس خودت رو بسنج.",
                        "تمرکزت یه لحظه پرید و کارت تموم شد. این بار حواست رو جمع کن!",
                        "نزدیک بود ها... ولی نشد. بلندشو دوباره امتحان کن!",
                        "بلوک‌ها بردن این دور. نوبت توئه که جواب بدی!",
                        "هرکی می‌بازه که بازیکن نیست؛ بازیکن اونیه که دوباره بلند میشه."
                    )
                }
                val loseTitle = remember { loseTitles.random() }
                val loseSub = remember { loseSubs.random() }
                Text(
                    text = loseTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = loseSub,
                    color = CyberTheme.RedDanger,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Metrics Board
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            MetricsLine("زمان پایداری", String.format("%.1f ثانیه", secFraction), CyberTheme.BlueElectric)
            MetricsLine("سکه‌های کسب‌شده", "+$coinsEarnedFraction سکه", CyberTheme.GoldCoin)
            MetricsLine("تعداد جاخالی", "$nearMissFraction بار", CyberTheme.PinkNeon)
            MetricsLine("هدف", "${activeLevel.survivalTargetSecs} ثانیه", Color.LightGray)

            if (rewardsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "جایزه‌های جدید باز شد!",
                    color = CyberTheme.GreenNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                rewardsList.forEach { r ->
                    Text(
                        text = "• $r",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Restart Panel Action
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { viewModel.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.GreenNeon),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("restart_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تلاش دوباره",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.navigateTo(TrialScreen.LevelSelect) },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "انتخاب مرحله",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// 8. GAME SUCCESS LAYOUT (Win Ceremony celebrations)
@Composable
fun GameSuccessLayout(viewModel: GameViewModel, profile: UserProfileEntity?) {
    val secFraction by viewModel.survivalTime.collectAsStateWithLifecycle()
    val coinsEarnedFraction by viewModel.coinsCollected.collectAsStateWithLifecycle()
    val xpEarnedFraction by viewModel.xpEarned.collectAsStateWithLifecycle()
    val activeLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val nearMissFraction by viewModel.nearMisses.collectAsStateWithLifecycle()
    
    val rewardsList by viewModel.recentlyUnlockedRewards.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberTheme.PanelDark),
            border = BorderStroke(1.dp, CyberTheme.GreenNeon)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Cleared",
                    tint = CyberTheme.GreenNeon,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val winTitles = remember {
                    listOf(
                        "مأموریت موفق!",
                        "آفرین، بردی!",
                        "تمرکزت بی‌نقص بود!",
                        "عالی بودی!",
                        "تو از پسش براومدی!"
                    )
                }
                val winSubs = remember {
                    listOf(
                        "با تمرکز کامل از پس همه بلوک‌ها براومدی. حالا نوبت مرحله بعده!",
                        "دیدی گفتم می‌تونی؟ همینطور ادامه بده!",
                        "این یعنی تمرکز واقعی. مرحله بعد منتظرته!",
                        "بلوک‌ها هیچ شانسی در برابرت نداشتن. درخشیدی!",
                        "یه قدم به تمرکز ۱۰۰٪ نزدیک‌تر شدی. نگه‌ش دار!"
                    )
                }
                val winTitle = remember { winTitles.random() }
                val winSub = remember { winSubs.random() }
                Text(
                    text = winTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = winSub,
                    color = CyberTheme.GreenNeon,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Metrics Board
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (nearMissFraction == 0) {
                MetricsLine("اجرای بی‌نقص", "بدون برخورد نزدیک", CyberTheme.GoldCoin)
            }
            MetricsLine("کل زمان پایداری", String.format("%.1f ثانیه", secFraction), CyberTheme.BlueElectric)
            MetricsLine("سکه‌های یافته‌شده", "+$coinsEarnedFraction سکه", CyberTheme.GoldCoin)

            if (rewardsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "دستاوردهای جدید:",
                    color = CyberTheme.PinkNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                rewardsList.forEach { r ->
                    Text(
                        text = "• $r",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Actions navigation
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val nextLvl = (activeLevel.levelNumber + 1).coerceAtMost(100)
                    viewModel.selectLevel(nextLvl)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.GreenNeon),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("next_level_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مرحله بعد",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.navigateTo(TrialScreen.LevelSelect) },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "فهرست مراحل",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MetricsLine(label: String, value: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// 9. DAILY STREAK HARVEST DIALOG CEREMONY
@Composable
fun DailyRewardDialog(
    streak: Int,
    onClaim: (Int, Int) -> Unit
) {
    val dailyRewardsMap = listOf(50, 100, 150, 200, 300, 500, 1000) // day 1 - day 7 coins

    Dialog(onDismissRequest = { /* forced claim */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberTheme.CharcoalDeep),
            border = BorderStroke(2.dp, CyberTheme.GreenNeon),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Win",
                    tint = CyberTheme.GoldCoin,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "خوش اومدی! جایزه امروزت اینجاست",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ایول که هر روز سر می‌زنی! الان $streak روزه که پشت سر هم داری تمرین می‌کنی.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Days layout representation row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (day in 1..7) {
                        val active = day == streak
                        val passed = day < streak
                        val rewardVal = dailyRewardsMap[day - 1]

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) CyberTheme.GreenNeon.copy(alpha = 0.2f)
                                    else if (passed) Color.White.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.01f)
                                )
                                .border(
                                    1.dp,
                                    if (active) CyberTheme.GreenNeon else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "روز$day",
                                    color = if (active) CyberTheme.GreenNeon else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                if (day == 7) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = "Skin",
                                        tint = if (active) CyberTheme.PinkNeon else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$rewardVal",
                                        color = if (active) CyberTheme.GoldCoin else Color.LightGray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val claimReward = dailyRewardsMap[streak - 1]
                Button(
                    onClick = { onClaim(streak, claimReward) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTheme.GreenNeon),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (streak == 7) "دمت گرم! ظاهر ویژه روز هفتمو کادو بده بیاد" else "سکه‌ها رو بزار تو جیبم! ($claimReward سکه)",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
