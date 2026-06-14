package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(private val db: AppDatabase) {
    val userProfile: Flow<UserProfileEntity?> = db.userDao().getUserProfileFlow()
    val achievements: Flow<List<AchievementEntity>> = db.achievementDao().getAllAchievementsFlow()
    val quests: Flow<List<QuestEntity>> = db.questDao().getAllQuestsFlow()

    suspend fun getProfileDirect(): UserProfileEntity {
        // Ensure achievements and quests are never empty
        checkAndPrepopulateIfEmpty()

        var profile = db.userDao().getUserProfile()
        if (profile == null) {
            profile = UserProfileEntity()
            db.userDao().insertOrUpdateProfile(profile)
        }
        return profile
    }

    suspend fun checkAndPrepopulateIfEmpty() {
        try {
            val existingAchievements = db.achievementDao().getAllAchievements()
            if (existingAchievements.isEmpty()) {
                val initialAchievements = mutableListOf<AchievementEntity>()
                
                // Survival Achievements
                val survivalLevels = listOf(10, 20, 30, 45, 60, 90, 120, 150, 180, 240)
                survivalLevels.forEachIndexed { idx, sec ->
                    initialAchievements.add(
                        AchievementEntity(
                            id = "survival_$sec",
                            title = "دووم بیار! ($sec ثانیه)",
                            description = "توی یه راند کلاً $sec ثانیه دووم بیار و زنده بمون.",
                            category = "پایداری",
                            target = sec,
                            currentProgress = 0,
                            isUnlocked = false,
                            rewardCoins = 50 * (idx + 1),
                            rewardXp = 0
                        )
                    )
                }

                // Skill Achievements
                val nearMisses = listOf(3, 5, 10, 15, 20, 30)
                nearMisses.forEachIndexed { idx, count ->
                    initialAchievements.add(
                        AchievementEntity(
                            id = "near_miss_$count",
                            title = "استاد لایی‌کشی ($count بار)",
                            description = "توی یه راند $count بار خیلی مویی و لایی‌کشی تمیز از بغل موانع انجام بده.",
                            category = "مهارت",
                            target = count,
                            currentProgress = 0,
                            isUnlocked = false,
                            rewardCoins = 100 * (idx + 1),
                            rewardXp = 0
                        )
                    )
                }

                // Game Level Progression Achievements
                val levelTargets = listOf(5, 10, 15, 20, 30, 40, 50, 60, 75, 100)
                levelTargets.forEachIndexed { idx, lvl ->
                    initialAchievements.add(
                        AchievementEntity(
                            id = "game_level_$lvl",
                            title = "فتح مرحله $lvl",
                            description = "این مرحله مقتدرانه‌ی $lvl رو با موفقیت رد کن و نشون بده کی همه‌کاره‌ست!",
                            category = "استادی",
                            target = lvl,
                            currentProgress = 0,
                            isUnlocked = false,
                            rewardCoins = 80 * (idx + 1),
                            rewardXp = 0
                        )
                    )
                }

                // Player Profile Level Achievements
                val profileTargets = listOf(2, 5, 10, 15, 20, 30, 50, 75, 100)
                profileTargets.forEachIndexed { idx, lvl ->
                    initialAchievements.add(
                        AchievementEntity(
                            id = "profile_level_$lvl",
                            title = "تمرکز فوق‌العاده ($lvl٪)",
                            description = "درصد تمرکزت رو به $lvl٪ برسون تا بتونی حتی چشم‌بسته هم رکورد بزنی!",
                            category = "تداوم",
                            target = lvl,
                            currentProgress = 1,
                            isUnlocked = false,
                            rewardCoins = 100 * (idx + 1),
                            rewardXp = 0
                        )
                    )
                }

                // Coins Achievements
                val coinTargets = listOf(100, 500, 1000, 5000, 10000)
                coinTargets.forEachIndexed { idx, target ->
                    initialAchievements.add(
                        AchievementEntity(
                            id = "coins_$target",
                            title = "کیسه کردن سکه ($target سکه)",
                            description = "روی‌هم‌رفته $target تا سکه‌ی طلایی و تپل‌مپل از روی زمین بردار و جمع کن.",
                            category = "اکتشاف",
                            target = target,
                            currentProgress = 0,
                            isUnlocked = false,
                            rewardCoins = 50 * (idx + 1),
                            rewardXp = 0
                        )
                    )
                }

                db.achievementDao().insertAchievements(initialAchievements)
            }

            val existingQuests = db.questDao().getAllQuests()
            if (existingQuests.isEmpty()) {
                val initialQuests = listOf(
                    QuestEntity("q_survive_30", "پا پس نکش!", "توی هر مرحله‌ای که دلت می‌خواد، ۳۰ ثانیه کلاً دوام بیار و زنده بمون.", 30, 0, false, 50, 0, "روزانه"),
                    QuestEntity("q_coins_20", "سکه جمع کن جیبت پر شه!", "۲۰ تا گوی سکه و جایزه از وسط میدون بازی بردار.", 20, 0, false, 60, 0, "روزانه"),
                    QuestEntity("q_levels_3", "پادشاه میدان", "۳ تا مرحله رو با موفقیت کامل کن و ببر.", 3, 0, false, 80, 0, "روزانه"),
                    QuestEntity("q_near_miss_10", "مویی ردش کن!", "۱۰ بار خیلی مویی و نزدیک از کنار خطرها رد شو و جاخالی بده.", 10, 0, false, 120, 0, "هفتگی"),
                    QuestEntity("q_coins_100", "صندوق طلا", "توی چند بار بازی کردن در مجموع ۱۰۰ تا سکه خوشگل به جیب بزن.", 100, 0, false, 150, 0, "هفتگی"),
                    QuestEntity("q_survive_level_20", "غول بازی", "خودتو نشون بده و کلاً برس به مرحله ۲۰ بازی.", 20, 0, false, 200, 0, "ویژه")
                )
                db.questDao().insertQuests(initialQuests)
            }
        } catch (_: Exception) {}
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        db.userDao().insertOrUpdateProfile(profile)
    }

    suspend fun incrementCoins(amount: Int) {
        val current = getProfileDirect()
        val updated = current.copy(coins = current.coins + amount)
        updateProfile(updated)
        updateCoinsAchievements(updated.coins)
    }

    suspend fun incrementFocusPoints(amount: Int) {
        val current = getProfileDirect()
        val updated = current.copy(focusPoints = current.focusPoints + amount)
        updateProfile(updated)
    }

    // سیستم XP حذف شده است. «میزان تمرکز» بر اساس بالاترین مرحله کامل‌شده بالا می‌رود.
    // این تابع دیگر XP نمی‌دهد و سطح را تغییر نمی‌دهد (برای حفظ سازگاری نگه داشته شده).
    suspend fun addXp(amount: Int): List<String> {
        return emptyList()
    }

    // میزان تمرکز را بر اساس شماره مرحله کامل‌شده تنظیم می‌کند (هر مرحله ۱٪ تا سقف ۱۰۰٪)
    suspend fun setFocusFromCompletedLevel(stageNumber: Int): List<String> {
        val unlocks = mutableListOf<String>()
        val current = getProfileDirect()
        val newFocus = maxOf(current.level, stageNumber).coerceAtMost(100)
        if (newFocus > current.level) {
            unlocks.add("میزان تمرکز تو به $newFocus٪ رسید!")
            updateProfile(current.copy(level = newFocus))
            updateProfileLevelAchievements(newFocus)
        }
        return unlocks
    }

    // Progress achievements
    suspend fun progressSurvivalAchievement(timeInSec: Int) {
        val activeAchievements = db.achievementDao().getAllAchievements()
        for (ach in activeAchievements) {
            if (ach.category == "پایداری" && !ach.isUnlocked) {
                if (timeInSec >= ach.target) {
                    val updated = ach.copy(currentProgress = ach.target, isUnlocked = true)
                    db.achievementDao().updateAchievement(updated)
                    // Reward user
                    incrementCoins(ach.rewardCoins)
                    addXp(ach.rewardXp)
                } else if (timeInSec > ach.currentProgress) {
                    db.achievementDao().updateAchievement(ach.copy(currentProgress = timeInSec))
                }
            }
        }
    }

    suspend fun progressNearMissAchievement(totalInRun: Int) {
        val activeAchievements = db.achievementDao().getAllAchievements()
        for (ach in activeAchievements) {
            if (ach.id.startsWith("near_miss_") && !ach.isUnlocked) {
                if (totalInRun >= ach.target) {
                    val updated = ach.copy(currentProgress = ach.target, isUnlocked = true)
                    db.achievementDao().updateAchievement(updated)
                    incrementCoins(ach.rewardCoins)
                    addXp(ach.rewardXp)
                } else if (totalInRun > ach.currentProgress) {
                    db.achievementDao().updateAchievement(ach.copy(currentProgress = totalInRun))
                }
            }
        }
    }

    suspend fun progressGameLevelAchievement(reachedLevel: Int) {
        val activeAchievements = db.achievementDao().getAllAchievements()
        for (ach in activeAchievements) {
            if (ach.id.startsWith("game_level_") && !ach.isUnlocked) {
                if (reachedLevel >= ach.target) {
                    val updated = ach.copy(currentProgress = ach.target, isUnlocked = true)
                    db.achievementDao().updateAchievement(updated)
                    incrementCoins(ach.rewardCoins)
                    addXp(ach.rewardXp)
                } else if (reachedLevel > ach.currentProgress) {
                    db.achievementDao().updateAchievement(ach.copy(currentProgress = reachedLevel))
                }
            }
        }
    }

    private suspend fun updateProfileLevelAchievements(reachedLevel: Int) {
        val activeAchievements = db.achievementDao().getAllAchievements()
        for (ach in activeAchievements) {
            if (ach.id.startsWith("profile_level_") && !ach.isUnlocked) {
                if (reachedLevel >= ach.target) {
                    val updated = ach.copy(currentProgress = ach.target, isUnlocked = true)
                    db.achievementDao().updateAchievement(updated)
                    incrementCoins(ach.rewardCoins)
                    addXp(ach.rewardXp)
                } else if (reachedLevel > ach.currentProgress) {
                    db.achievementDao().updateAchievement(ach.copy(currentProgress = reachedLevel))
                }
            }
        }
    }

    private suspend fun updateCoinsAchievements(totalCoinsOwned: Int) {
        val activeAchievements = db.achievementDao().getAllAchievements()
        for (ach in activeAchievements) {
            if (ach.id.startsWith("coins_") && !ach.isUnlocked) {
                if (totalCoinsOwned >= ach.target) {
                    val updated = ach.copy(currentProgress = ach.target, isUnlocked = true)
                    db.achievementDao().updateAchievement(updated)
                    incrementCoins(ach.rewardCoins)
                    addXp(ach.rewardXp)
                } else if (totalCoinsOwned > ach.currentProgress) {
                    db.achievementDao().updateAchievement(ach.copy(currentProgress = totalCoinsOwned))
                }
            }
        }
    }

    // Quest progression
    suspend fun progressQuest(questId: String, increment: Int) {
        val quest = db.questDao().getQuestById(questId) ?: return
        if (quest.isCompleted) return

        val newProgress = (quest.currentProgress + increment).coerceAtMost(quest.target)
        val isCompletedNow = newProgress >= quest.target
        val updated = quest.copy(currentProgress = newProgress, isCompleted = isCompletedNow)
        db.questDao().updateQuest(updated)

        if (isCompletedNow) {
            incrementCoins(quest.rewardCoins)
            addXp(quest.rewardXp)
        }
    }

    // Dynamic quest progress for generic objectives (e.g. survival, collecting)
    suspend fun progressAllQuestsOfObjective(objectivePrefix: String, value: Int) {
        val activeQuests = db.questDao().getAllQuests()
        for (q in activeQuests) {
            if (!q.isCompleted) {
                val matches = when (q.id) {
                    "q_survive_30" -> objectivePrefix == "survive" && value >= 30
                    "q_coins_20" -> objectivePrefix == "collect_orbs"
                    "q_levels_3" -> objectivePrefix == "complete_level"
                    "q_near_miss_10" -> objectivePrefix == "near_miss"
                    "q_coins_100" -> objectivePrefix == "collect_coins"
                    "q_survive_level_20" -> objectivePrefix == "game_level" && value >= 20
                    else -> false
                }
                if (matches) {
                    val step = if (objectivePrefix == "survive" || objectivePrefix == "game_level") value else 1
                    progressQuest(q.id, step)
                }
            }
        }
    }

    // Handles progression streak and daily login
    suspend fun checkDailyStreak(): Pair<Int, Boolean> {
        val current = getProfileDirect()
        val now = System.currentTimeMillis()
        val lastLogin = current.lastLoginTime
        
        if (lastLogin == 0L) {
            // First time login ever
            val updated = current.copy(lastLoginTime = now, streak = 1)
            updateProfile(updated)
            return Pair(1, true)
        }

        val diffMs = now - lastLogin
        val oneDayMs = 24 * 60 * 60 * 1000L
        val twoDaysMs = 48 * 60 * 60 * 1000L

        return when {
            diffMs < oneDayMs -> {
                // Already logged in within 24hr, don't break streak or double-grant
                Pair(current.streak, false)
            }
            diffMs < twoDaysMs -> {
                // Next calendar day login, advance streak
                var newStreak = current.streak + 1
                if (newStreak > 7) newStreak = 1 // recycle daily login 7 days carousel
                
                // Unlocks exclusive cosmetic on Day 7
                var skinsStr = current.unlockedSkins
                if (newStreak == 7 && !current.isSkinUnlocked("royal_gold")) {
                    skinsStr += ",royal_gold"
                }

                val updated = current.copy(lastLoginTime = now, streak = newStreak, unlockedSkins = skinsStr)
                updateProfile(updated)
                Pair(newStreak, true)
            }
            else -> {
                // Streak broken, reset to Day 1
                val updated = current.copy(lastLoginTime = now, streak = 1)
                updateProfile(updated)
                Pair(1, true)
            }
        }
    }
}
