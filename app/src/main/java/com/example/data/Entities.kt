package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1, // Single profile
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 0,
    val stars: Int = 0,
    val focusPoints: Int = 0,
    val streak: Int = 1,
    val lastLoginTime: Long = 0,
    val equippedSkin: String = "neon_green",
    val unlockedSkins: String = "neon_green", // Comma-separated like "neon_green,neon_cyan"
    val equippedAbility: String = "shield", // shield, dash, time_slow, ghost, magnet
    val unlockedAbilities: String = "shield", // Comma-separated
    val upgradesString: String = "shield_duration:0,slow_mo_duration:0" // Permanent stats levels
) {
    // Helper to extract upgrade levels
    fun getUpgradeLevel(key: String): Int {
        return upgradesString.split(",")
            .map { it.split(":") }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)?.toIntOrNull() ?: 0
    }

    // Helper to calculate cost of next upgrade
    fun getUpgradeCost(key: String): Int {
        val currentLvl = getUpgradeLevel(key)
        if (currentLvl >= 3) return -1 // Maxed at 3 levels
        return when (currentLvl) {
            0 -> 300
            1 -> 700
            2 -> 1500
            else -> -1
        }
    }

    // Is a skin unlocked
    fun isSkinUnlocked(skinId: String): Boolean {
        return unlockedSkins.split(",").contains(skinId)
    }

    // Is an ability unlocked
    fun isAbilityUnlocked(abilityId: String): Boolean {
        return unlockedAbilities.split(",").contains(abilityId)
    }

    // XP required to level up
    fun xpToNextLevel(): Int {
        return level * 100 + 50
    }
}

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String, // Survival, Skill, Exploration, Consistency, Mastery
    val target: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val rewardCoins: Int,
    val rewardXp: Int
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val currentProgress: Int,
    val isCompleted: Boolean,
    val rewardCoins: Int,
    val rewardXp: Int,
    val questType: String // daily, weekly, special
)
