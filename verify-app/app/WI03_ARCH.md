# Clean Architecture 骨架代码

以下是完整的Clean Architecture骨架代码，基于Kotlin + Jetpack Compose + Hilt实现：

## 1. Application类

```kotlin
// app/src/main/java/com/mathknowledge/app/MathKnowledgeApp.kt
package com.mathknowledge.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MathKnowledgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化全局配置
    }
}
```

## 2. 依赖注入模块

```kotlin
// app/src/main/java/com/mathknowledge/app/di/AppModule.kt
package com.mathknowledge.app.di

import android.content.Context
import androidx.room.Room
import com.mathknowledge.app.data.local.MathKnowledgeDatabase
import com.mathknowledge.app.data.remote.MathKnowledgeApi
import com.mathknowledge.app.data.repository.MathKnowledgeRepositoryImpl
import com.mathknowledge.app.domain.repository.MathKnowledgeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Database
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MathKnowledgeDatabase {
        return Room.databaseBuilder(
            context,
            MathKnowledgeDatabase::class.java,
            "math_knowledge_db"
        ).build()
    }
    
    // API Service
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.mathknowledge.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideMathKnowledgeApi(retrofit: Retrofit): MathKnowledgeApi {
        return retrofit.create(MathKnowledgeApi::class.java)
    }
    
    // Repository
    @Provides
    @Singleton
    fun provideMathKnowledgeRepository(
        database: MathKnowledgeDatabase,
        api: MathKnowledgeApi
    ): MathKnowledgeRepository {
        return MathKnowledgeRepositoryImpl(database, api)
    }
}
```

## 3. Domain层

### 3.1 领域模型

```kotlin
// app/src/main/java/com/mathknowledge/app/domain/model/MathProblem.kt
package com.mathknowledge.app.domain.model

import java.util.Date

data class MathProblem(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val category: Category,
    val solution: String?,
    val createdAt: Date,
    val updatedAt: Date
)

enum class Difficulty {
    EASY, MEDIUM, HARD
}

enum class Category {
    ALGEBRA, GEOMETRY, CALCULUS, STATISTICS, TRIGONOMETRY
}
```

```kotlin
// app/src/main/java/com/mathknowledge/app/domain/model/User.kt
package com.mathknowledge.app.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val progress: UserProgress
)

data class UserProgress(
    val completedProblems: Int,
    val totalProblems: Int,
    val streakDays: Int,
    val level: Int
)
```

### 3.2 Repository接口

```kotlin
// app/src/main/java/com/mathknowledge/app/domain/repository/MathKnowledgeRepository.kt
package com.mathknowledge.app.domain.repository

import com.mathknowledge.app.domain.model.MathProblem
import com.mathknowledge.app.domain.model.User
import com.mathknowledge.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface MathKnowledgeRepository {
    // Math Problems
    fun getMathProblems(): Flow<Resource<List<MathProblem>>>
    fun getMathProblemById(id: String): Flow<Resource<MathProblem>>
    suspend fun saveMathProblem(problem: MathProblem): Resource<Unit>
    suspend fun deleteMathProblem(id: String): Resource<Unit>
    
    // User
    fun getUserProfile(): Flow<Resource<User>>
    suspend fun updateUserProgress(userId: String, progress: UserProgress): Resource<Unit>
    
    // Sync
    suspend fun syncData(): Resource<Unit>
}
```

### 3.3 用例基类

```kotlin
// app/src/main/java/com/mathknowledge/app/domain/usecase/BaseUseCase.kt
package com.mathknowledge.app.domain.usecase

import com.mathknowledge.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow

abstract class BaseUseCase<in Params, out Result> {
    abstract operator fun invoke(params: Params): Flow<Resource<Result>>
}

// 无参数用例基类
abstract class BaseNoParamUseCase<out Result> {
    abstract operator fun invoke(): Flow<Resource<Result>>
}
```

```kotlin
// app/src/main/java/com/mathknowledge/app/domain/usecase/GetMathProblemsUseCase.kt
package com.mathknowledge.app.domain.usecase

import com.mathknowledge.app.domain.model.MathProblem
import com.mathknowledge.app.domain.repository.MathKnowledgeRepository
import com.mathknowledge.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMathProblemsUseCase @Inject constructor(
    private val repository: MathKnowledgeRepository
) : BaseNoParamUseCase<List<MathProblem>>() {
    
    override fun invoke(): Flow<Resource<List<MathProblem>>> {
        return repository.getMathProblems()
    }
}
```

```kotlin
// app/src/main/java/com/mathknowledge/app/domain/usecase/GetMathProblemByIdUseCase.kt
package com.mathknowledge.app.domain.usecase

import com.mathknowledge.app.domain.model.MathProblem
import com.mathknowledge.app.domain.repository.MathKnowledgeRepository
import com.mathknowledge.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMathProblemByIdUseCase @Inject constructor(
    private val repository: MathKnowledgeRepository
) : BaseUseCase<String, MathProblem>() {
    
    override fun invoke(params: String): Flow<Resource<MathProblem>> {
        return repository.getMathProblemById(params)
    }
}
```

## 4. Data层

### 4.1 本地数据源

```kotlin
// app/src/main/java/com/mathknowledge/app/data/local/MathKnowledgeDatabase.kt
package com.mathknowledge.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mathknowledge.app.data.local.dao.MathProblemDao
import com.mathknowledge.app.data.local.dao.UserDao
import com.mathknowledge.app.data.local.entity.MathProblemEntity
import com.mathknowledge.app.data.local.entity.UserEntity
import com.mathknowledge.app.data.local.util.Converters

@Database(
    entities = [MathProblemEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MathKnowledgeDatabase : RoomDatabase() {
    abstract fun mathProblemDao(): MathProblemDao
    abstract fun userDao(): UserDao
}
```

```kotlin
// app/src/main/java/com/mathknowledge/app/data/local/entity/MathProblemEntity.kt
package com.mathknowledge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mathknowledge.app.domain.model.Category
import com.mathknowledge.app.domain.model.Difficulty
import java.util.Date

@Entity(tableName = "math_problems")
data class MathProblemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val category: Category,
    val solution: String?,
    val createdAt: Date,
    val updatedAt: Date
)
```

```kotlin
// app/src/main/java/com/mathknowledge/app/data/local/dao/MathProblemDao.kt
package com.mathknowledge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mathknowledge.app.data.local.entity.MathProblemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MathProblemDao {
    @Query("SELECT * FROM math_problems ORDER BY createdAt DESC")
    fun getAllMathProblems(): Flow<List<MathProblemEntity>>
    
    @Query("SELECT * FROM math_problems WHERE id = :id")
    fun getMathProblemById(id: String): Flow<MathProblemEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMathProblem(problem: MathProblemEntity)
    
    @Query("DELETE FROM math_problems WHERE id = :id")
    suspend fun deleteMathProblem(id: String)
}
```

```kotlin
// app/src/main/java/com/mathknowledge/app/data/local/util/Converters.kt
package com.mathknowledge.app.data.local.util

import androidx.room.TypeConverter
import com.mathknowledge.app.domain.model.Category
import com.mathknowledge.app.domain.model.Difficulty
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
    
    @TypeConverter
    fun fromDifficulty(value: Difficulty): String {
        return value.name
    }

    @TypeConverter
    fun toDifficulty(value: String): Difficulty {
        return Difficulty.valueOf(value)
    }
    
    @TypeConverter
    fun fromCategory(value: Category): String {
        return value.name
    }

    @TypeConverter
    fun toCategory(value: String): Category {
        return Category.valueOf(value)
    }
}
```

### 4.2 远程数据源

```kotlin
// app/src/main/java/com/mathknowledge/app/data/remote/MathKnowledgeApi.kt
package com.mathknowledge.app.data.remote

import com.mathknowledge.app.data.remote.dto.MathProblemDto
import com.mathknowledge.app.data.remote.dto.UserDto
import retrofit2.http.GET
import retrofit2.http.Path

interface MathKnowledgeApi {
    @GET("problems")
    suspend fun getMathProblems(): List<MathProblemDto>
    
    @GET("problems/{id}")
    suspend fun getMathProblemById(@Path("id") id: String): MathProblemDto
    
    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): UserDto
}
```

```kotlin
// app/src/main/java/com/mathknowledge/app/data/remote/dto/MathProblemDto.kt
package com.mathknowledge.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MathProblemDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("difficulty")
    val difficulty: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("solution")
    val solution: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
```

### 4.3 Repository实现

```kotlin
// app/src/main/java/com/mathknowledge/app/data/repository/MathKnowledgeRepositoryImpl.kt
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.MathKnowledgeDatabase
import com.mathknowledge.app.data.local.entity.MathProblemEntity
import com.mathknowledge.app.data.remote.MathKnowledgeApi
import com.mathknowledge.app.data.remote.dto.MathProblemDto
import com.mathknowledge.app.domain.model.MathProblem
import com.mathknowledge.app.domain.model.User
import com.mathknowledge.app.domain.model.UserProgress
import com.mathknowledge.app.domain.repository.MathKnowledgeRepository
import com.mathknowledge.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class MathKnowledgeRepositoryImpl @Inject constructor(
    private val database: MathKnowledgeDatabase,
    private val api: MathKnowledgeApi
) : MathKnowledgeRepository {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    
    override fun getMathProblems(): Flow<Resource<List<MathProblem>>> = flow {
        emit(Resource.Loading())
        
        try {
            // 先从本地获取
            val localProblems = database.mathProblemDao().getAllMathProblems()
            localProblems.collect { entities ->
                val problems = entities.map { it.toDomainModel() }
                emit(Resource.Success(problems))
            }
            
            // 然后从远程获取并更新本地
            val remoteProblems = api.getMathProblems()
            remoteProblems.forEach { dto ->
                database.mathProblemDao().insertMathProblem(dto.toEntity())
            }
            
            // 再次从本地获取更新后的数据
            database.mathProblemDao().getAllMathProblems().collect { entities ->
                val problems = entities.map { it.toDomainModel() }
                emit(Resource.Success(problems))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    override fun getMathProblemById(id: String): Flow<Resource<MathProblem>> = flow {
        emit(Resource.Loading())
        
        try {
            // 先从本地获取
            database.mathProblemDao().getMathProblemById(id).collect { entity ->
                entity?.let {
                    emit(Resource.Success(it.toDomainModel()))
                } ?: run {
                    // 本地没有则从远程获取
                    val remoteProblem = api.getMathProblemById(id)
                    database.mathProblemDao().insertMathProblem(remoteProblem.toEntity())
                    
                    database.mathProblemDao().getMathProblemById(id).collect { updatedEntity ->
                        updatedEntity?.let {
                            emit(Resource.Success(it.toDomainModel()))
                        } ?: emit(Resource.Error("Problem not found"))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun saveMathProblem(problem: MathProblem): Resource<Unit> {
        return try {
            database.mathProblemDao().insertMathProblem(problem.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save problem")
        }
    }
    
    override suspend fun deleteMathProblem(id: String): Resource<Unit> {
        return try {
            database.mathProblemDao().deleteMathProblem(id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete problem")
        }
    }
    
    override fun getUserProfile(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        // 实现用户资料获取逻辑
        emit(Resource.Error("Not implemented"))
    }
    
    override suspend fun updateUserProgress(userId: String, progress: UserProgress): Resource<Unit> {
        // 实现更新用户进度逻辑
        return Resource.Error("Not implemented")
    }
    
    override suspend fun syncData(): Resource<Unit> {
        // 实现数据同步逻辑
        return Resource.Success(Unit)
    }
    
    // 扩展函数：DTO转Entity
    private fun MathProblemDto.toEntity(): MathProblemEntity {
        return MathProblemEntity(
            id = id,
            title = title,
            description = description,
            difficulty = com.mathknowledge.app.domain.model.Difficulty.valueOf(difficulty),
            category = com.mathknowledge.app.domain.model.Category.valueOf(category),
            solution = solution,
            createdAt = dateFormat.parse(createdAt) ?: java.util.Date(),
            updatedAt = dateFormat.parse(updatedAt) ?: java.util.Date()
        )
    }
    
    // 扩展函数：Entity转Domain Model
    private fun MathProblemEntity.toDomainModel(): MathProblem {
        return MathProblem(
            id = id,
            title = title,
            description = description,
            difficulty = difficulty,
            category = category,
            solution = solution,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    // 扩展函数：Domain Model转Entity
    private fun MathProblem.toEntity(): MathProblemEntity {
        return MathProblemEntity(
            id = id,
            title = title,
            description = description,
            difficulty = difficulty,
            category = category,
            solution = solution,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
```

## 5. Presentation层

### 5.1 Navigation

```kotlin
// app/src/main/java/com/mathknowledge/app/presentation/navigation/Screen.kt
package com.mathknowledge.app.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ProblemList : Screen("problem_list")
    object ProblemDetail : Screen("problem_detail/{problemId}") {
        fun createRoute(problemId: String) = "problem_detail/$problemId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
```

```kotlin
// app/src/main/java/com/mathknowledge/app/presentation/navigation/NavGraph.kt
package com.mathknowledge.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mathknowledge.app.presentation.screens.home.HomeScreen
import com.mathknowledge.app.presentation.screens.problemlist.ProblemListScreen
import com.mathknowledge.app.presentation.screens.problemdetail.ProblemDetailScreen
import com.mathknowledge.app.presentation.screens.profile.ProfileScreen
import com.mathknowledge.app.presentation.screens.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProblemList = {
                    navController.navigate(Screen.ProblemList.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        
        composable(Screen.ProblemList.route) {
            ProblemListScreen(
                onNavigateToProblemDetail = { problemId ->
                    navController.navigate(Screen.ProblemDetail.createRoute(problemId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.ProblemDetail.route,
            arguments = listOf(
                navArgument("problemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getString("problemId") ?: ""
            ProblemDetailScreen(
                problemId = problemId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

### 5.2 Theme

```kotlin
// app/src/main/java/com/mathknowledge/app/presentation/theme/Color.kt
package com.mathknowledge.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Primary Colors
val PrimaryLight = Color(0xFF6200EE)
val PrimaryDark = Color(0xFFBB86FC)
val PrimaryVariant = Color(0xFF3700B3)

// Secondary Colors
val SecondaryLight = Color(0xFF03DAC6)
val SecondaryDark = Color(0xFF018786)
val SecondaryVariant = Color(0xFF018786)

// Background Colors
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF121212)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF121212)

// Error Colors
val ErrorLight = Color(0xFFB00020)
val ErrorDark = Color(0xFFCF6679)

// Text Colors
val TextPrimaryLight = Color(0xFF000000)
val TextSecondaryLight = Color(0xFF666666)
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFBBBBBB)

// Math-specific Colors
val MathBlue = Color(0xFF2196F3)
val MathGreen = Color(0xFF4CAF50)
val MathOrange = Color(0xFFFF9800)
val MathRed = Color(0xFFF44336)
```

```kotlin
// app/src/main/java/com/mathknowledge/app/presentation/theme/Type.kt
package com.mathknowledge.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

```kotlin
// app/src/main/java/com/mathknowledge/app/presentation/theme/Theme.kt
package com.mathknowledge.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = TextPrimaryLight,
    onSecondary = TextPrimaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = ErrorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextPrimaryDark,
    onSecondary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = ErrorDark
)

@Composable
fun MathKnowledgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content