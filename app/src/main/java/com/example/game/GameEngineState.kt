package com.example.game

import androidx.compose.ui.geometry.Offset
import java.util.Random

// Game Screen boundary representation (Scale: 1000 x 1000 Virtual Coordinates)
object VirtualArena {
    const val SIZE = 1000f
    const val PLAYER_RADIUS_DEFAULT = 25f
    const val ENEMY_RADIUS_DEFAULT = 30f
    const val ORB_RADIUS_DEFAULT = 20f
}

enum class OrbType {
    COIN, XP, SHIELD, SPEED_BOOST, SLOW_MO, MAGNET
}

enum class ActiveModifierType {
    LASER_GRID,      // Horizontal/vertical moving sweeping hazards
    SHRINKING_ARENA, // Restricts boundary limits
    ROTATING_BLADE,   // Central rotating hazard bar
    FREEZING_ZONE,   // Swelling cold spots that damp control responsiveness
    GRAVITY_WELL,    // Pulls player core toward the center
    DARK_FLASHLIGHT, // Fog of war requiring precise flashlight positioning
    ASTEROID_RAIN    // Sudden falling meteor hazards from random angles
}

data class Orb(
    val id: String,
    var position: Offset,
    val type: OrbType,
    val value: Int = 10,
    var life: Float = 1.0f // fade out if not collected in 12s
)

data class Enemy(
    val id: String,
    var position: Offset,
    var velocity: Offset,
    val radius: Float = VirtualArena.ENEMY_RADIUS_DEFAULT,
    val colorType: Int = 0, // 0 = standard red, 1 = tracer dynamic yellow, 2 = pulsator orange
    var scale: Float = 1.0f,
    var pulseDir: Int = 1
)

data class Particle(
    var position: Offset,
    var velocity: Offset,
    val color: Int, // Int representation
    var size: Float,
    var maxLife: Int,
    var curLife: Int
)

data class RotatingBeam(
    var angle: Float = 0f,
    val speed: Float = 1.5f,
    val length: Float = 450f,
    val thickness: Float = 15f
)

data class LaserHazard(
    val isVertical: Boolean,
    var coord: Float, // current x or y coord
    var chargeTimer: Int = 90, // frames to flash danger lines
    var isFiring: Boolean = false,
    var fireDuration: Int = 60, // frames to state active lethal grid
    var travelSpeed: Float = 2.0f
)

data class DynamicLevelConfig(
    val levelNumber: Int,
    val title: String,
    val survivalTargetSecs: Int,
    val difficultyRating: String, // Tutorial, Easy, Medium, Hard, Extreme
    val initialEnemyCount: Int,
    val modifiersAllowed: List<ActiveModifierType>
) {
    companion object {
        fun generate(level: Int): DynamicLevelConfig {
            val title = when (level) {
                1 -> "اولین گام‌ها"
                2 -> "مبانی جاخالی"
                5 -> "دردسر دوگانه"
                10 -> "آزمون تمرکز"
                20 -> "شبکه سرعت"
                50 -> "شکاف زمانی"
                75 -> "هجوم خوشه‌ای"
                100 -> "نبرد نهایی"
                else -> "مرحله $level"
            }

            val rating = when (level) {
                in 1..10 -> "آموزشی"
                in 11..30 -> "آسان"
                in 31..60 -> "متوسط"
                in 61..85 -> "سخت"
                else -> "بسیار سخت"
            }

            val targetSec = when (level) {
                in 1..10 -> 10 + (level * 2) // 12s to 30s
                in 11..30 -> 15 + level // 26s to 45s
                in 31..60 -> 25 + level // 56s to 85s
                in 61..85 -> 30 + (level * 2) // 152s to 200s
                else -> 120 + (level - 85) * 5 // up to 195s
            }

            val enemyCount = when (level) {
                in 1..10 -> 2 + (level / 3)
                in 11..30 -> 4 + (level / 5)
                in 31..60 -> 7 + (level / 7)
                in 61..85 -> 10 + (level / 8)
                else -> 14 + (level / 10)
            }

            val mods = mutableListOf<ActiveModifierType>()
            if (level >= 5) mods.add(ActiveModifierType.LASER_GRID)
            if (level >= 15) mods.add(ActiveModifierType.FREEZING_ZONE)
            if (level >= 30) mods.add(ActiveModifierType.GRAVITY_WELL)
            if (level >= 45) mods.add(ActiveModifierType.ROTATING_BLADE)
            if (level >= 60) {
                mods.add(ActiveModifierType.SHRINKING_ARENA)
                mods.add(ActiveModifierType.DARK_FLASHLIGHT)
                mods.add(ActiveModifierType.ASTEROID_RAIN)
            }

            return DynamicLevelConfig(
                levelNumber = level,
                title = title,
                survivalTargetSecs = targetSec,
                difficultyRating = rating,
                initialEnemyCount = enemyCount.coerceAtMost(30),
                modifiersAllowed = mods
            )
        }
    }
}
