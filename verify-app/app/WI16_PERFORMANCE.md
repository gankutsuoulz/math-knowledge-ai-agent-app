# 性能优化模块完整代码

## 项目结构

```
com.mathknowledge.app/
├── performance/
│   ├── startup/
│   │   ├── AppStartupManager.kt
│   │   ├── StartupTask.kt
│   │   ├── SplashScreenActivity.kt
│   │   └── PreloadManager.kt
│   ├── memory/
│   │   ├── ImageCacheManager.kt
│   │   ├── ComposeOptimization.kt
│   │   └── MemoryLeakDetector.kt
│   ├── frame/
│   │   ├── LazyColumnOptimizer.kt
│   │   ├── AnimationOptimizer.kt
│   │   └── FrameDropDetector.kt
│   ├── monitor/
│   │   ├── PerformanceMonitor.kt
│   │   ├── StartupTracer.kt
│   │   └── MemoryMonitor.kt
│   └── ui/
│       ├── OptimizedLazyColumn.kt
│       ├── OptimizedImage.kt
│       └── PerformanceOverlay.kt
```

---

## 1. 启动速度优化

### StartupTask.kt — 启动任务定义

```kotlin
package com.mathknowledge.app.performance.startup

import android.content.Context
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 启动任务抽象基类
 * 每个任务可声明依赖关系、是否异步执行、是否需要等待完成
 */
abstract class StartupTask(
    val taskName: String,
    val dependencies: List<String> = emptyList(),
    val runOnMainThread: Boolean = false,
    val waitUntilComplete: Boolean = true
) {
    var isCompleted = false
        private set

    var startTime = 0L
        private set

    var endTime = 0L
        private set

    val durationMs: Long
        get() = if (endTime > 0 && startTime > 0) endTime - startTime else 0

    /**
     * 执行任务，子类实现具体逻辑
     */
    abstract suspend fun execute(context: Context)

    /**
     * 标记任务完成
     */
    fun markCompleted() {
        endTime = System.nanoTime()
        isCompleted = true
    }

    /**
     * 标记任务开始
     */
    fun markStarted() {
        startTime = System.nanoTime()
    }
}

/**
 * 具体启动任务示例：初始化日志
 */
class InitLogTask : StartupTask(
    taskName = "InitLog",
    runOnMainThread = false,
    waitUntilComplete = false
) {
    override suspend fun execute(context: Context) {
        // 初始化日志框架
        // Timber.plant(Timber.DebugTree())
        delay(50) // 模拟初始化耗时
    }
}

/**
 * 具体启动任务示例：初始化网络
 */
class InitNetworkTask : StartupTask(
    taskName = "InitNetwork",
    dependencies = listOf("InitLog"),
    runOnMainThread = false,
    waitUntilComplete = true
) {
    override suspend fun execute(context: Context) {
        // 初始化OkHttp/Retrofit
        delay(100)
    }
}

/**
 * 具体启动任务示例：初始化数据库
 */
class InitDatabaseTask : StartupTask(
    taskName = "InitDatabase",
    dependencies = listOf("InitLog"),
    runOnMainThread = false,
    waitUntilComplete = true
) {
    override suspend fun execute(context: Context) {
        // Room数据库初始化
        delay(150)
    }
}

/**
 * 具体启动任务示例：预加载用户数据
 */
class PreloadUserDataTask : StartupTask(
    taskName = "PreloadUserData",
    dependencies = listOf("InitNetwork", "InitDatabase"),
    runOnMainThread = false,
    waitUntilComplete = false
) {
    override suspend fun execute(context: Context) {
        // 预加载用户数据到内存
        delay(200)
    }
}
```



### AppStartupManager.kt — 启动任务调度器

```kotlin
package com.mathknowledge.app.performance.startup

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 启动任务调度管理器
 *
 * 特性：
 * 1. 拓扑排序自动解析依赖关系
 * 2. 支持主线程/子线程任务
 * 3. 支持阻塞式/非阻塞式任务
 * 4. 超时保护机制
 * 5. 详细的性能日志
 */
class AppStartupManager private constructor() {

    companion object {
        private const val TAG = "StartupManager"
        private const val DEFAULT_TIMEOUT_MS = 5000L

        @Volatile
        private var instance: AppStartupManager? = null

        fun getInstance(): AppStartupManager {
            return instance ?: synchronized(this) {
                instance ?: AppStartupManager().also { instance = it }
            }
        }
    }

    private val tasks = mutableListOf<StartupTask>()
    private val completedTasks = ConcurrentHashMap<String, Boolean>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioDispatcher = Dispatchers.IO
    private val mainDispatcher = Dispatchers.Main

    private val isRunning = AtomicBoolean(false)
    private var startupStartTime = 0L

    /**
     * 注册启动任务
     */
    fun register(task: StartupTask): AppStartupManager {
        tasks.add(task)
        return this
    }

    /**
     * 批量注册启动任务
     */
    fun registerAll(taskList: List<StartupTask>): AppStartupManager {
        tasks.addAll(taskList)
        return this
    }

    /**
     * 执行所有启动任务（拓扑排序 + 并行执行）
     */
    suspend fun execute(context: Context) {
        if (isRunning.getAndSet(true)) {
            Log.w(TAG, "Startup tasks already running")
            return
        }

        startupStartTime = System.nanoTime()
        Log.d(TAG, "========== Startup Tasks Begin ==========")

        val sortedTasks = topologicalSort()
        val mainThreadTasks = sortedTasks.filter { it.runOnMainThread }
        val backgroundTasks = sortedTasks.filter { !it.runOnMainThread }

        // 分组执行：后台任务并行，主线程任务串行
        coroutineScope {
            // 后台任务并行执行
            val backgroundJob = launch(ioDispatcher) {
                executeBackgroundTasks(context, backgroundTasks)
            }

            // 主线程任务串行执行
            val mainJob = launch(mainDispatcher) {
                executeMainThreadTasks(context, mainThreadTasks)
            }

            // 等待所有需要等待的任务完成
            backgroundJob.join()
            mainJob.join()
        }

        val totalDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupStartTime)
        Log.d(TAG, "========== Startup Tasks Completed in ${totalDuration}ms ==========")
        printTaskReport()

        isRunning.set(false)
    }

    /**
     * 执行后台任务（按依赖关系分层并行）
     */
    private suspend fun executeBackgroundTasks(context: Context, taskList: List<StartupTask>) {
        val layers = buildLayers(taskList)

        for (layer in layers) {
            coroutineScope {
                layer.map { task ->
                    async(ioDispatcher) {
                        executeSingleTask(context, task)
                    }
                }.awaitAll()
            }
        }
    }

    /**
     * 执行主线程任务（串行）
     */
    private suspend fun executeMainThreadTasks(context: Context, taskList: List<StartupTask>) {
        val layers = buildLayers(taskList)

        for (layer in layers) {
            for (task in layer) {
                executeSingleTask(context, task)
            }
        }
    }

    /**
     * 执行单个任务（带超时保护）
     */
    private suspend fun executeSingleTask(context: Context, task: StartupTask) {
        // 检查依赖是否全部完成
        task.dependencies.forEach { dep ->
            if (!completedTasks.containsKey(dep)) {
                Log.e(TAG, "Task [${task.taskName}] dependency [$dep] not completed!")
                return
            }
        }

        task.markStarted()
        Log.d(TAG, "▶ Task [${task.taskName}] started")

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                task.execute(context)
            }
            task.markCompleted()
            completedTasks[task.taskName] = true
            Log.d(TAG, "✓ Task [${task.taskName}] completed in ${task.durationMs}ms")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "✗ Task [${task.taskName}] timed out after ${DEFAULT_TIMEOUT_MS}ms")
            task.markCompleted() // 标记完成避免阻塞后续任务
            completedTasks[task.taskName] = true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Task [${task.taskName}] failed: ${e.message}")
            task.markCompleted()
            completedTasks[task.taskName] = true
        }
    }

    /**
     * 拓扑排序（Kahn算法）
     */
    private fun topologicalSort(): List<StartupTask> {
        val taskMap = tasks.associateBy { it.taskName }
        val inDegree = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        // 初始化入度和邻接表
        tasks.forEach { task ->
            inDegree[task.taskName] = 0
            adjacency[task.taskName] = mutableListOf()
        }

        tasks.forEach { task ->
            task.dependencies.forEach { dep ->
                adjacency[dep]?.add(task.taskName)
                inDegree[task.taskName] = (inDegree[task.taskName] ?: 0) + 1
            }
        }

        // BFS拓扑排序
        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }.forEach { (name, _) ->
            queue.addLast(name)
        }

        val sorted = mutableListOf<StartupTask>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            taskMap[current]?.let { sorted.add(it) }

            adjacency[current]?.forEach { neighbor ->
                inDegree[neighbor] = (inDegree[neighbor] ?: 1) - 1
                if (inDegree[neighbor] == 0) {
                    queue.addLast(neighbor)
                }
            }
        }

        if (sorted.size != tasks.size) {
            Log.e(TAG, "Circular dependency detected! Sorted: ${sorted.size}, Total: ${tasks.size}")
        }

        return sorted
    }

    /**
     * 将任务列表按依赖层级分组
     */
    private fun buildLayers(taskList: List<StartupTask>): List<List<StartupTask>> {
        val taskNames = taskList.map { it.taskName }.toSet()
        val layers = mutableListOf<MutableList<StartupTask>>()
        val completed = mutableSetOf<String>()
        val remaining = taskList.toMutableList()

        while (remaining.isNotEmpty()) {
            val layer = remaining.filter { task ->
                task.dependencies.all { it in completed || it !in taskNames }
            }

            if (layer.isEmpty()) {
                Log.e(TAG, "Unresolvable dependencies detected")
                break
            }

            layers.add(layer.toMutableList())
            layer.forEach { completed.add(it.taskName) }
            remaining.removeAll(layer)
        }

        return layers
    }

    /**
     * 打印任务执行报告
     */
    private fun printTaskReport() {
        val sortedByDuration = tasks.sortedByDescending { it.durationMs }
        Log.d(TAG, "┌─────────────────────────────────────────────┐")
        Log.d(TAG, "│          Startup Task Performance Report     │")
        Log.d(TAG, "├──────────────────┬──────────┬───────────────┤")
        Log.d(TAG, "│ Task Name        │ Duration │ Thread        │")
        Log.d(TAG, "├──────────────────┼──────────┼───────────────┤")

        sortedByDuration.forEach { task ->
            val name = task.taskName.padEnd(16)
            val duration = "${task.durationMs}ms".padEnd(8)
            val thread = if (task.runOnMainThread) "Main" else "IO"
            Log.d(TAG, "│ $name │ $duration │ ${thread.padEnd(13)} │")
        }

        val totalDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupStartTime)
        Log.d(TAG, "├──────────────────┴──────────┴───────────────┤")
        Log.d(TAG, "│ Total: ${totalDuration}ms                              │")
        Log.d(TAG, "└─────────────────────────────────────────────┘")
    }

    /**
     * 获取任务执行结果
     */
    fun getTaskResults(): List<TaskResult> {
        return tasks.map { task ->
            TaskResult(
                taskName = task.taskName,
                durationMs = task.durationMs,
                isCompleted = task.isCompleted,
                runOnMainThread = task.runOnMainThread
            )
        }
    }

    fun reset() {
        tasks.clear()
        completedTasks.clear()
        isRunning.set(false)
    }
}

data class TaskResult(
    val taskName: String,
    val durationMs: Long,
    val isCompleted: Boolean,
    val runOnMainThread: Boolean
)
```



### SplashScreenActivity.kt — Splash Screen配置

```kotlin
package com.mathknowledge.app.performance.startup

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 优化的启动画面Activity
 *
 * 优化点：
 * 1. 使用 Android 12+ SplashScreen API
 * 2. 启动任务在Splash期间并行执行
 * 3. 自定义Compose动画，避免过度绘制
 * 4. 最小化布局层级
 */
class SplashScreenActivity : ComponentActivity() {

    private val startupManager = AppStartupManager.getInstance()
    private var isReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装Splash Screen（Android 12+原生支持）
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 保持Splash Screen直到数据准备就绪
        splashScreen.setKeepOnScreenCondition { !isReady }

        // 设置退出动画
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenView.view,
                "alpha",
                1f, 0f
            )
            fadeOut.duration = 300L
            fadeOut.doOnEnd { splashScreenView.remove() }
            fadeOut.start()
        }

        // 注册启动任务
        registerStartupTasks()

        // 执行启动任务
        executeStartupTasks()

        setContent {
            SplashScreenContent()
        }
    }

    private fun registerStartupTasks() {
        startupManager.registerAll(listOf(
            InitLogTask(),
            InitNetworkTask(),
            InitDatabaseTask(),
            PreloadUserDataTask()
        ))
    }

    private fun executeStartupTasks() {
        lifecycleScope.launch {
            startupManager.execute(applicationContext)

            // 预加载关键资源
            PreloadManager.getInstance().preloadCriticalResources(applicationContext)

            isReady = true

            // 跳转到主界面
            delay(100) // 短暂延迟确保UI平滑过渡
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, Class.forName("com.mathknowledge.app.MainActivity"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    @Composable
    private fun SplashScreenContent() {
        val infiniteTransition = rememberInfiniteTransition(label = "splash")

        // Logo缩放动画
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = OvershootInterpolator(2f).toEasing()),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logoScale"
        )

        // Logo透明度动画
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logoAlpha"
        )

        // 文字淡入动画
        val textAlpha by animateFloatAsState(
            targetValue = if (isReady) 1f else 0.6f,
            animationSpec = tween(500),
            label = "textAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A237E),
                            Color(0xFF0D47A1),
                            Color(0xFF01579B)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo区域（使用固定尺寸避免重组）
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 替换为实际的Logo资源
                    // Image(
                    //     painter = painterResource(id = R.drawable.ic_logo),
                    //     contentDescription = "Logo"
                    // )
                    Text(
                        text = "∑",
                        fontSize = 72.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App名称
                Text(
                    text = "MathKnowledge",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 副标题
                Text(
                    text = "探索数学的奥秘",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.alpha(textAlpha)
                )
            }
        }
    }
}

/**
 * 扩展函数：将Android interpolator转为Compose easing
 */
private fun android.view.animation.Interpolator.toEasing(): Easing {
    val interpolator = this
    return Easing { fraction ->
        interpolator.getInterpolation(fraction)
    }
}
```

### PreloadManager.kt — 预加载管理器

```kotlin
package com.mathknowledge.app.performance.startup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.*
import java.net.URL

/**
 * 预加载管理器
 *
 * 负责在启动阶段预加载关键资源：
 * 1. 关键图片资源
 * 2. 热门课程数据
 * 3. 用户配置信息
 * 4. 字体文件
 */
class PreloadManager private constructor() {

    companion object {
        private const val TAG = "PreloadManager"

        @Volatile
        private var instance: PreloadManager? = null

        fun getInstance(): PreloadManager {
            return instance ?: synchronized(this) {
                instance ?: PreloadManager().also { instance = it }
            }
        }
    }

    // 预加载的图片缓存
    private val preloadImageCache = LruCache<String, Bitmap>(10 * 1024 * 1024) // 10MB

    // 预加载的数据缓存
    private val preloadDataCache = LruCache<String, Any>(50)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 预加载关键资源
     */
    suspend fun preloadCriticalResources(context: Context) {
        Log.d(TAG, "Starting critical resource preloading...")

        coroutineScope {
            // 并行预加载各类资源
            launch { preloadDrawableResources(context) }
            launch { preloadFontResources(context) }
            launch { preloadCriticalData(context) }
        }

        Log.d(TAG, "Critical resource preloading completed")
    }

    /**
     * 预加载Drawable资源（解码到内存）
     */
    private suspend fun preloadDrawableResources(context: Context) {
        val criticalDrawables = listOf(
            // R.drawable.ic_home,
            // R.drawable.ic_profile,
            // R.drawable.ic_course_default,
            // R.drawable.bg_gradient_header
        )

        criticalDrawables.forEach { resId ->
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(context, resId, 200, 200)
                }
                bitmap?.let {
                    preloadImageCache.put("drawable_$resId", it)
                    Log.d(TAG, "Preloaded drawable: $resId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload drawable: $resId", e)
            }
        }
    }

    /**
     * 预加载字体资源