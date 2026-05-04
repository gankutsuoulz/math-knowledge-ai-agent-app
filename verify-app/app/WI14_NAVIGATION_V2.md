# 导航集成模块修复方案

根据问题描述，我将修复导航集成模块中的三个主要问题。以下是修复后的完整代码：

```kotlin
// 修复后的导航集成模块代码
package com.example.mathapp.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mathapp.ui.screens.*

// 定义底部导航路由集合
val bottomNavigationRoutes = setOf(
    "home",
    "profile",
    "settings",
    "history"
)

@Composable
fun MainNavigation(
    navigationEvent: NavigationEvent? = null,
    onRouteChanged: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 问题3修复：添加LaunchedEffect消费navigationEvent
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is NavigationEvent.Navigate -> {
                    navController.navigate(event.route) {
                        // 避免重复创建实例
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is NavigationEvent.PopBackStack -> {
                    navController.popBackStack()
                }
                is NavigationEvent.NavigateUp -> {
                    navController.navigateUp()
                }
            }
        }
    }

    // 问题2修复：使用currentBackStackEntryAsState()观察路由变化
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            onRouteChanged(route)
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation {
                // 问题1修复：使用精确的路由ID判断
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("首页") },
                    selected = currentRoute == "home",
                    onClick = {
                        navigateToRoute(navController, "home")
                    }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("个人") },
                    selected = currentRoute == "profile",
                    onClick = {
                        navigateToRoute(navController, "profile")
                    }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("设置") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navigateToRoute(navController, "settings")
                    }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("历史") },
                    selected = currentRoute == "history",
                    onClick = {
                        navigateToRoute(navController, "history")
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            // 问题2修复：移除composable内的显式回调
            composable("home") {
                HomeScreen()
            }
            composable("profile") {
                ProfileScreen()
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("history") {
                HistoryScreen()
            }
            composable("math/{topicId}") { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                MathScreen(topicId = topicId)
            }
            composable("quiz/{quizId}") { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                QuizScreen(quizId = quizId)
            }
        }
    }
}

// 辅助函数：安全导航到路由
private fun navigateToRoute(navController: NavController, route: String) {
    navController.navigate(route) {
        // 避免重复创建实例
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

// 导航事件密封类
sealed class NavigationEvent {
    data class Navigate(val route: String) : NavigationEvent()
    object PopBackStack : NavigationEvent()
    object NavigateUp : NavigationEvent()
}

// 导航工具类
object NavigationUtils {
    // 检查路由是否为底部导航路由
    fun isBottomNavigationRoute(route: String?): Boolean {
        return route in bottomNavigationRoutes
    }
    
    // 获取路由的基础部分（去除参数）
    fun getBaseRoute(route: String?): String {
        return route?.substringBefore("?")?.substringBefore("{") ?: ""
    }
    
    // 构建带参数的路由
    fun buildRoute(baseRoute: String, params: Map<String, String> = emptyMap()): String {
        var route = baseRoute
        params.forEach { (key, value) ->
            route = route.replace("{$key}", value)
        }
        return route
    }
}

// 导航状态管理器
@Composable
fun rememberNavigationState(
    navController: NavController = rememberNavController()
): NavigationState {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    return remember(navController) {
        NavigationState(
            navController = navController,
            currentRoute = currentRoute
        )
    }
}

data class NavigationState(
    val navController: NavController,
    val currentRoute: String?
) {
    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    
    fun popBackStack() {
        navController.popBackStack()
    }
    
    fun navigateUp() {
        navController.navigateUp()
    }
    
    fun isCurrentRoute(route: String): Boolean {
        return currentRoute == route
    }
    
    fun isBottomNavigationRoute(): Boolean {
        return NavigationUtils.isBottomNavigationRoute(currentRoute)
    }
}

// 导航监听器
@Composable
fun NavigationListener(
    navController: NavController,
    onRouteChanged: (String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            onRouteChanged(route)
        }
    }
}

// 导航守卫
@Composable
fun NavigationGuard(
    navController: NavController,
    guardCondition: (String) -> Boolean,
    onGuardFailed: (String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            if (!guardCondition(route)) {
                onGuardFailed(route)
            }
        }
    }
}
```

## 修复说明

### 问题1修复：路由匹配逻辑增强
1. **维护底部导航路由集合**：定义了`bottomNavigationRoutes`集合，包含所有底部导航路由
2. **精确路由ID判断**：在`BottomNavigationItem`中使用`currentRoute == "home"`进行精确匹配
3. **添加路由工具类**：`NavigationUtils`提供了路由处理的辅助方法

### 问题2修复：onRouteChanged回调优化
1. **移除composable内的显式回调**：从各个`composable`块中移除了`onRouteChanged`调用
2. **使用currentBackStackEntryAsState()**：在`MainNavigation`组件中通过`LaunchedEffect`监听路由变化
3. **添加NavigationListener组件**：提供可复用的路由监听功能

### 问题3修复：NavigationEvent消费
1. **添加LaunchedEffect**：在`MainNavigation`中添加`LaunchedEffect`来消费`navigationEvent`
2. **实现事件处理逻辑**：处理`Navigate`、`PopBackStack`、`NavigateUp`等事件
3. **密封类定义**：使用密封类`NavigationEvent`定义所有导航事件类型

## 额外改进

1. **NavigationState管理器**：提供状态管理功能，简化导航操作
2. **NavigationGuard组件**：提供路由守卫功能，用于权限控制等场景
3. **路由工具方法**：提供路由参数处理、路由构建等实用功能
4. **代码结构优化**：将导航相关逻辑模块化，提高代码可维护性

这个修复方案解决了原始代码中的三个主要问题，同时提供了更健壮、更易维护的导航集成架构。