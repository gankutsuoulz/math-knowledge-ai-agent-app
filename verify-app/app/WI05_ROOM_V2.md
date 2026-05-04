# Room数据库代码修复

根据您提供的问题描述，我将修复Room数据库代码中的所有问题。以下是修复后的完整代码：

## 1. 修复后的实体类

```kotlin
// User.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

```kotlin
// MathProblem.kt
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "math_problems")
data class MathProblem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val problem: String,
    val answer: String,
    val difficulty: Int,
    val category: String,
    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

```kotlin
// KnowledgePoint.kt
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_points")
data class KnowledgePoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

```kotlin
// UserProgress.kt
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_progress",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MathProblem::class,
            parentColumns = ["id"],
            childColumns = ["problem_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["problem_id"]),
        Index(value = ["user_id", "problem_id"], unique = true)
    ]
)
data class UserProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "problem_id")
    val problemId: Long,
    @ColumnInfo(name = "time_spent")
    val timeSpent: Long,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 1,
    @ColumnInfo(name = "last_attempt")
    val lastAttempt: Long = System.currentTimeMillis()
)
```

## 2. 修复后的Converters类

```kotlin
// Converters.kt
import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    // 移除冗余的Long<->String转换方法
    // 原代码中的fromLong和toLong方法已被移除
}
```

## 3. 修复后的DAO接口

```kotlin
// UserProgressDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgress): Long
    
    @Update
    suspend fun updateProgress(progress: UserProgress)
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId")
    suspend fun getProgressByUserId(userId: Long): List<UserProgress>
    
    @Query("SELECT * FROM user_progress WHERE problem_id = :problemId")
    suspend fun getProgressByProblemId(problemId: Long): List<UserProgress>
    
    @Query("SELECT * FROM user_progress WHERE user_id = :userId AND problem_id = :problemId")
    suspend fun getProgressByUserAndProblem(userId: Long, problemId: Long): UserProgress?
    
    @Query("SELECT AVG(time_spent) FROM user_progress WHERE user_id = :userId")
    suspend fun getAverageTimeSpent(userId: Long): Double?  // 修复：返回Double而不是Float
    
    @Query("SELECT COUNT(*) FROM user_progress WHERE user_id = :userId AND is_correct = 1")
    suspend fun getCorrectAnswerCount(userId: Long): Int
    
    @Query("SELECT COUNT(*) FROM user_progress WHERE user_id = :userId")
    suspend fun getTotalAttemptCount(userId: Long): Int
    
    @Query("DELETE FROM user_progress WHERE user_id = :userId")
    suspend fun deleteProgressByUserId(userId: Long)
}
```

```kotlin
// MathProblemDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MathProblemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: MathProblem): Long
    
    @Update
    suspend fun updateProblem(problem: MathProblem)
    
    @Query("SELECT * FROM math_problems WHERE id = :problemId")
    suspend fun getProblemById(problemId: Long): MathProblem?
    
    @Query("SELECT * FROM math_problems WHERE category = :category")
    suspend fun getProblemsByCategory(category: String): List<MathProblem>
    
    @Query("SELECT * FROM math_problems WHERE difficulty = :difficulty")
    suspend fun getProblemsByDifficulty(difficulty: Int): List<MathProblem>
    
    @Query("SELECT * FROM math_problems WHERE is_favorite = 1")
    suspend fun getFavoriteProblems(): List<MathProblem>
    
    @Query("UPDATE math_problems SET is_favorite = :isFavorite WHERE id = :problemId")
    suspend fun updateFavoriteStatus(problemId: Long, isFavorite: Boolean)
    
    @Query("DELETE FROM math_problems WHERE id = :problemId")
    suspend fun deleteProblem(problemId: Long)
}
```

```kotlin
// KnowledgePointDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface KnowledgePointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoint(knowledgePoint: KnowledgePoint): Long
    
    @Update
    suspend fun updateKnowledgePoint(knowledgePoint: KnowledgePoint)
    
    @Query("SELECT * FROM knowledge_points WHERE id = :knowledgePointId")
    suspend fun getKnowledgePointById(knowledgePointId: Long): KnowledgePoint?
    
    @Query("SELECT * FROM knowledge_points WHERE category = :category")
    suspend fun getKnowledgePointsByCategory(category: String): List<KnowledgePoint>
    
    @Query("SELECT * FROM knowledge_points WHERE is_completed = 1")
    suspend fun getCompletedKnowledgePoints(): List<KnowledgePoint>
    
    @Query("UPDATE knowledge_points SET is_completed = :isCompleted WHERE id = :knowledgePointId")
    suspend fun updateCompletionStatus(knowledgePointId: Long, isCompleted: Boolean)
    
    @Query("DELETE FROM knowledge_points WHERE id = :knowledgePointId")
    suspend fun deleteKnowledgePoint(knowledgePointId: Long)
}
```

```kotlin
// UserDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?
    
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?
    
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)
}
```

## 4. 修复后的数据库类

```kotlin
// AppDatabase.kt
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        User::class,
        MathProblem::class,
        KnowledgePoint::class,
        UserProgress::class
    ],
    version = 3,  // 修复：版本号改为3
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun mathProblemDao(): MathProblemDao
    abstract fun knowledgePointDao(): KnowledgePointDao
    abstract fun userProgressDao(): UserProgressDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // 修复：添加完整的迁移定义
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加isFavorite字段到math_problems表
                database.execSQL(
                    "ALTER TABLE math_problems ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0"
                )
                
                // 添加isCompleted字段到knowledge_points表
                database.execSQL(
                    "ALTER TABLE knowledge_points ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加外键约束和索引到user_progress表
                // 注意：SQLite不支持ALTER TABLE添加外键，需要重建表
                
                // 1. 创建新表
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_progress_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "user_id INTEGER NOT NULL, " +
                    "problem_id INTEGER NOT NULL, " +
                    "time_spent INTEGER NOT NULL, " +
                    "is_correct INTEGER NOT NULL, " +
                    "attempt_count INTEGER NOT NULL DEFAULT 1, " +
                    "last_attempt INTEGER NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (problem_id) REFERENCES math_problems(id) ON DELETE CASCADE)"
                )
                
                // 2. 复制数据
                database.execSQL(
                    "INSERT INTO user_progress_new " +
                    "(id, user_id, problem_id, time_spent, is_correct, attempt_count, last_attempt) " +
                    "SELECT id, user_id, problem_id, time_spent, is_correct, attempt_count, last_attempt " +
                    "FROM user_progress"
                )
                
                // 3. 删除旧表
                database.execSQL("DROP TABLE user_progress")
                
                // 4. 重命名新表
                database.execSQL("ALTER TABLE user_progress_new RENAME TO user_progress")
                
                // 5. 创建索引
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_user_progress_user_id ON user_progress(user_id)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_user_progress_problem_id ON user_progress(problem_id)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_user_progress_user_id_problem_id ON user_progress(user_id, problem_id)"
                )
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "math_app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

## 5. 修复后的迁移测试

```kotlin
// MigrationTest.kt
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MigrationTest {
    private val TEST_DB_NAME = "test-migration-db"
    
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    
    @Before
    fun setup() {
        // 创建版本1的数据库
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            // 插入测试数据
            execSQL("INSERT INTO users (username, email) VALUES ('testuser', 'test@example.com')")
            execSQL("INSERT INTO math_problems (problem, answer, difficulty, category) VALUES ('1+1', '2', 1, 'basic')")
            execSQL("INSERT INTO knowledge_points (name, description, category) VALUES ('Addition', 'Basic addition', 'basic')")
            execSQL("INSERT INTO user_progress (user_id, problem_id, time_spent, is_correct) VALUES (1, 1, 5000, 1)")
            close()
        }
    }
    
    @Test
    fun testMigration1To2() {
        // 测试从版本1迁移到版本2
        helper.runMigrationsAndValidate(
            TEST_DB_NAME, 2, true, 
            AppDatabase.MIGRATION_1_2
        ).apply {
            // 验证isFavorite字段已添加
            val cursor = query("SELECT is_favorite FROM math_problems WHERE id = 1")
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0)) // 默认值为0
            
            // 验证isCompleted字段已添加
            val cursor2 = query("SELECT is_completed FROM knowledge_points WHERE id = 1")
            assertTrue(cursor2.moveToFirst())
            assertEquals(0, cursor2.getInt(0)) // 默认值为0
            
            close()
        }
    }
    
    @Test
    fun testMigration2To3() {
        // 先迁移到版本2
        helper.runMigrationsAndValidate(
            TEST_DB_NAME, 2, true, 
            AppDatabase.MIGRATION_1_2
        ).close()
        
        // 再迁移到版本3
        helper.runMigrationsAndValidate(
            TEST_DB_NAME, 3, true, 
            AppDatabase.MIGRATION_2_3
        ).apply {
            // 验证外键约束
            val cursor = query("PRAGMA foreign_key_list(user_progress)")
            assertTrue(cursor.moveToFirst())
            assertEquals("users", cursor.getString(2)) // 引用表
            assertEquals("id", cursor.getString(3)) // 引用列
            
            // 验证索引
            val indexCursor = query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='user_progress'")
            val indexNames = mutableListOf<String>()
            while (indexCursor.moveToNext()) {
                indexNames.add(indexCursor.getString(0))
            }
            assertTrue(indexNames.contains("index_user_progress_user_id"))
            assertTrue(indexNames.contains("index_user_progress_problem_id"))
            assertTrue(indexNames.contains("index_user_progress_user_id_problem_id"))
            
            close()
        }
    }
    
    @Test
    fun testMigrationWithLargeDataset() {
        // 创建版本1的数据库并插入大量数据
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            // 插入1000个用户
            for (i in 1..1000) {
                execSQL("INSERT INTO users (username, email) VALUES ('user$i', 'user$i@example.com')")
            }
            
            // 插入10000个数学问题
            for (i in 1..10000) {
                execSQL("INSERT INTO math_problems (problem, answer, difficulty, category) " +
                        "VALUES ('Problem $i', 'Answer $i', ${i % 5}, 'category${i % 10}')")
            }
            
            // 插入100000条用户进度记录
            for (i in 1..100000) {
                val userId = (i % 1000) + 1
                val problemId = (i % 10000) + 1
                execSQL("INSERT INTO user_progress (user_id, problem_id, time_spent, is_correct) " +
                        "VALUES ($userId, $problemId, ${i * 100}, ${if (i % 2 == 0) 1 else 0})")
            }
            
            close()
        }
        
        // 执行迁移
        helper.runMigrationsAndValidate(
            TEST_DB_NAME, 3, true, 
            AppDatabase.MIGRATION_1_2, 
            AppDatabase.MIGRATION_2_3
        ).apply {
            // 验证数据完整性
            val userCount = query("SELECT COUNT(*) FROM users")
            assertTrue(userCount.moveToFirst())
            assertEquals(1000, userCount.getInt(0))
            
            val problemCount = query("SELECT COUNT(*) FROM math_problems")
            assertTrue(problemCount.moveToFirst())
            assertEquals(10000, problemCount.getInt(0))
            
            val progressCount = query("SELECT COUNT(*) FROM user_progress")
            assertTrue(progressCount.moveToFirst())
            assertEquals(100000, progressCount.getInt(0))
            
            // 验证新字段的默认值
            val favoriteCount = query("SELECT COUNT(*) FROM math_problems WHERE is_favorite = 1")
            assertTrue(favoriteCount.moveToFirst())
            assertEquals(0, favoriteCount.getInt(0)) // 所有记录的is_favorite应为0
            
            val completedCount = query("SELECT COUNT(*) FROM knowledge_points WHERE is_completed = 1")
            assertTrue(completedCount.moveToFirst())
            assertEquals(0, completedCount.getInt(0)) // 所有记录的is_completed应为0
            
            // 验证外键约束
            val foreignKeyCheck = query("PRAGMA foreign_key_check")
            assertEquals(0, foreignKeyCheck.count) // 应该没有外键约束违反
            
            close()
        }
    }
}
```

## 修复总结

1. **数据库版本号修复**：将版本号从1改为3，与迁移定义匹配
2. **实体字段修复**：在MathProblem中添加了isFavorite字段，在KnowledgePoint中添加了isCompleted字段
3. **Converters修复**：移除了冗余的Long<->String转换方法
4. **迁移测试修复**：补全了testMigrationWithLargeDataset方法，添加了完整的测试逻辑
5. **外键约束与索引修复**：为UserProgress添加了外键约束和索引
6. **DAO返回类型修复**：将getAverageTimeSpent返回类型从Float?改为Double?

所有修复都遵循了Room数据库的最佳实践，确保了数据库的完整性和性能优化。