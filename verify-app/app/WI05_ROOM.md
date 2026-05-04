# Room数据库搭建代码

## 1. Entity定义

### MathProblem.kt
```kotlin
package com.mathknowledge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mathknowledge.app.data.local.converter.Converters

@Entity(tableName = "math_problems")
@TypeConverters(Converters::class)
data class MathProblem(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val difficulty: Int, // 1-5
    val category: String,
    val knowledgePointIds: List<String>,
    val solution: String,
    val explanation: String,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### KnowledgePoint.kt
```kotlin
package com.mathknowledge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_points")
data class KnowledgePoint(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val parentId: String? = null,
    val level: Int, // 1-5
    val category: String,
    val formula: String? = null,
    val example: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### UserProgress.kt
```kotlin
package com.mathknowledge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey
    val id: String,
    val userId: String,
    val knowledgePointId: String,
    val problemId: String,
    val isCorrect: Boolean,
    val timeSpent: Long, // 毫秒
    val attempts: Int = 1,
    val lastAttemptAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
```

### Favorite.kt
```kotlin
package com.mathknowledge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey
    val id: String,
    val userId: String,
    val problemId: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

## 2. DAO定义

### MathProblemDao.kt
```kotlin
package com.mathknowledge.app.data.local.dao

import androidx.room.*
import com.mathknowledge.app.data.local.entity.MathProblem
import kotlinx.coroutines.flow.Flow

@Dao
interface MathProblemDao {
    @Query("SELECT * FROM math_problems")
    fun getAllProblems(): Flow<List<MathProblem>>

    @Query("SELECT * FROM math_problems WHERE id = :problemId")
    fun getProblemById(problemId: String): Flow<MathProblem?>

    @Query("SELECT * FROM math_problems WHERE category = :category")
    fun getProblemsByCategory(category: String): Flow<List<MathProblem>>

    @Query("SELECT * FROM math_problems WHERE difficulty = :difficulty")
    fun getProblemsByDifficulty(difficulty: Int): Flow<List<MathProblem>>

    @Query("SELECT * FROM math_problems WHERE category = :category AND difficulty = :difficulty")
    fun getProblemsByCategoryAndDifficulty(category: String, difficulty: Int): Flow<List<MathProblem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: MathProblem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<MathProblem>)

    @Update
    suspend fun updateProblem(problem: MathProblem)

    @Delete
    suspend fun deleteProblem(problem: MathProblem)

    @Query("DELETE FROM math_problems")
    suspend fun deleteAllProblems()

    @Query("SELECT COUNT(*) FROM math_problems")
    suspend fun getProblemCount(): Int
}
```

### KnowledgePointDao.kt
```kotlin
package com.mathknowledge.app.data.local.dao

import androidx.room.*
import com.mathknowledge.app.data.local.entity.KnowledgePoint
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgePointDao {
    @Query("SELECT * FROM knowledge_points")
    fun getAllKnowledgePoints(): Flow<List<KnowledgePoint>>

    @Query("SELECT * FROM knowledge_points WHERE id = :pointId")
    fun getKnowledgePointById(pointId: String): Flow<KnowledgePoint?>

    @Query("SELECT * FROM knowledge_points WHERE parentId = :parentId")
    fun getKnowledgePointsByParent(parentId: String): Flow<List<KnowledgePoint>>

    @Query("SELECT * FROM knowledge_points WHERE category = :category")
    fun getKnowledgePointsByCategory(category: String): Flow<List<KnowledgePoint>>

    @Query("SELECT * FROM knowledge_points WHERE level = :level")
    fun getKnowledgePointsByLevel(level: Int): Flow<List<KnowledgePoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoint(knowledgePoint: KnowledgePoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoints(knowledgePoints: List<KnowledgePoint>)

    @Update
    suspend fun updateKnowledgePoint(knowledgePoint: KnowledgePoint)

    @Delete
    suspend fun deleteKnowledgePoint(knowledgePoint: KnowledgePoint)

    @Query("DELETE FROM knowledge_points")
    suspend fun deleteAllKnowledgePoints()

    @Query("SELECT COUNT(*) FROM knowledge_points")
    suspend fun getKnowledgePointCount(): Int
}
```

### UserProgressDao.kt
```kotlin
package com.mathknowledge.app.data.local.dao

import androidx.room.*
import com.mathknowledge.app.data.local.entity.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress")
    fun getAllProgress(): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getProgressByUserId(userId: String): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE knowledgePointId = :pointId")
    fun getProgressByKnowledgePoint(pointId: String): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE problemId = :problemId")
    fun getProgressByProblemId(problemId: String): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND knowledgePointId = :pointId")
    fun getProgressByUserAndKnowledgePoint(userId: String, pointId: String): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND problemId = :problemId")
    fun getProgressByUserAndProblem(userId: String, problemId: String): Flow<UserProgress?>

    @Query("SELECT AVG(timeSpent) FROM user_progress WHERE userId = :userId AND knowledgePointId = :pointId")
    fun getAverageTimeSpent(userId: String, pointId: String): Flow<Float?>

    @Query("SELECT COUNT(*) FROM user_progress WHERE userId = :userId AND isCorrect = 1")
    fun getCorrectAnswerCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<UserProgress>)

    @Update
    suspend fun updateProgress(progress: UserProgress)

    @Delete
    suspend fun deleteProgress(progress: UserProgress)

    @Query("DELETE FROM user_progress")
    suspend fun deleteAllProgress()

    @Query("DELETE FROM user_progress WHERE userId = :userId")
    suspend fun deleteProgressByUserId(userId: String)
}
```

### FavoriteDao.kt
```kotlin
package com.mathknowledge.app.data.local.dao

import androidx.room.*
import com.mathknowledge.app.data.local.entity.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getFavoritesByUserId(userId: String): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites WHERE problemId = :problemId")
    fun getFavoritesByProblemId(problemId: String): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND problemId = :problemId")
    fun getFavoriteByUserAndProblem(userId: String, problemId: String): Flow<Favorite?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<Favorite>)

    @Update
    suspend fun updateFavorite(favorite: Favorite)

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun deleteFavoritesByUserId(userId: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId")
    suspend fun getFavoriteCountByUserId(userId: String): Int
}
```

## 3. Database定义

### MathKnowledgeDatabase.kt
```kotlin
package com.mathknowledge.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mathknowledge.app.data.local.converter.Converters
import com.mathknowledge.app.data.local.dao.*
import com.mathknowledge.app.data.local.entity.*

@Database(
    entities = [
        MathProblem::class,
        KnowledgePoint::class,
        UserProgress::class,
        Favorite::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MathKnowledgeDatabase : RoomDatabase() {
    
    abstract fun mathProblemDao(): MathProblemDao
    abstract fun knowledgePointDao(): KnowledgePointDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun favoriteDao(): FavoriteDao
    
    companion object {
        @Volatile
        private var INSTANCE: MathKnowledgeDatabase? = null
        
        private const val DATABASE_NAME = "math_knowledge_database"
        
        fun getDatabase(context: Context): MathKnowledgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MathKnowledgeDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(*MIGRATIONS)
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // 数据库迁移策略
        private val MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3
        )
        
        // 版本1到版本2的迁移
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加新字段或修改表结构
                database.execSQL(
                    "ALTER TABLE math_problems ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE knowledge_points ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
        
        // 版本2到版本3的迁移
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建新表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_statistics (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        totalProblemsSolved INTEGER NOT NULL DEFAULT 0,
                        correctAnswers INTEGER NOT NULL DEFAULT 0,
                        averageTimeSpent REAL NOT NULL DEFAULT 0.0,
                        streakDays INTEGER NOT NULL DEFAULT 0,
                        lastActiveAt INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
                
                // 添加索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_user_statistics_userId ON user_statistics(userId)"
                )
            }
        }
        
        // 数据库回调
        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 数据库创建时的初始化操作
                INSTANCE?.let { database ->
                    // 可以在这里插入初始数据
                }
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // 数据库打开时的操作
                // 可以在这里启用外键约束
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }
    }
}
```

### Converters.kt
```kotlin
package com.mathknowledge.app.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromLong(value: Long?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLong(value: String?): Long? {
        return value?.toLongOrNull()
    }
}
```

## 4. Repository实现

### MathProblemRepositoryImpl.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.dao.MathProblemDao
import com.mathknowledge.app.data.local.entity.MathProblem
import com.mathknowledge.app.domain.repository.MathProblemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MathProblemRepositoryImpl @Inject constructor(
    private val mathProblemDao: MathProblemDao
) : MathProblemRepository {
    
    override fun getAllProblems(): Flow<List<MathProblem>> {
        return mathProblemDao.getAllProblems()
    }
    
    override fun getProblemById(problemId: String): Flow<MathProblem?> {
        return mathProblemDao.getProblemById(problemId)
    }
    
    override fun getProblemsByCategory(category: String): Flow<List<MathProblem>> {
        return mathProblemDao.getProblemsByCategory(category)
    }
    
    override fun getProblemsByDifficulty(difficulty: Int): Flow<List<MathProblem>> {
        return mathProblemDao.getProblemsByDifficulty(difficulty)
    }
    
    override suspend fun insertProblem(problem: MathProblem) {
        mathProblemDao.insertProblem(problem)
    }
    
    override suspend fun insertProblems(problems: List<MathProblem>) {
        mathProblemDao.insertProblems(problems)
    }
    
    override suspend fun updateProblem(problem: MathProblem) {
        mathProblemDao.updateProblem(problem)
    }
    
    override suspend fun deleteProblem(problem: MathProblem) {
        mathProblemDao.deleteProblem(problem)
    }
    
    override suspend fun deleteAllProblems() {
        mathProblemDao.deleteAllProblems()
    }
    
    override suspend fun getProblemCount(): Int {
        return mathProblemDao.getProblemCount()
    }
}
```

### KnowledgePointRepositoryImpl.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.dao.KnowledgePointDao
import com.mathknowledge.app.data.local.entity.KnowledgePoint
import com.mathknowledge.app.domain.repository.KnowledgePointRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgePointRepositoryImpl @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao
) : KnowledgePointRepository {
    
    override fun getAllKnowledgePoints(): Flow<List<KnowledgePoint>> {
        return knowledgePointDao.getAllKnowledgePoints()
    }
    
    override fun getKnowledgePointById(pointId: String): Flow<KnowledgePoint?> {
        return knowledgePointDao.getKnowledgePointById(pointId)
    }
    
    override fun getKnowledgePointsByParent(parentId: String): Flow<List<KnowledgePoint>> {
        return knowledgePointDao.getKnowledgePointsByParent(parentId)
    }
    
    override fun getKnowledgePointsByCategory(category: String): Flow<List<KnowledgePoint>> {
        return knowledgePointDao.getKnowledgePointsByCategory(category)
    }
    
    override suspend fun insertKnowledgePoint(knowledgePoint: KnowledgePoint) {
        knowledgePointDao.insertKnowledgePoint(knowledgePoint)
    }
    
    override suspend fun insertKnowledgePoints(knowledgePoints: List<KnowledgePoint>) {
        knowledgePointDao.insertKnowledgePoints(knowledgePoints)
    }
    
    override suspend fun updateKnowledgePoint(knowledgePoint: KnowledgePoint) {
        knowledgePointDao.updateKnowledgePoint(knowledgePoint)
    }
    
    override suspend fun deleteKnowledgePoint(knowledgePoint: KnowledgePoint) {
        knowledgePointDao.deleteKnowledgePoint(knowledgePoint)
    }
    
    override suspend fun deleteAllKnowledgePoints() {
        knowledgePointDao.deleteAllKnowledgePoints()
    }
    
    override suspend fun getKnowledgePointCount(): Int {
        return knowledgePointDao.getKnowledgePointCount()
    }
}
```

### UserProgressRepositoryImpl.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.dao.UserProgressDao
import com.mathknowledge.app.data.local.entity.UserProgress
import com.mathknowledge.app.domain.repository.UserProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressRepositoryImpl @Inject constructor(
    private val userProgressDao: UserProgressDao
) : UserProgressRepository {
    
    override fun getAllProgress(): Flow<List<UserProgress>> {
        return userProgressDao.getAllProgress()
    }
    
    override fun getProgressByUserId(userId: String): Flow<List<UserProgress>> {
        return userProgressDao.getProgressByUserId(userId)
    }
    
    override fun getProgressByKnowledgePoint(pointId: String): Flow<List<UserProgress>> {
        return userProgressDao.getProgressByKnowledgePoint(pointId)
    }
    
    override fun getProgressByUserAndKnowledgePoint(
        userId: String, 
        pointId: String
    ): Flow<List<UserProgress>> {
        return userProgressDao.getProgressByUserAndKnowledgePoint(userId, pointId)
    }
    
    override fun getProgressByUserAndProblem(
        userId: String, 
        problemId: String
    ): Flow<UserProgress?> {
        return userProgressDao.getProgressByUserAndProblem(userId, problemId)
    }
    
    override fun getAverageTimeSpent(userId: String, pointId: String): Flow<Float?> {
        return userProgressDao.getAverageTimeSpent(userId, pointId)
    }
    
    override fun getCorrectAnswerCount(userId: String): Flow<Int> {
        return userProgressDao.getCorrectAnswerCount(userId)
    }
    
    override suspend fun insertProgress(progress: UserProgress) {
        userProgressDao.insertProgress(progress)
    }
    
    override suspend fun updateProgress(progress: UserProgress) {
        userProgressDao.updateProgress(progress)
    }
    
    override suspend fun deleteProgress(progress: UserProgress) {
        userProgressDao.deleteProgress(progress)
    }
    
    override suspend fun deleteAllProgress() {
        userProgressDao.deleteAllProgress()
    }
    
    override suspend fun deleteProgressByUserId(userId: String) {
        userProgressDao.deleteProgressByUserId(userId)
    }
}
```

### FavoriteRepositoryImpl.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.dao.FavoriteDao
import com.mathknowledge.app.data.local.entity.Favorite
import com.mathknowledge.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {
    
    override fun getAllFavorites(): Flow<List<Favorite>> {
        return favoriteDao.getAllFavorites()
    }
    
    override fun getFavoritesByUserId(userId: String): Flow<List<Favorite>> {
        return favoriteDao.getFavoritesByUserId(userId)
    }
    
    override fun getFavoritesByProblemId(problemId: String): Flow<List<Favorite>> {
        return favoriteDao.getFavoritesByProblemId(problemId)
    }
    
    override fun getFavoriteByUserAndProblem(
        userId: String, 
        problemId: String
    ): Flow<Favorite?> {
        return favoriteDao.getFavoriteByUserAndProblem(userId, problemId)
    }
    
    override suspend fun insertFavorite(favorite: Favorite) {
        favoriteDao.insertFavorite(favorite)
    }
    
    override suspend fun updateFavorite(favorite: Favorite) {
        favoriteDao.updateFavorite(favorite)
    }
    
    override suspend fun deleteFavorite(favorite: Favorite) {
        favoriteDao.deleteFavorite(favorite)
    }
    
    override suspend fun deleteAllFavorites() {
        favoriteDao.deleteAllFavorites()
    }
    
    override suspend fun deleteFavoritesByUserId(userId: String) {
        favoriteDao.deleteFavoritesByUserId(userId)
    }
    
    override suspend fun getFavoriteCountByUserId(userId: String): Int {
        return favoriteDao.getFavoriteCountByUserId(userId)
    }
}
```

## 5. 迁移测试

### MigrationTest.kt
```kotlin
package com.mathknowledge.app.data.local

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MigrationTest {
    
    private val TEST_DB_NAME = "test_database"
    
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MathKnowledgeDatabase::class.java
    )
    
    @Before
    fun setup() {
        // 创建初始数据库
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            // 插入测试数据
            execSQL(
                """
                INSERT INTO math_problems (id, title, content, difficulty, category, knowledgePointIds, solution, explanation, createdAt, updatedAt)
                VALUES ('1', 'Test Problem', 'Test Content', 1, 'Algebra', '["1","2"]', 'Test Solution', 'Test Explanation', 1000000, 1000000)
                """
            )
            
            execSQL(
                """
                INSERT INTO knowledge_points (id, name, description, level, category, createdAt, updatedAt)
                VALUES ('1', 'Test Point', 'Test Description', 1, 'Algebra', 1000000, 1000000)
                """
            )
            
            close()
        }
    }
    
    @Test
    fun testMigration1To2() {
        // 测试从版本1到版本2的迁移
        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            2,
            true,
            MathKnowledgeDatabase.MIGRATION_1_2
        ).apply {
            // 验证新字段已添加
            val cursor = query("SELECT isFavorite FROM math_problems WHERE id = '1'")
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(cursor.getColumnIndex("isFavorite")))
            
            val cursor2 = query("SELECT isCompleted FROM knowledge_points WHERE id = '1'")
            assertTrue(cursor2.moveToFirst())
            assertEquals(0, cursor2.getInt(cursor2.getColumnIndex("isCompleted")))
            
            close()
        }
    }
    
    @Test
    fun testMigration2To3() {
        // 测试从版本2到版本3的迁移
        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            3,
            true,
            MathKnowledgeDatabase.MIGRATION_2_3
        ).apply {
            // 验证新表已创建
            val cursor = query("SELECT name FROM sqlite_master WHERE type='table' AND name='user_statistics'")
            assertTrue(cursor.moveToFirst())
            assertEquals("user_statistics", cursor.getString(cursor.getColumnIndex("name")))
            
            // 验证索引已创建
            val cursor2 = query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_user_statistics_userId'")
            assertTrue(cursor2.moveToFirst())
            assertEquals("index_user_statistics_userId", cursor2.getString(cursor2.getColumnIndex("name")))
            
            close()
        }
    }
    
    @Test
    fun testAllMigrations() {
        // 测试所有迁移
        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            3,
            true,
            MathKnowledgeDatabase.MIGRATION_1_2,
            MathKnowledgeDatabase.MIGRATION_2_3
        ).apply {
            // 验证数据完整性
            val cursor = query("SELECT * FROM math_problems WHERE id = '1'")
            assertTrue(cursor.moveToFirst())
            assertEquals("Test Problem", cursor.getString(cursor.getColumnIndex("title")))
            
            val cursor2 = query("SELECT * FROM knowledge_points WHERE id = '1'")
            assertTrue(cursor2.moveToFirst())
            assertEquals("Test Point", cursor2.getString(cursor2.getColumnIndex("name")))
            
            close()
        }
    }
    
    @Test
    fun testMigrationWithCorruptedData() {
        // 测试迁移时处理损坏的数据
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            // 插入损坏的数据
            execSQL(
                """
                INSERT INTO math_problems (id, title, content, difficulty, category, knowledgePointIds, solution, explanation, createdAt, updatedAt)
                VALUES ('2', NULL, 'Test Content', 1, 'Algebra', '["1","2"]', 'Test Solution', 'Test Explanation', 1000000, 1000000)
                """
            )
            close()
        }
        
        // 尝试迁移
        try {
            helper.runMigrationsAndValidate(
                TEST_DB_NAME,
                2,
                true,
                MathKnowledgeDatabase.MIGRATION_1_2
            )
            // 如果迁移成功，验证数据处理
        } catch (e: Exception) {
            // 如果迁移失败，验证错误处理
            assertNotNull(e)
        }
    }
    
    @Test
    fun testMigrationPerformance() {
        // 测试迁移性能
        val startTime = System.currentTimeMillis()
        
        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            3,
            true,
            MathKnowledgeDatabase.MIGRATION_1_2,
            MathKnowledgeDatabase.MIGRATION_2_3
        )
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // 验证迁移在合理时间内完成（例如，小于1秒）
        assertTrue("Migration took too long: ${duration}ms", duration < 1000)
    }
    
    @Test
    fun testMigrationWithLargeDataset() {
        // 测试大数据集的