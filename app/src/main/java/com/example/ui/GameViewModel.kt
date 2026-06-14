package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.game.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.random.Random

sealed class TrialScreen {
    object Menu : TrialScreen()
    object LevelSelect : TrialScreen()
    object Upgrades : TrialScreen()
    object Achievements : TrialScreen()
    object Quests : TrialScreen()
    object Gameplay : TrialScreen()
    object GameOver : TrialScreen()
    object GameSuccess : TrialScreen()
}

data class CoinPack(
    val id: String,
    val title: String,
    val coins: Int,
    val priceFa: String,
    val description: String
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = GameRepository(db)
    val synthesizer = GameSoundSynthesizer()

    val activeBuyCoinPack = MutableStateFlow<CoinPack?>(null)

    // Preferences for Level HighScores
    private val prefs = application.getSharedPreferences("avoid_blocks_prefs", Context.MODE_PRIVATE)

    // Exposed Flows from DB
    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quests: StateFlow<List<QuestEntity>> = repository.quests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    var currentScreen = MutableStateFlow<TrialScreen>(TrialScreen.Menu)
    var selectedLevel = MutableStateFlow<DynamicLevelConfig>(DynamicLevelConfig.generate(1))

    // Core Game States
    var playerPos = MutableStateFlow(Offset(500f, 500f))
    var playerTargetPos = MutableStateFlow(Offset(500f, 500f))
    var playerRadius = VirtualArena.PLAYER_RADIUS_DEFAULT
    var playerVelX = 0f
    var playerVelY = 0f
    
    // Start countdown controls
    val isWaitingToStart = MutableStateFlow(true)
    val countdownValue = MutableStateFlow(-1)

    fun isLevelUnlocked(lvl: Int): Boolean {
        if (lvl == 1) return true
        val prevLvl = lvl - 1
        val prevTarget = DynamicLevelConfig.generate(prevLvl).survivalTargetSecs
        val prevHighScore = getHighScoreForLevel(prevLvl)
        return prevHighScore >= prevTarget
    }

    fun onUserTouchedArena(target: Offset) {
        if (promptPurchaseAbility.value != null) return
        playerTargetPos.value = target
        if (isWaitingToStart.value) {
            isWaitingToStart.value = false
            countdownValue.value = -1 // Instant gameplay activation
            synthesizer.playSuccess() // Warm welcome start sound
        }
    }
    
    // Smooth touch controls parameters
    private var smoothingFactor = 0.15f // altered by permanent speed_bonus / ice zones

    // Gameplay active stats
    var survivalTime = MutableStateFlow(0f) // seconds
    var coinsCollected = MutableStateFlow(0)
    var xpEarned = MutableStateFlow(0)
    var nearMisses = MutableStateFlow(0)
    var orbsCollected = MutableStateFlow(0)
    var activeMultiplier = MutableStateFlow(1)

    // Visual effect states
    var screenShakeAmount = MutableStateFlow(0f)
    var flashBlink = MutableStateFlow(false)
    var particles = MutableStateFlow<List<Particle>>(emptyList())
    var textPopups = MutableStateFlow<List<Pair<Offset, String>>>(emptyList())

    // Modifiers/Hazard States
    var enemies = MutableStateFlow<List<Enemy>>(emptyList())
    var orbs = MutableStateFlow<List<Orb>>(emptyList())
    var currentArenaRadius = MutableStateFlow(500f) // default virtual extent
    var rotatingBlade = MutableStateFlow<RotatingBeam?>(null)
    var freezingZonePos = MutableStateFlow<Offset?>(null)
    var freezingZoneRadius = MutableStateFlow(0f)
    var gravityWellPos = Offset(500f, 500f)
    var lasers = MutableStateFlow<List<LaserHazard>>(emptyList())
    var darknessActive = MutableStateFlow(false)

    // Player temporary buffs
    var shieldTimeLeft = MutableStateFlow(0f)
    var speedBoostTimeLeft = MutableStateFlow(0f)
    var slowMoTimeLeft = MutableStateFlow(0f)
    var magnetTimeLeft = MutableStateFlow(0f)
    var ghostTimeLeft = MutableStateFlow(0f)

    // Ability Cooldown Timers (in seconds status)
    var shieldCooldown = MutableStateFlow(0f)
    var dashCooldown = MutableStateFlow(0f)
    var slowMoCooldown = MutableStateFlow(0f)
    var ghostCooldown = MutableStateFlow(0f)
    var magnetCooldown = MutableStateFlow(0f)

    // Near miss cool down per enemy to avoid double-triggers
    private val nearMissCoolDownMap = mutableMapOf<String, Long>()

    // Unlocked lists (for immediate UI response)
    var recentlyUnlockedRewards = MutableStateFlow<List<String>>(emptyList())

    // Physics looping job
    private var gameLoopJob: Job? = null

    // Daily streak logic state
    var showDailyRewardDialog = MutableStateFlow(false)
    var animationTriggered = MutableStateFlow(0) // increment to fire celebrations
    val promptPurchaseAbility = MutableStateFlow<String?>(null)
    val promptPurchaseLevel = MutableStateFlow<Int?>(null)

    // پیام توضیح چالش‌های تازه‌بازشده پیش از شروع مرحله (عنوان، توضیح محاوره‌ای)
    val pendingChallengeInfo = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    fun dismissChallengeInfo() {
        pendingChallengeInfo.value = emptyList()
    }

    private fun challengeExplanation(mod: ActiveModifierType): Pair<String, String> {
        return when (mod) {
            ActiveModifierType.LASER_GRID -> "لیزرها" to
                "هر چند وقت یه‌بار خط‌های لیزر از کنار صفحه رد می‌شن. اول یه خط هشدار می‌بینی، بعد لیزر شلیک می‌شه؛ پس تا خط رو دیدی، سریع برو کنار!"
            ActiveModifierType.FREEZING_ZONE -> "منطقه یخی" to
                "یه لکه یخی روی صفحه پیدا می‌شه. اگه واردش بشی، لوزیت سُر می‌خوره و سخت می‌شه کنترلش کنی؛ پس تا می‌تونی ازش فاصله بگیر!"
            ActiveModifierType.GRAVITY_WELL -> "چاه گرانش" to
                "یه نقطه وسط میدون هست که مثل آهنربا تو رو به سمت خودش می‌کشه. باید مدام خلافش حرکت کنی تا توش نیفتی!"
            ActiveModifierType.ROTATING_BLADE -> "تیغه چرخان" to
                "یه میله تیز از وسط صفحه مدام می‌چرخه. حواست به زاویه‌ش باشه و قبل از اینکه بهت برسه جاخالی بده!"
            ActiveModifierType.SHRINKING_ARENA -> "کوچک شدن میدون" to
                "کم‌کم لبه‌های میدون تنگ‌تر می‌شن و جای کمتری برای فرار داری. هرچی جلوتر بری باید دقیق‌تر بازی کنی!"
            ActiveModifierType.DARK_FLASHLIGHT -> "تاریکی" to
                "صفحه تاریک می‌شه و فقط دور و بر خودت رو می‌بینی. آروم و با احتیاط حرکت کن چون خطرها رو دیر می‌بینی!"
            ActiveModifierType.ASTEROID_RAIN -> "بارش شهاب" to
                "یهو یه سری گوی سریع از بالا می‌ریزن پایین. آماده باش که فرز جاخالی بدی!"
        }
    }

    private fun computeNewChallenges(level: Int): List<Pair<String, String>> {
        val currentMods = DynamicLevelConfig.generate(level).modifiersAllowed
        val prevMods = if (level > 1) DynamicLevelConfig.generate(level - 1).modifiersAllowed else emptyList()
        val newMods = currentMods.filter { it !in prevMods }
        return newMods.map { challengeExplanation(it) }
    }

    val abilityChargesMap = MutableStateFlow<Map<String, Int>>(emptyMap())

    fun getAbilityCharges(abilityId: String): Int {
        return prefs.getInt("ability_charges_$abilityId", 3)
    }

    fun setAbilityCharges(abilityId: String, charges: Int) {
        prefs.edit().putInt("ability_charges_$abilityId", charges).apply()
        loadAllAbilityCharges()
    }

    fun loadAllAbilityCharges() {
        abilityChargesMap.value = mapOf(
            "shield" to getAbilityCharges("shield"),
            "dash" to getAbilityCharges("dash"),
            "time_slow" to getAbilityCharges("time_slow"),
            "ghost" to getAbilityCharges("ghost"),
            "magnet" to getAbilityCharges("magnet")
        )
    }

    init {
        loadAllAbilityCharges()
        // Run daily login check on launch
        viewModelScope.launch {
            try {
                val profile = repository.getProfileDirect()
                val required = listOf("shield", "dash", "time_slow", "ghost", "magnet")
                val currentAbilities = profile.unlockedAbilities.split(",").map { it.trim() }.toSet()
                if (!currentAbilities.containsAll(required)) {
                    val updatedAbilities = (currentAbilities + required).filter { it.isNotEmpty() }.joinToString(",")
                    repository.updateProfile(profile.copy(unlockedAbilities = updatedAbilities))
                }
            } catch (_: Exception) {}

            delay(800)
            val result = repository.checkDailyStreak()
            if (result.second) {
                // Streak updated today, display daily ceremony popup!
                showDailyRewardDialog.value = true
                synthesizer.playSuccess()
            }
        }
    }

    fun triggerSystemHaptic() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(15)
                }
            }
        } catch (_: Exception) {}
    }

    fun navigateTo(screen: TrialScreen) {
        currentScreen.value = screen
        if (screen == TrialScreen.Menu) {
            recentlyUnlockedRewards.value = emptyList()
        }
    }

    fun handleBackPress() {
        when (currentScreen.value) {
            TrialScreen.LevelSelect,
            TrialScreen.Upgrades,
            TrialScreen.Achievements,
            TrialScreen.Quests -> {
                navigateTo(TrialScreen.Menu)
            }
            TrialScreen.Gameplay -> {
                // خروج از گیم‌پلی و برگشتن به لیست مراحل
                gameLoopJob?.cancel()
                navigateTo(TrialScreen.LevelSelect)
            }
            TrialScreen.GameOver,
            TrialScreen.GameSuccess -> {
                navigateTo(TrialScreen.LevelSelect)
            }
            TrialScreen.Menu -> {
                // در منوی اصلی، با بک زدن خارج می‌شود
            }
        }
    }

    fun selectLevel(level: Int) {
        selectedLevel.value = DynamicLevelConfig.generate(level)
        pendingChallengeInfo.value = computeNewChallenges(level)
        navigateTo(TrialScreen.Gameplay)
        startGame()
    }

    fun getHighScoreForLevel(levelNumber: Int): Float {
        return prefs.getFloat("highscore_lvl_$levelNumber", 0f)
    }

    private fun saveHighScoreIfBetter(levelNumber: Int, time: Float) {
        val currentBest = getHighScoreForLevel(levelNumber)
        if (time > currentBest) {
            prefs.edit().putFloat("highscore_lvl_$levelNumber", time).apply()
        }
    }

    fun executePurchaseSkin(skinId: String, cost: Int) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            if (current.coins >= cost && !current.isSkinUnlocked(skinId)) {
                val updated = current.copy(
                    coins = current.coins - cost,
                    unlockedSkins = current.unlockedSkins + ",$skinId"
                )
                repository.updateProfile(updated)
                synthesizer.playSuccess()
                // Quest unlock achievement
                repository.progressQuest("q_coins_100", 0) // refresh quests
            } else {
                synthesizer.playFailure()
            }
        }
    }

    fun executePurchaseAbility(abilityId: String, cost: Int, onSuccess: (() -> Unit)? = null, onFailure: (() -> Unit)? = null) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            if (current.coins >= cost) {
                val updated = current.copy(
                    coins = current.coins - cost
                )
                repository.updateProfile(updated)
                setAbilityCharges(abilityId, getAbilityCharges(abilityId) + 3)
                synthesizer.playSuccess()
                onSuccess?.invoke()
            } else {
                synthesizer.playFailure()
                onFailure?.invoke()
            }
        }
    }

    fun executePurchaseLevel(lvl: Int, cost: Int, onSuccess: (() -> Unit)? = null, onFailure: (() -> Unit)? = null) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            if (current.coins >= cost) {
                val updated = current.copy(coins = current.coins - cost)
                repository.updateProfile(updated)
                prefs.edit().putBoolean("level_purchased_$lvl", true).apply()
                synthesizer.playSuccess()
                onSuccess?.invoke()
            } else {
                synthesizer.playFailure()
                onFailure?.invoke()
            }
        }
    }

    fun selectSkin(skinId: String) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            if (current.isSkinUnlocked(skinId)) {
                repository.updateProfile(current.copy(equippedSkin = skinId))
                synthesizer.playSuccess()
            }
        }
    }

    fun executeUpgradeStats(key: String) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            val currentLvl = current.getUpgradeLevel(key)
            if (currentLvl >= 3) return@launch // maxed at 3 levels
            val cost = current.getUpgradeCost(key)
            if (current.coins >= cost) {
                // Update upgrade dictionary
                val upgrades = current.upgradesString.split(",")
                    .map { it.split(":") }
                    .map {
                        if (it[0] == key) "$key:${currentLvl + 1}" else "${it[0]}:${it[1]}"
                    }.joinToString(",")

                val updated = current.copy(
                    coins = current.coins - cost,
                    upgradesString = upgrades
                )
                repository.updateProfile(updated)
                synthesizer.playSuccess()
            } else {
                synthesizer.playFailure()
            }
        }
    }

    fun claimDailyRewardCoins(day: Int, amount: Int) {
        viewModelScope.launch {
            repository.incrementCoins(amount)
            showDailyRewardDialog.value = false
            synthesizer.playCollectCoin()
        }
    }

    fun buyCoinsSimulated(amount: Int) {
        viewModelScope.launch {
            repository.incrementCoins(amount)
            synthesizer.playSuccess()
        }
    }

    // Use active abilities
    fun activateAbility(abilityId: String) {
        if (currentScreen.value != TrialScreen.Gameplay) return

        val charges = getAbilityCharges(abilityId)
        if (charges <= 0) {
            promptPurchaseAbility.value = abilityId
            synthesizer.playFailure()
            return
        }

        val cooldown = when (abilityId) {
            "shield" -> shieldCooldown.value
            "dash" -> dashCooldown.value
            "time_slow" -> slowMoCooldown.value
            "ghost" -> ghostCooldown.value
            "magnet" -> magnetCooldown.value
            else -> 0f
        }
        if (cooldown > 0f) return

        setAbilityCharges(abilityId, charges - 1)

        when (abilityId) {
            "shield" -> {
                val profile = userProfile.value ?: return
                val durationBonus = profile.getUpgradeLevel("shield_duration") * 1.0f
                shieldTimeLeft.value = 4.0f + durationBonus
                shieldCooldown.value = 12.0f
                synthesizer.playSavingAbility()
                spawnVibrantBurst(playerPos.value, 0xFF00D2FF.toInt())
            }
            "dash" -> {
                dashCooldown.value = 8.0f
                // Calculate quick direction towards target
                val delta = playerTargetPos.value - playerPos.value
                val dist = Math.hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                if (dist > 1f) {
                    val dx = delta.x / dist
                    val dy = delta.y / dist
                    val dashDistance = 180f
                    val newPos = Offset(
                        (playerPos.value.x + dx * dashDistance).coerceIn(50f, 950f),
                        (playerPos.value.y + dy * dashDistance).coerceIn(50f, 950f)
                    )
                    playerPos.value = newPos
                    playerTargetPos.value = newPos
                    ghostTimeLeft.value = 0.5f // brief invincibility
                    synthesizer.playDash()
                    synthesizer.playSavingAbility()
                    spawnVibrantBurst(playerPos.value, 0xFF00FFCC.toInt())
                }
            }
            "time_slow" -> {
                val profile = userProfile.value
                val durationBonus = (profile?.getUpgradeLevel("slow_mo_duration") ?: 0) * 1.0f
                slowMoTimeLeft.value = 4.0f + durationBonus
                slowMoCooldown.value = 15.0f
                synthesizer.playSavingAbility()
                spawnVibrantBurst(Offset(500f, 500f), 0xFFE2E2E2.toInt())
            }
            "ghost" -> {
                ghostTimeLeft.value = 3.0f
                ghostCooldown.value = 14.0f
                synthesizer.playSavingAbility()
                spawnVibrantBurst(playerPos.value, 0xFFFF00D2.toInt())
            }
            "magnet" -> {
                magnetTimeLeft.value = 5.0f
                magnetCooldown.value = 10.0f
                synthesizer.playSavingAbility()
                spawnVibrantBurst(playerPos.value, 0xFFFFE600.toInt())
            }
        }
        triggerSystemHaptic()
    }

    // GAME LOOP MANAGER
    fun startGame() {
        gameLoopJob?.cancel()
        navigateTo(TrialScreen.Gameplay)
        
        // Reset states according to Touch pre-requisites
        isWaitingToStart.value = true
        countdownValue.value = -1
        promptPurchaseAbility.value = null

        val spawnOrigin = Offset(500f, 500f)
        playerPos.value = spawnOrigin
        playerTargetPos.value = spawnOrigin
        playerVelX = 0f
        playerVelY = 0f
        survivalTime.value = 0f
        coinsCollected.value = 0
        xpEarned.value = 0
        nearMisses.value = 0
        orbsCollected.value = 0
        activeMultiplier.value = 1
        screenShakeAmount.value = 0f
        flashBlink.value = false
        particles.value = emptyList()
        textPopups.value = emptyList()
        currentArenaRadius.value = 500f
        
        // Buffs/cooldowns resets
        shieldTimeLeft.value = 0f
        speedBoostTimeLeft.value = 0f
        slowMoTimeLeft.value = 0f
        magnetTimeLeft.value = 0f
        ghostTimeLeft.value = 0f

        shieldCooldown.value = 0f
        dashCooldown.value = 0f
        slowMoCooldown.value = 0f
        ghostCooldown.value = 0f
        magnetCooldown.value = 0f

        nearMissCoolDownMap.clear()

        // Get permanent upgrades parameters
        val profile = userProfile.value
        val speedLvl = profile?.getUpgradeLevel("speed_bonus") ?: 0
        smoothingFactor = 0.15f + (speedLvl * 0.02f)

        // Spawn initial enemies based on Level configuration
        val levelConfig = selectedLevel.value
        val initialList = mutableListOf<Enemy>()
        val rand = Random.Default
        for (i in 1..levelConfig.initialEnemyCount) {
            val angle = rand.nextFloat() * 2 * Math.PI
            val dist = 300f + rand.nextFloat() * 150f
            val ex = (500f + Math.cos(angle) * dist).toFloat()
            val ey = (500f + Math.sin(angle) * dist).toFloat()
            
            // Progressive level difficulty speed scaling (Keep difficulty in mind)
            val levelSpeedSkew = (selectedLevel.value.levelNumber - 1) * 0.08f
            val spd = 4.2f + levelSpeedSkew + rand.nextFloat() * (3f + levelSpeedSkew * 0.5f)
            val vx = (Math.cos(angle + Math.PI / 2) * spd).toFloat()
            val vy = (Math.sin(angle + Math.PI / 2) * spd).toFloat()

            initialList.add(
                Enemy(
                    id = "enemy_$i",
                    position = Offset(ex, ey),
                    velocity = Offset(vx, vy),
                    colorType = if (rand.nextFloat() < 0.2f) 1 else 0 // 20% tracers
                )
            )
        }
        enemies.value = initialList
        orbs.value = emptyList()

        // Establish dynamic hazards based on unlocked level features
        lasers.value = emptyList()
        rotatingBlade.value = if (levelConfig.modifiersAllowed.contains(ActiveModifierType.ROTATING_BLADE)) {
            RotatingBeam()
        } else null

        if (levelConfig.modifiersAllowed.contains(ActiveModifierType.FREEZING_ZONE)) {
            freezingZonePos.value = Offset(250f + rand.nextFloat() * 500f, 250f + rand.nextFloat() * 500f)
            freezingZoneRadius.value = 160f
        } else {
            freezingZonePos.value = null
        }

        darknessActive.value = levelConfig.modifiersAllowed.contains(ActiveModifierType.DARK_FLASHLIGHT)

        // Spawn first orbs
        spawnOrbsBatch()

        // Start 60FPS physics ticker flow with Delta-Time calculation for buttery-smooth movement
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.nanoTime()
            var frameCount = 0
            while (isActive) {
                val delayMs = 16L
                delay(delayMs)
                
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTime = currentTime

                if (promptPurchaseAbility.value != null) {
                    lastTime = System.nanoTime()
                    continue
                }
                
                frameCount++

                if (!isWaitingToStart.value && countdownValue.value < 0) {
                    // Update core clocks
                    survivalTime.value += dt
                    updateBuffsAndCooldowns(dt)

                    // Every 15s check random modifier additions if level permits
                    if (frameCount % 937 == 0) {
                        introduceRandomInGameModifier()
                    }

                    // Play periodic danger rumble if threats exist
                    if (frameCount % 120 == 0) {
                        if (enemies.value.isNotEmpty() || lasers.value.isNotEmpty() || rotatingBlade.value != null || freezingZonePos.value != null) {
                            synthesizer.playDangerRumble()
                        }
                    }

                    // Play alarm tick as target survival approaches
                    val targetSec = levelConfig.survivalTargetSecs
                    if (targetSec - survivalTime.value in 0.1f..3.0f && frameCount % 60 == 0) {
                        synthesizer.playTick()
                    }

                    // Check Victory Threshold
                    if (survivalTime.value >= targetSec && currentScreen.value == TrialScreen.Gameplay) {
                        triggerVictoryCeremony()
                        break
                    }

                    // Process Physics Frame
                    updatePhysicsFrame(dt, frameCount)
                } else {
                    // Pre-game idle or countdown active: only track player finger and drift graphics
                    updatePlayerPhysicsOnly(dt)
                    
                    // drift active particles if any exist in background
                    if (particles.value.isNotEmpty()) {
                        val activeParts = particles.value.toMutableList()
                        val iterator = activeParts.iterator()
                        while (iterator.hasNext()) {
                            val p = iterator.next()
                            p.curLife++
                            if (p.curLife >= p.maxLife) {
                                iterator.remove()
                            } else {
                                p.position = p.position + p.velocity * (dt * 60f)
                            }
                        }
                        particles.value = activeParts
                    }
                }
            }
        }
    }

    private fun updateBuffsAndCooldowns(step: Float) {
        shieldTimeLeft.value = (shieldTimeLeft.value - step).coerceAtLeast(0f)
        speedBoostTimeLeft.value = (speedBoostTimeLeft.value - step).coerceAtLeast(0f)
        slowMoTimeLeft.value = (slowMoTimeLeft.value - step).coerceAtLeast(0f)
        magnetTimeLeft.value = (magnetTimeLeft.value - step).coerceAtLeast(0f)
        ghostTimeLeft.value = (ghostTimeLeft.value - step).coerceAtLeast(0f)

        shieldCooldown.value = (shieldCooldown.value - step).coerceAtLeast(0f)
        dashCooldown.value = (dashCooldown.value - step).coerceAtLeast(0f)
        slowMoCooldown.value = (slowMoCooldown.value - step).coerceAtLeast(0f)
        ghostCooldown.value = (ghostCooldown.value - step).coerceAtLeast(0f)
        magnetCooldown.value = (magnetCooldown.value - step).coerceAtLeast(0f)
    }

    private fun introduceRandomInGameModifier() {
        val allowed = selectedLevel.value.modifiersAllowed
        if (allowed.isEmpty()) return

        val rand = Random.Default
        val modToTrigger = allowed[rand.nextInt(allowed.size)]
        
        viewModelScope.launch(Dispatchers.Default) {
            flashBlink.value = true
            screenShakeAmount.value = 15f
            synthesizer.playLaserWarning()
            delay(300)
            flashBlink.value = false
            screenShakeAmount.value = 0f

            when (modToTrigger) {
                ActiveModifierType.LASER_GRID -> {
                    // Activate 2 crossing vertical/horizontal warning lasers
                    val newLasers = listOf(
                        LaserHazard(isVertical = true, coord = 200f + rand.nextFloat() * 600f),
                        LaserHazard(isVertical = false, coord = 200f + rand.nextFloat() * 600f)
                    )
                    lasers.value = newLasers
                }
                ActiveModifierType.FREEZING_ZONE -> {
                    freezingZonePos.value = Offset(200f + rand.nextFloat() * 600f, 200f + rand.nextFloat() * 600f)
                    freezingZoneRadius.value = 180f
                }
                ActiveModifierType.ASTEROID_RAIN -> {
                    // Spawns sudden swift enemies sweeping across standard bounds
                    val swarm = mutableListOf<Enemy>()
                    for (i in 1..4) {
                        swarm.add(
                            Enemy(
                                id = "asteroid_${UUID.randomUUID()}",
                                position = Offset(100f + i * 200f, -50f),
                                velocity = Offset(0f, 12f),
                                radius = 40f,
                                colorType = 2
                            )
                        )
                    }
                    enemies.value = enemies.value + swarm
                }
                ActiveModifierType.GRAVITY_WELL -> {
                    gravityWellPos = Offset(300f + rand.nextFloat() * 400f, 300f + rand.nextFloat() * 400f)
                }
                ActiveModifierType.ROTATING_BLADE -> {
                    rotatingBlade.value = RotatingBeam(angle = rand.nextFloat() * 360f)
                }
                ActiveModifierType.SHRINKING_ARENA -> {
                    // Shrinking scale is animated in drawing
                }
                ActiveModifierType.DARK_FLASHLIGHT -> {
                    darknessActive.value = true
                }
            }
        }
    }

    fun updatePlayerPhysicsOnly(dt: Float) {
        val pOffset = playerPos.value
        val tOffset = playerTargetPos.value
        
        // Snappy positioning for starting placement
        val actualFac = smoothingFactor
        val forceX = (tOffset.x - pOffset.x) * (actualFac * 0.8f) * (dt * 60f)
        val forceY = (tOffset.y - pOffset.y) * (actualFac * 0.8f) * (dt * 60f)

        playerVelX += forceX
        playerVelY += forceY

        val friction = 0.78f // Snappy responsive control feel
        playerVelX *= friction
        playerVelY *= friction

        val nPx = pOffset.x + playerVelX * (dt * 60f)
        val nPy = pOffset.y + playerVelY * (dt * 60f)

        // restrict inside bounds (0f to 1000f box, with playerRadius cushion)
        val minX = playerRadius
        val maxX = 1000f - playerRadius
        val minY = playerRadius
        val maxY = 1000f - playerRadius

        playerPos.value = Offset(
            nPx.coerceIn(minX, maxX),
            nPy.coerceIn(minY, maxY)
        )
    }

    private fun updatePhysicsFrame(dt: Float, frameCount: Int) {
        val rand = Random.Default
        val profile = userProfile.value

        // Speed multi every 5s gets 10% faster: speedMultiplier
        val speedMultiBonus = 1.0f + (survivalTime.value / 5f).toInt() * 0.10f
        
        // Slow mo debuff multiplication
        val slowMoMultiplier = if (slowMoTimeLeft.value > 0f) 0.35f else 1.0f
        val netHazardsMultiplier = speedMultiBonus * slowMoMultiplier

        // Scaled factor relative to 60fps targets
        val frameScale = (dt * 60f).coerceIn(0.3f, 3.0f)

        // 1. Move Player towards target position smoothly (touch smoothing)
        var actualFac = smoothingFactor
        val isIce = freezingZonePos.value?.let { zone ->
            freezingZoneRadius.value > 0f &&
            Math.hypot(playerPos.value.x.toDouble() - zone.x, playerPos.value.y.toDouble() - zone.y) < freezingZoneRadius.value
        } ?: false

        if (isIce) {
            actualFac = 0.04f // extreme ice slide friction!
        } else if (speedBoostTimeLeft.value > 0f) {
            actualFac *= 1.5f
        }

        // Apply control vector
        val pOffset = playerPos.value
        var tOffset = playerTargetPos.value

        // Check gravity well pull mechanics
        val gravityAllowed = selectedLevel.value.modifiersAllowed.contains(ActiveModifierType.GRAVITY_WELL)
        if (gravityAllowed) {
            val dG = gravityWellPos - pOffset
            val distG = Math.hypot(dG.x.toDouble(), dG.y.toDouble()).toFloat()
            if (distG > 1f) {
                val pullForce = 3.5f / (distG / 150f).coerceAtLeast(0.5f)
                tOffset = Offset(
                    tOffset.x + (dG.x / distG) * pullForce,
                    tOffset.y + (dG.y / distG) * pullForce
                )
            }
        }

        // 2D Physics Engine: Steering impulse & inertia calculation (Matter.js style)
        val forceX = (tOffset.x - pOffset.x) * (actualFac * 0.8f) * frameScale
        val forceY = (tOffset.y - pOffset.y) * (actualFac * 0.8f) * frameScale

        playerVelX += forceX
        playerVelY += forceY

        // Friction/drag model tuned for high responsiveness and snappiness on request
        val friction = if (isIce) Math.pow(0.985, frameScale.toDouble()).toFloat() else Math.pow(0.78, frameScale.toDouble()).toFloat()
        playerVelX *= friction
        playerVelY *= friction

        val nPx = pOffset.x + playerVelX * frameScale
        val nPy = pOffset.y + playerVelY * frameScale
        
        // Confined Arena checks based on dynamic shrinking arena modifier
        val shrinkActive = selectedLevel.value.modifiersAllowed.contains(ActiveModifierType.SHRINKING_ARENA)
        val boundaryExtent = if (shrinkActive) {
            // pulsate boundary with progress time
            400f + Math.sin(survivalTime.value.toDouble() * 1.5).toFloat() * 80f
        } else {
            500f
        }
        currentArenaRadius.value = boundaryExtent

        // Check rectangular boundary instead of meaningless circular limit per request ("دایره رو بردار و تبدیلش کن به خطکشی")
        val minX = 500f - boundaryExtent + playerRadius
        val maxX = 500f + boundaryExtent - playerRadius
        val minY = 500f - boundaryExtent + playerRadius
        val maxY = 500f + boundaryExtent - playerRadius

        var finalPx = nPx
        var finalPy = nPy

        val isInvincible = shieldTimeLeft.value > 0f || ghostTimeLeft.value > 0f
        var hitBorder = false
        if (nPx < minX || nPx > maxX || nPy < minY || nPy > maxY) {
            hitBorder = true
        }

        if (hitBorder) {
            if (isInvincible) {
                if (shieldTimeLeft.value > 0f) {
                    shieldTimeLeft.value = 0f
                    ghostTimeLeft.value = 1.0f
                    synthesizer.playFailure()
                    spawnVibrantBurst(pOffset, 0xFFE2E2E2.toInt())
                    screenShakeAmount.value = 12f
                }
                finalPx = nPx.coerceIn(minX, maxX)
                finalPy = nPy.coerceIn(minY, maxY)
                playerVelX = -playerVelX * 0.7f
                playerVelY = -playerVelY * 0.7f
            } else {
                triggerDeathSequence()
                return
            }
        }

        playerPos.value = Offset(finalPx, finalPy)

        // Spawn player move sparks trail
        if (frameCount % 4 == 0) {
            val color = when (profile?.equippedSkin) {
                "neon_cyan" -> 0xFF00E5FF.toInt()
                "neon_pink" -> 0xFFFF00D2.toInt()
                "royal_gold" -> 0xFFFFD700.toInt()
                "retro_orange" -> 0xFFFF7300.toInt()
                else -> 0xFF39FF14.toInt() // Neon Green
            }
            spawnParticles(playerPos.value, count = 1, color = color)
        }

        // 2. Move Enemies
        val curEnemies = enemies.value.map { enemy ->
            // Update tracking behavior for type=1
            var vec = enemy.velocity
            if (enemy.colorType == 1 && frameCount % 12 == 0) {
                // Tracking vector
                val dP = playerPos.value - enemy.position
                val distP = Math.hypot(dP.x.toDouble(), dP.y.toDouble()).toFloat()
                if (distP > 1f) {
                    val trackSpd = 5f
                    vec = Offset(
                        (dP.x / distP) * trackSpd,
                        (dP.y / distP) * trackSpd
                    )
                }
            }

            var nEx = enemy.position.x + vec.x * netHazardsMultiplier * frameScale
            var nEy = enemy.position.y + vec.y * netHazardsMultiplier * frameScale

            // Rectangular Arena bounce instead of circular limit per request
            val eMinX = 500f - boundaryExtent + enemy.radius
            val eMaxX = 500f + boundaryExtent - enemy.radius
            val eMinY = 500f - boundaryExtent + enemy.radius
            val eMaxY = 500f + boundaryExtent - enemy.radius

            var vx = vec.x
            var vy = vec.y

            if (nEx < eMinX) {
                nEx = eMinX
                vx = -vx // reflect
            } else if (nEx > eMaxX) {
                nEx = eMaxX
                vx = -vx
            }

            if (nEy < eMinY) {
                nEy = eMinY
                vy = -vy // reflect
            } else if (nEy > eMaxY) {
                nEy = eMaxY
                vy = -vy
            }

            vec = Offset(vx, vy)

            // Pulsating logic for visual aesthetics
            var pulseValue = enemy.scale
            var currentDir = enemy.pulseDir
            if (frameCount % 6 == 0) {
                pulseValue += currentDir * 0.05f
                if (pulseValue > 1.2f) {
                    pulseValue = 1.2f
                    currentDir = -1
                } else if (pulseValue < 0.8f) {
                    pulseValue = 0.8f
                    currentDir = 1
                }
            }

            enemy.copy(position = Offset(nEx, nEy), velocity = vec, scale = pulseValue, pulseDir = currentDir)
        }

        // Apply high-performance 2D rigid-body elastic collisions among all blocks (Matter.js style)
        val enemiesList = curEnemies.toMutableList()
        for (i in 0 until enemiesList.size) {
            for (j in i + 1 until enemiesList.size) {
                val e1 = enemiesList[i]
                val e2 = enemiesList[j]
                
                val dx = e2.position.x - e1.position.x
                val dy = e2.position.y - e1.position.y
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val minDist = (e1.radius * e1.scale) + (e2.radius * e2.scale)
                
                if (dist < minDist && dist > 0.05f) {
                    val overlap = minDist - dist
                    val nx = dx / dist
                    val ny = dy / dist
                    
                    // Push overlapping rigid bodies apart to prevent sticking
                    val pushX = nx * overlap * 0.5f
                    val pushY = ny * overlap * 0.5f
                    
                    val newE1Pos = Offset(e1.position.x - pushX, e1.position.y - pushY)
                    val newE2Pos = Offset(e2.position.x + pushX, e2.position.y + pushY)
                    
                    // Evaluate velocities relative to standard normal collision axes
                    val rvx = e1.velocity.x - e2.velocity.x
                    val rvy = e1.velocity.y - e2.velocity.y
                    val velAlongNormal = rvx * nx + rvy * ny
                    
                    if (velAlongNormal > 0f) {
                        val restitution = 0.95f
                        val impulseScalar = -(1f + restitution) * velAlongNormal / 2f
                        
                        val impX = nx * impulseScalar
                        val impY = ny * impulseScalar
                        
                        enemiesList[i] = e1.copy(
                            position = newE1Pos,
                            velocity = Offset(e1.velocity.x + impX, e1.velocity.y + impY)
                        )
                        enemiesList[j] = e2.copy(
                            position = newE2Pos,
                            velocity = Offset(e2.velocity.x - impX, e2.velocity.y - impY)
                        )
                    } else {
                        enemiesList[i] = e1.copy(position = newE1Pos)
                        enemiesList[j] = e2.copy(position = newE2Pos)
                    }
                }
            }
        }
        enemies.value = enemiesList

        // 3. Update Rotating Blade modifier frame if exists
        rotatingBlade.value?.let { blade ->
            var newAngle = blade.angle + blade.speed * slowMoMultiplier * frameScale
            if (newAngle > 360f) newAngle -= 360f
            rotatingBlade.value = blade.copy(angle = newAngle)
            
            // Check Collision with blade pivot rod
            checkBladeTouch(blade)
        }

        // 4. Update Warning lasers
        val curLasers = lasers.value.mapNotNull { laser ->
            if (laser.chargeTimer > 0) {
                // decrease warning phase
                laser.copy(chargeTimer = (laser.chargeTimer - frameScale).toInt().coerceAtLeast(0))
            } else if (!laser.isFiring) {
                // activate fire!
                synthesizer.playLaserWarning()
                laser.copy(isFiring = true, fireDuration = 45)
            } else if (laser.fireDuration > 0) {
                // check collision with active fire laser line
                checkLaserTouch(laser)
                laser.copy(fireDuration = (laser.fireDuration - frameScale).toInt().coerceAtLeast(0))
            } else {
                // lasers complete trigger cycle
                null
            }
        }
        lasers.value = curLasers

        // 5. Update and drag collections orbs
        val magnetActive = magnetTimeLeft.value > 0f
        val curOrbs = orbs.value.mapNotNull { orb ->
            orb.life -= 0.003f * frameScale // decay lifes
            if (orb.life <= 0f) {
                null
            } else {
                if (magnetActive) {
                    val dM = playerPos.value - orb.position
                    val distM = Math.hypot(dM.x.toDouble(), dM.y.toDouble()).toFloat()
                    if (distM > 1f) {
                        val magnetStrength = 12f * frameScale
                        orb.position = Offset(
                            orb.position.x + (dM.x / distM) * magnetStrength,
                            orb.position.y + (dM.y / distM) * magnetStrength
                        )
                    }
                }
                orb
            }
        }
        orbs.value = curOrbs

        // 6. Spawn new collectibles periodically (halved the frequency and max active count)
        if (frameCount % 360 == 0 && orbs.value.size < 3) {
            spawnOrbsBatch()
        }

        // 7. Check Collisions & near misses
        evaluateCollisionsAndNearMisses()

        // 8. Decay Screenshake and clean animations
        if (screenShakeAmount.value > 0) {
            screenShakeAmount.value = (screenShakeAmount.value - 0.7f * frameScale).coerceAtLeast(0f)
        }

        // Update active particles list
        val activeParts = particles.value.mapNotNull { p ->
            p.curLife++
            if (p.curLife >= p.maxLife) {
                null
            } else {
                p.position = Offset(
                    p.position.x + p.velocity.x * frameScale,
                    p.position.y + p.velocity.y * frameScale
                )
                p
            }
        }
        particles.value = activeParts

        // Float notification tickers lists
        if (textPopups.value.isNotEmpty() && frameCount % 2 == 0) {
            textPopups.value = textPopups.value.mapNotNull { p ->
                val newPos = Offset(p.first.x, p.first.y - 1.5f * frameScale) // float skyward
                if (newPos.y < 0) null else Pair(newPos, p.second)
            }
        }
    }

    private fun spawnOrbsBatch() {
        val rand = Random.Default
        val num = 1
        val list = orbs.value.toMutableList()

        val profile = userProfile.value
        val dropLvl = profile?.getUpgradeLevel("rare_drop") ?: 0
        val shieldMagnChance = 0.1f + (dropLvl * 0.05f) // Upgraded drop odds list

        for (i in 1..num) {
            val angle = rand.nextFloat() * 2 * Math.PI
            val dist = rand.nextFloat() * (currentArenaRadius.value - 60f)
            val ox = (500f + Math.cos(angle) * dist).toFloat()
            val oy = (500f + Math.sin(angle) * dist).toFloat()

            // Random Type selective (Excluding Coins so players only earn coins by winning stages)
            val dice = rand.nextFloat()
            val type = when {
                dice < 0.25f -> OrbType.SHIELD
                dice < 0.50f -> OrbType.MAGNET
                dice < 0.75f -> OrbType.SLOW_MO
                else -> OrbType.SPEED_BOOST
            }

            list.add(
                Orb(
                    id = "orb_${UUID.randomUUID()}",
                    position = Offset(ox, oy),
                    type = type
                )
            )
        }
        orbs.value = list
    }

    private fun evaluateCollisionsAndNearMisses() {
        val now = System.currentTimeMillis()
        val pOffset = playerPos.value
        val isInvincible = shieldTimeLeft.value > 0f || ghostTimeLeft.value > 0f

        val obstacleList = enemies.value
        val orbsList = orbs.value

        // A. Analyze Near Misses first
        obstacleList.forEach { enemy ->
            val dist = Math.hypot(pOffset.x.toDouble() - enemy.position.x, pOffset.y.toDouble() - enemy.position.y).toFloat()
            val bufferThreshold = (playerRadius + enemy.radius) + 38f // very brushy! (~15-18 virtual units metric)
            val fatalThreshold = playerRadius + enemy.radius
            
            if (dist in fatalThreshold..bufferThreshold) {
                val lastTriggered = nearMissCoolDownMap[enemy.id] ?: 0L
                if (now - lastTriggered > 1500L) { // cool off per enemy target
                    nearMissCoolDownMap[enemy.id] = now
                    triggerNearMissMechanic(enemy.position)
                }
            }

            // B. Evaluate Death bounds
            if (dist < fatalThreshold) {
                if (isInvincible) {
                    // Trigger shock protection breakdown
                    if (shieldTimeLeft.value > 0f) {
                        shieldTimeLeft.value = 0f // consume shield
                        ghostTimeLeft.value = 1.0f // give brief fraction immunity
                        synthesizer.playFailure()
                        spawnVibrantBurst(pOffset, 0xFFE2E2E2.toInt())
                        screenShakeAmount.value = 12f
                    }
                } else {
                    // DEATH CYCLE!
                    triggerDeathSequence()
                }
            }
        }

        // C. Evaluate Orbs collection
        val collectedOrbIds = mutableListOf<String>()
        orbsList.forEach { orb ->
            val dist = Math.hypot(pOffset.x.toDouble() - orb.position.x, pOffset.y.toDouble() - orb.position.y).toFloat()
            val bounds = playerRadius + VirtualArena.ORB_RADIUS_DEFAULT
            if (dist < bounds) {
                collectedOrbIds.add(orb.id)
                triggerOrbCollection(orb)
            }
        }

        if (collectedOrbIds.isNotEmpty()) {
            orbs.value = orbs.value.filter { it.id !in collectedOrbIds }
        }
    }

    private fun triggerNearMissMechanic(hazardPos: Offset) {
        val pOffset = playerPos.value
        nearMisses.value++
        synthesizer.playTick()
        triggerSystemHaptic()
        screenShakeAmount.value = 6f
        
        // Generate glowing near miss float indicator popup
        val popupPos = Offset((pOffset.x + hazardPos.x) / 2f, (pOffset.y + hazardPos.y) / 2f - 20f)
        val textMultiplier = "جاخالی!"
        textPopups.value = textPopups.value + Pair(popupPos, textMultiplier)

        // spawn spark particles
        spawnParticles(popupPos, count = 3, color = 0xFFFF00CC.toInt())
        
        viewModelScope.launch {
            repository.progressAllQuestsOfObjective("near_miss", 1)
            repository.progressNearMissAchievement(nearMisses.value)
        }
    }

    private fun triggerOrbCollection(orb: Orb) {
        triggerSystemHaptic()

        val profile = userProfile.value
        val coinMultiLvl = profile?.getUpgradeLevel("coin_bonus") ?: 0

        val extraCoin = 1 + coinMultiLvl

        var colorHex = 0xFFFFD700.toInt() // default Gold

        when (orb.type) {
            OrbType.COIN -> {
                synthesizer.playCollectCoin()
                coinsCollected.value += 5 * extraCoin
                textPopups.value = textPopups.value + Pair(orb.position, "+${5 * extraCoin} سکه")
            }
            OrbType.XP -> {
                // XP حذف شده؛ این نوع گوی به سکه تبدیل می‌شود
                synthesizer.playCollectCoin()
                coinsCollected.value += 5 * extraCoin
                textPopups.value = textPopups.value + Pair(orb.position, "+${5 * extraCoin} سکه")
            }
            OrbType.SHIELD -> {
                synthesizer.playSavingAbility()
                val bonus = (profile?.getUpgradeLevel("shield_duration") ?: 0) * 1.0f
                shieldTimeLeft.value = 4.0f + bonus
                // بدون عبارت بالای صفحه (طبق درخواست)
                colorHex = 0xFF00D2FF.toInt()
            }
            OrbType.SPEED_BOOST -> {
                synthesizer.playSavingAbility()
                speedBoostTimeLeft.value = 5.0f
                colorHex = 0xFFAA00FF.toInt()
            }
            OrbType.SLOW_MO -> {
                synthesizer.playSavingAbility()
                val bonus = (profile?.getUpgradeLevel("slow_mo_duration") ?: 0) * 1.0f
                slowMoTimeLeft.value = 4.0f + bonus
                colorHex = 0xFFE2E2E2.toInt()
            }
            OrbType.MAGNET -> {
                synthesizer.playSavingAbility()
                magnetTimeLeft.value = 5.0f
                colorHex = 0xFFFFE600.toInt()
            }
        }

        // Spawn visual burst particles
        spawnVibrantBurst(orb.position, colorHex)
        orbsCollected.value++

        viewModelScope.launch {
            repository.progressAllQuestsOfObjective("collect_orbs", 1)
            repository.progressAllQuestsOfObjective("collect_coins", 5 * extraCoin)
        }
    }

    private fun checkBladeTouch(blade: RotatingBeam) {
        val pOffset = playerPos.value
        val isInvincible = shieldTimeLeft.value > 0f || ghostTimeLeft.value > 0f
        if (isInvincible) return

        // Rotating line equation from (500,500) with len and angle
        val rad = Math.toRadians(blade.angle.toDouble())
        val endX = 500f + Math.cos(rad).toFloat() * blade.length
        val endY = 500f + Math.sin(rad).toFloat() * blade.length

        // Distance from point to line segment
        val lx1 = 500f
        val ly1 = 500f
        val lx2 = endX
        val ly2 = endY

        val lineLenSq = (lx2 - lx1) * (lx2 - lx1) + (ly2 - ly1) * (ly2 - ly1)
        if (lineLenSq == 0f) return

        var t = ((pOffset.x - lx1) * (lx2 - lx1) + (pOffset.y - ly1) * (ly2 - ly1)) / lineLenSq
        t = t.coerceIn(0f, 1f)

        val projX = lx1 + t * (lx2 - lx1)
        val projY = ly1 + t * (ly2 - ly1)

        val distance = Math.hypot(pOffset.x.toDouble() - projX, pOffset.y.toDouble() - projY).toFloat()
        if (distance < playerRadius + blade.thickness / 2f) {
            triggerDeathSequence()
        }
    }

    private fun checkLaserTouch(laser: LaserHazard) {
        val pOffset = playerPos.value
        val isInvincible = shieldTimeLeft.value > 0f || ghostTimeLeft.value > 0f
        if (isInvincible) return

        if (laser.isVertical) {
            if (Math.abs(pOffset.x - laser.coord) < playerRadius + 8f) {
                triggerDeathSequence()
            }
        } else {
            if (Math.abs(pOffset.y - laser.coord) < playerRadius + 8f) {
                triggerDeathSequence()
            }
        }
    }

    private fun triggerDeathSequence() {
        gameLoopJob?.cancel()
        synthesizer.playFailure()
        triggerSystemHaptic()
        screenShakeAmount.value = 25f

        // Freeze and display stats
        viewModelScope.launch {
            delay(200) // freeze frame feeling
            saveHighScoreIfBetter(selectedLevel.value.levelNumber, survivalTime.value)
            
            // Push currency progress to local database permanently (XP حذف شده)
            val realCoinsGained = coinsCollected.value
            
            repository.incrementCoins(realCoinsGained)
            recentlyUnlockedRewards.value = emptyList()

            // Save achievements
            repository.progressSurvivalAchievement(survivalTime.value.toInt())

            navigateTo(TrialScreen.GameOver)
        }
    }

    private fun triggerVictoryCeremony() {
        gameLoopJob?.cancel()
        synthesizer.playSuccess()
        triggerSystemHaptic()
        
        viewModelScope.launch {
            saveHighScoreIfBetter(selectedLevel.value.levelNumber, survivalTime.value)

            val baseCoinsWin = 15 // bonus for complete

            // double with active multiplier and permanent stat levels updates
            val current = repository.getProfileDirect()
            val coinsUpgrade = current.getUpgradeLevel("coin_bonus")

            val totalCoins = (baseCoinsWin + coinsCollected.value) * (1 + coinsUpgrade)

            repository.incrementCoins(totalCoins)
            // میزان تمرکز بر اساس مرحله کامل‌شده بالا می‌رود (XP حذف شده)
            val focusUnlocks = repository.setFocusFromCompletedLevel(selectedLevel.value.levelNumber)
            recentlyUnlockedRewards.value = focusUnlocks

            // Progress db milestones
            repository.progressGameLevelAchievement(selectedLevel.value.levelNumber)
            repository.progressAllQuestsOfObjective("complete_level", 1)

            // Set screen
            navigateTo(TrialScreen.GameSuccess)
        }
    }

    // Particle Sparks Systems
    fun spawnParticles(pos: Offset, count: Int, color: Int) {
        val rand = Random.Default
        val list = particles.value.toMutableList()
        for (i in 0 until count) {
            val angle = rand.nextFloat() * 2 * Math.PI
            val speed = 1f + rand.nextFloat() * 3f
            list.add(
                Particle(
                    position = pos,
                    velocity = Offset(
                        (Math.cos(angle) * speed).toFloat(),
                        (Math.sin(angle) * speed).toFloat()
                    ),
                    color = color,
                    size = 5f + rand.nextFloat() * 8f,
                    maxLife = 20 + rand.nextInt(20),
                    curLife = 0
                )
            )
        }
        particles.value = list
    }

    private fun spawnVibrantBurst(pos: Offset, color: Int) {
        spawnParticles(pos, count = 15, color = color)
    }
}
