由于我无法直接访问您的文件系统，我将基于常见的Android测试问题提供修复方案。以下是针对您提到的三个问题的修复代码：

## 1. 统一测试框架 - 使用JUnit4 + HiltAndroidRule

```kotlin
// 修复后的测试基类
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
abstract class BaseTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Before
    open fun setUp() {
        hiltRule.inject()
    }
}
```

## 2. 删除冗余TestRunner，统一使用HiltTestRunner

```kotlin
// 删除所有自定义TestRunner，只保留一个HiltTestRunner
// app/src/androidTest/java/.../HiltTestRunner.kt
import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
```

## 3. 修复ViewModel测试中的skipItems问题

```kotlin
// 修复后的ViewModel测试示例
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MyViewModelTest : BaseTest() {
    
    @Inject
    lateinit var viewModel: MyViewModel
    
    @Test
    fun `test state emission`() = runTest {
        // 使用更稳定的Turbine测试写法
        viewModel.uiState.test {
            // 等待初始状态
            val initialState = awaitItem()
            assertEquals(expectedInitialState, initialState)
            
            // 执行操作
            viewModel.loadData()
            
            // 等待加载状态
            val loadingState = awaitItem()
            assertEquals(expectedLoadingState, loadingState)
            
            // 等待最终状态
            val finalState = awaitItem()
            assertEquals(expectedFinalState, finalState)
            
            // 确保没有更多事件
            expectNoEvents()
        }
    }
    
    @Test
    fun `test flow with multiple emissions`() = runTest {
        viewModel.dataFlow.test {
            // 使用更精确的事件验证
            assertEquals("First emission", awaitItem())
            assertEquals("Second emission", awaitItem())
            assertEquals("Third emission", awaitItem())
            
            // 取消订阅并忽略剩余事件
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## 4. 更新build.gradle配置

```gradle
// app/build.gradle
android {
    defaultConfig {
        testInstrumentationRunner "com.example.app.HiltTestRunner"
    }
}

dependencies {
    // 测试依赖
    testImplementation "junit:junit:4.13.2"
    androidTestImplementation "androidx.test.ext:junit:1.1.5"
    androidTestImplementation "androidx.test:runner:1.5.2"
    androidTestImplementation "androidx.test:rules:1.5.0"
    androidTestImplementation "com.google.dagger:hilt-android-testing:2.48"
    androidTestImplementation "app.cash.turbine:turbine:1.0.0"
    kaptAndroidTest "com.google.dagger:hilt-android-compiler:2.48"
}
```

## 5. 完整的测试示例

```kotlin
// 完整的ViewModel测试示例
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UserViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Inject
    lateinit var viewModel: UserViewModel
    
    @Inject
    lateinit var repository: UserRepository
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun `loadUsers should emit loading then success state`() = runTest {
        // Given
        val users = listOf(
            User(id = 1, name = "John"),
            User(id = 2, name = "Jane")
        )
        whenever(repository.getUsers()).thenReturn(flowOf(users))
        
        // When & Then
        viewModel.uiState.test {
            // 等待初始状态
            val initialState = awaitItem()
            assertEquals(UserUiState.Loading, initialState)
            
            // 执行加载
            viewModel.loadUsers()
            
            // 等待成功状态
            val successState = awaitItem()
            assertTrue(successState is UserUiState.Success)
            assertEquals(users, (successState as UserUiState.Success).users)
            
            // 确保没有更多事件
            expectNoEvents()
        }
    }
    
    @Test
    fun `loadUsers should emit error state on failure`() = runTest {
        // Given
        whenever(repository.getUsers()).thenReturn(flow {
            throw RuntimeException("Network error")
        })
        
        // When & Then
        viewModel.uiState.test {
            // 等待初始状态
            awaitItem()
            
            // 执行加载
            viewModel.loadUsers()
            
            // 等待错误状态
            val errorState = awaitItem()
            assertTrue(errorState is UserUiState.Error)
            assertEquals("Network error", (errorState as UserUiState.Error).message)
            
            // 确保没有更多事件
            expectNoEvents()
        }
    }
}
```

## 修复要点总结：

1. **统一测试框架**：所有测试类使用JUnit4 + HiltAndroidRule，删除JUnit5依赖
2. **删除冗余TestRunner**：只保留一个HiltTestRunner，删除其他自定义TestRunner
3. **改进Turbine测试**：
   - 使用`test`块进行结构化测试
   - 使用`awaitItem()`逐个验证事件
   - 使用`expectNoEvents()`或`cancelAndIgnoreRemainingEvents()`替代不稳定的`skipItems()`
   - 在`runTest`中使用协程测试

这些修复将使测试更加稳定、可维护，并遵循Android测试的最佳实践。