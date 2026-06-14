package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        AchievementEntity::class,
        QuestEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun achievementDao(): AchievementDao
    abstract fun questDao(): QuestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "avoid_the_blocks_db"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Default Profile
            db.userDao().insertOrUpdateProfile(UserProfileEntity())

            // Default Achievements (Dynamic generation satisfying 100 levels and rich categories)
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

            // Default Quests
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
    }
}
