# 修复后的Clean Architecture骨架代码

## 1. Resource类定义（问题6修复）

```kotlin
// data/Resource.kt
package com.example.mathapp.data

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
```

## 2. Repository实现（问题1、2、3、4修复）

```kotlin
// data/repository/ProblemRepositoryImpl.kt
package com.example.mathapp.data.repository

import com.example.mathapp.data.Resource
import com.example.mathapp.data.local.LocalDataSource
import com.example.mathapp.data.remote.RemoteDataSource
import com.example.mathapp.domain.model.Problem
import com.example.mathapp.domain.repository.ProblemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ProblemRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : ProblemRepository {

    override fun getProblems(): Flow<Resource<List<Problem>>> = flow {
        emit(Resource.Loading)
        
        try {
            // 修复问题1：使用first()获取初始值，避免无限阻塞
            val localProblems = localDataSource.getProblems().first()
            
            // 如果本地有数据，先返回本地数据
            if (localProblems.isNotEmpty()) {
                emit(Resource.Success(localProblems))
            }
            
            // 执行网络请求
            val remoteProblems = remoteDataSource.fetchProblems()
            
            // 更新本地数据
            localDataSource.insertProblems(remoteProblems)
            
            // 返回最新数据
            val updatedProblems = localDataSource.getProblems().first()
            emit(Resource.Success(updatedProblems))
            
        } catch (e: Exception) {
            // 网络请求失败时，尝试返回本地数据
            try {
                val localProblems = localDataSource.getProblems().first()
                if (localProblems.isNotEmpty()) {
                    emit(Resource.Success(localProblems))
                } else {
                    emit(Resource.Error(e.message ?: "Unknown error occurred", e))
                }
            } catch (localException: Exception) {
                emit(Resource.Error(e.message ?: "Unknown error occurred", e))
            }
        }
    }.flowOn(Dispatchers.IO) // 修复问题2：添加flowOn(Dispatchers.IO)
    
    override fun getProblemById(id: String): Flow<Resource<Problem>> = flow {
        emit(Resource.Loading)
        
        try {
            val problem = localDataSource.getProblemById(id)
            if (problem != null) {
                emit(Resource.Success(problem))
            } else {
                emit(Resource.Error("Problem not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred", e))
        }
    }.flowOn(Dispatchers.IO) // 修复问题2：添加flowOn(Dispatchers.IO)
}
```

## 3. DTO日期解析修复（问题3、4修复）

```kotlin
// data/remote/dto/ProblemDto.kt
package com.example.mathapp.data.remote.dto

import com.example.mathapp.domain.model.Problem
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ProblemDto(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomainModel(): Problem {
        return Problem(
            id = id,
            title = title,
            description = description,
            difficulty = difficulty,
            createdAt = parseDate(createdAt),
            updatedAt = parseDate(updatedAt)
        )
    }
    
    // 修复问题3和4：使用线程安全的DateTimeFormatter并增加容错处理
    private fun parseDate(dateString: String): Instant {
        return try {
            // 使用ISO_INSTANT格式解析，这是线程安全的
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse(dateString))
        } catch (e: DateTimeParseException) {
            // 解析失败时返回当前时间作为默认值
            Instant.now()
        } catch (e: Exception) {
            // 其他异常也返回当前时间
            Instant.now()
        }
    }
}
```

## 4. 本地数据源修复

```kotlin
// data/local/LocalDataSource.kt
package com.example.mathapp.data.local

import com.example.mathapp.domain.model.Problem
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {
    fun getProblems(): Flow<List<Problem>>
    suspend fun getProblemById(id: String): Problem?
    suspend fun insertProblems(problems: List<Problem>)
    suspend fun deleteAllProblems()
}
```

```kotlin
// data/local/LocalDataSourceImpl.kt
package com.example.mathapp.data.local

import com.example.mathapp.data.local.dao.ProblemDao
import com.example.mathapp.data.local.entity.ProblemEntity
import com.example.mathapp.domain.model.Problem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalDataSourceImpl(
    private val problemDao: ProblemDao
) : LocalDataSource {
    
    override fun getProblems(): Flow<List<Problem>> {
        return problemDao.getAllProblems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getProblemById(id: String): Problem? {
        return problemDao.getProblemById(id)?.toDomainModel()
    }
    
    override suspend fun insertProblems(problems: List<Problem>) {
        problemDao.insertAll(problems.map { ProblemEntity.fromDomainModel(it) })
    }
    
    override suspend fun deleteAllProblems() {
        problemDao.deleteAll()
    }
}
```

## 5. 远程数据源修复

```kotlin
// data/remote/RemoteDataSource.kt
package com.example.mathapp.data.remote

import com.example.mathapp.domain.model.Problem

interface RemoteDataSource {
    suspend fun fetchProblems(): List<Problem>
}
```

```kotlin
// data/remote/RemoteDataSourceImpl.kt
package com.example.mathapp.data.remote

import com.example.mathapp.data.remote.api.ProblemApi
import com.example.mathapp.domain.model.Problem

class RemoteDataSourceImpl(
    private val problemApi: ProblemApi
) : RemoteDataSource {
    
    override suspend fun fetchProblems(): List<Problem> {
        val response = problemApi.getProblems()
        return response.map { it.toDomainModel() }
    }
}
```

## 6. Domain层模型

```kotlin
// domain/model/Problem.kt
package com.example.mathapp.domain.model

import java.time.Instant

data class Problem(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## 7. Repository接口

```kotlin
// domain/repository/ProblemRepository.kt
package com.example.mathapp.domain.repository

import com.example.mathapp.data.Resource
import com.example.mathapp.domain.model.Problem
import kotlinx.coroutines.flow.Flow

interface ProblemRepository {
    fun getProblems(): Flow<Resource<List<Problem>>>
    fun getProblemById(id: String): Flow<Resource<Problem>>
}
```

## 8. UseCase示例

```kotlin
// domain/usecase/GetProblemsUseCase.kt
package com.example.mathapp.domain.usecase

import com.example.mathapp.data.Resource
import com.example.mathapp.domain.model.Problem
import com.example.mathapp.domain.repository.ProblemRepository
import kotlinx.coroutines.flow.Flow

class GetProblemsUseCase(
    private val repository: ProblemRepository
) {
    operator fun invoke(): Flow<Resource<List<Problem>>> {
        return repository.getProblems()
    }
}
```

## 9. ViewModel示例

```kotlin
// presentation/viewmodel/ProblemViewModel.kt
package com.example.mathapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mathapp.data.Resource
import com.example.mathapp.domain.model.Problem
import com.example.mathapp.domain.usecase.GetProblemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ProblemViewModel(
    private val getProblemsUseCase: GetProblemsUseCase
) : ViewModel() {
    
    private val _problems = MutableStateFlow<Resource<List<Problem>>>(Resource.Loading)
    val problems: StateFlow<Resource<List<Problem>>> = _problems.asStateFlow()
    
    init {
        getProblems()
    }
    
    private fun getProblems() {
        getProblemsUseCase().onEach { result ->
            _problems.value = result
        }.launchIn(viewModelScope)
    }
}
```

## 10. Theme.kt完整修复（问题5修复）

```kotlin
// presentation/theme/Theme.kt
package com.example.mathapp.presentation.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun MathAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalView.current.context
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
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
    )
}
```

## 11. 依赖注入模块（可选）

```kotlin
// di/AppModule.kt
package com.example.mathapp.di

import com.example.mathapp.data.local.LocalDataSource
import com.example.mathapp.data.local.LocalDataSourceImpl
import com.example.mathapp.data.remote.RemoteDataSource
import com.example.mathapp.data.remote.RemoteDataSourceImpl
import com.example.mathapp.data.repository.ProblemRepositoryImpl
import com.example.mathapp.domain.repository.ProblemRepository
import com.example.mathapp.domain.usecase.GetProblemsUseCase
import org.koin.dsl.module

val appModule = module {
    
    single<LocalDataSource> { LocalDataSourceImpl(get()) }
    single<RemoteDataSource> { RemoteDataSourceImpl(get()) }
    single<ProblemRepository> { ProblemRepositoryImpl(get(), get()) }
    
    factory { GetProblemsUseCase(get()) }
}
```

## 修复总结

1. **问题1修复**：使用`first()`替代`collect`，避免无限阻塞
2. **问题2修复**：所有Flow添加`.flowOn(Dispatchers.IO)`
3. **问题3修复**：使用`java.time.Instant`和`DateTimeFormatter`替代SimpleDateFormat
4. **问题4修复**：日期解析增加try-catch和默认值处理
5. **问题5修复**：确保Theme.kt完整闭合
6. **问题6修复**：添加完整的Resource泛型类定义

所有代码都遵循Clean Architecture原则，确保了：
- 关注点分离
- 可测试性
- 可维护性
- 线程安全
- 错误处理完善