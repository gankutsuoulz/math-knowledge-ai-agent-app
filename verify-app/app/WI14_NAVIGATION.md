# 导航集成模块完整代码

## 项目结构

```
com.mathknowledge.app/
├── navigation/
│   ├── Route.kt
│   ├── NavigationViewModel.kt
│   ├── MainScreen.kt
│   ├── BottomNavBar.kt
│   ├── NavGraph.kt
│   └── Screen.kt
├── ui/
│   ├── screen/
│   │   ├── home/
│   │   │   └── HomeScreen.kt
│   │   ├── knowledge/
│   │   │   └── KnowledgeScreen.kt
│   │   ├── practice/
│   │   │   └── PracticeScreen.kt
│   │   └── profile/
│   │       └── ProfileScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── di/
│   └── AppModule.kt
└── MathKnowledgeApp.kt
```

---

## 1. 路由常量定义

```kotlin
// navigation/Route.kt
package com.mathknowledge.app.navigation

/**
 * 路由常量定义
 * 遵循 Clean Architecture 原则，集中管理所有路由路径
 */
object Route {

    // ==================== 底部导航主路由 ====================
    const val HOME = "home"
    const val KNOWLEDGE = "knowledge"
    const val PRACTICE = "practice"
    const val PROFILE = "profile"

    // ==================== 首页子路由 ====================
    object Home {
        const val HOME = "home_screen"
        const val NOTIFICATION = "home/notification"
        const val SEARCH = "home/search"
        const val DAILY_RECOMMEND = "home/daily_recommend"
        const val STUDY_PLAN = "home/study_plan"
    }

    // ==================== 知识模块子路由 ====================
    object Knowledge {
        const val KNOWLEDGE = "knowledge_screen"
        const val CATEGORY_LIST = "knowledge/category_list"
        const val CATEGORY_DETAIL = "knowledge/category_detail/{categoryId}"
        const val ARTICLE_DETAIL = "knowledge/article_detail/{articleId}"
        const val BOOKMARK_LIST = "knowledge/bookmark_list"
        const val SEARCH = "knowledge/search"

        // 带参数路由构建
        fun categoryDetail(categoryId: String) = "knowledge/category_detail/$categoryId"
        fun articleDetail(articleId: String) = "knowledge/article_detail/$articleId"
    }

    // ==================== 练习模块子路由 ====================
    object Practice {
        const val PRACTICE = "practice_screen"
        const val EXERCISE_LIST = "practice/exercise_list"
        const val EXERCISE_DETAIL = "practice/exercise_detail/{exerciseId}"
        const val EXERCISE_RESULT = "practice/exercise_result/{resultId}"
        const val EXAM_MODE = "practice/exam_mode/{examId}"
        const val EXAM_RESULT = "practice/exam_result/{examId}"
        const val ERROR_BOOK = "practice/error_book"
        const val COLLECTION = "practice/collection"
        const val RANKING = "practice/ranking"

        fun exerciseDetail(exerciseId: String) = "practice/exercise_detail/$exerciseId"
        fun exerciseResult(resultId: String) = "practice/exercise_result/$resultId"
        fun examMode(examId: String) = "practice/exam_mode/$examId"
        fun examResult(examId: String) = "practice/exam_result/$examId"
    }

    // ==================== 我的模块子路由 ====================
    object Profile {
        const val PROFILE = "profile_screen"
        const val SETTINGS = "profile/settings"
        const val EDIT_PROFILE = "profile/edit_profile"
        const val STUDY_HISTORY = "profile/study_history"
        const val STUDY_STATISTICS = "profile/study_statistics"
        const val ACHIEVEMENT = "profile/achievement"
        const val ABOUT = "profile/about"
        const val FEEDBACK = "profile/feedback"
        const val LOGIN = "profile/login"
        const val REGISTER = "profile/register"
    }

    // ==================== 通用路由 ====================
    object Common {
        const val WEB_VIEW = "common/web_view?url={url}&title={title}"
        const val IMAGE_VIEWER = "common/image_viewer?url={url}"
        const val ABOUT = "common/about"

        fun webView(url: String, title: String) = "common/web_view?url=$url&title=$title"
        fun imageViewer(url: String) = "common/image_viewer?url=$url"
    }
}
```

---

## 2. Screen 定义（导航项封装）

```kotlin
// navigation/Screen.kt
package com.mathknowledge.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.ui.graphics.vector.ImageVector
import com.mathknowledge.app.R

/**
 * 底部导航项数据模型
 */
sealed class Screen(
    val route: String,
    @StringRes val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen(
        route = Route.Home.HOME,
        titleResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Knowledge : Screen(
        route = Route.Knowledge.KNOWLEDGE,
        titleResId = R.string.nav_knowledge,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    )

    object Practice : Screen(
        route = Route.Practice.PRACTICE,
        titleResId = R.string.nav_practice,
        selectedIcon = Icons.Filled.Quiz,
        unselectedIcon = Icons.Outlined.Quiz
    )

    object Profile : Screen(
        route = Route.Profile.PROFILE,
        titleResId = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val bottomNavItems = listOf(Home, Knowledge, Practice, Profile)
    }
}
```

---

## 3. NavigationViewModel

```kotlin
// navigation/NavigationViewModel.kt
package com.mathknowledge.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 导航状态数据类
 */
data class NavigationState(
    val currentRoute: String = Route.Home.HOME,
    val previousRoute: String? = null,
    val selectedBottomNavItem: Int = 0,
    val isBottomBarVisible: Boolean = true,
    val navigationHistory: List<String> = listOf(Route.Home.HOME),
    val navigationParams: Map<String, Any?> = emptyMap()
)

/**
 * 导航事件（单次触发）
 */
sealed class NavigationEvent {
    data class NavigateTo(val route: String, val params: Map<String, Any?> = emptyMap()) : NavigationEvent()
    object NavigateBack : NavigationEvent()
    data class NavigateAndPopUpTo(
        val route: String,
        val popUpToRoute: String,
        val inclusive: Boolean = false
    ) : NavigationEvent()
    object NavigateToHome : NavigationEvent()
    data class ShowToast(val message: String) : NavigationEvent()
}

/**
 * NavigationViewModel
 * 管理底部导航状态、路由历史和导航事件
 */
@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    // 底部导航项对应的路由列表
    private val bottomNavRoutes = Screen.bottomNavItems.map { it.route }

    /**
     * 更新当前路由
     */
    fun updateCurrentRoute(route: String?) {
        route ?: return
        val cleanRoute = route.substringBefore("?").substringBefore("/{")

        // 判断是否为底部导航项
        val bottomNavIndex = bottomNavRoutes.indexOfFirst { bottomRoute ->
            cleanRoute.startsWith(bottomRoute.substringBefore("_screen"))
        }.coerceAtLeast(0)

        _navigationState.update { state ->
            state.copy(
                previousRoute = state.currentRoute,
                currentRoute = route,
                selectedBottomNavItem = bottomNavIndex,
                isBottomBarVisible = cleanRoute in bottomNavRoutes,
                navigationHistory = state.navigationHistory + route
            )
        }
    }

    /**
     * 处理底部导航点击
     */
    fun onBottomNavItemSelected(index: Int) {
        val screen = Screen.bottomNavItems.getOrNull(index) ?: return
        val targetRoute = screen.route

        // 如果点击的是当前选中的项，不做处理
        if (_navigationState.value.selectedBottomNavItem == index) return

        _navigationState.update { state ->
            state.copy(
                selectedBottomNavItem = index,
                isBottomBarVisible = true
            )
        }

        _navigationEvent.value = NavigationEvent.NavigateTo(targetRoute)
    }

    /**
     * 导航到指定路由
     */
    fun navigateTo(route: String, params: Map<String, Any?> = emptyMap()) {
        _navigationState.update { state ->
            state.copy(navigationParams = params)
        }
        _navigationEvent.value = NavigationEvent.NavigateTo(route, params)
    }

    /**
     * 返回上一页
     */
    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    /**
     * 导航到指定页面并清除回退栈
     */
    fun navigateAndPopUpTo(
        route: String,
        popUpToRoute: String,
        inclusive: Boolean = false
    ) {
        _navigationEvent.value = NavigationEvent.NavigateAndPopUpTo(route, popUpToRoute, inclusive)
    }

    /**
     * 导航到首页（清除回退栈）
     */
    fun navigateToHome() {
        _navigationState.update { state ->
            state.copy(
                selectedBottomNavItem = 0,
                isBottomBarVisible = true
            )
        }
        _navigationEvent.value = NavigationEvent.NavigateToHome
    }

    /**
     * 显示 Toast
     */
    fun showToast(message: String) {
        _navigationEvent.value = NavigationEvent.ShowToast(message)
    }

    /**
     * 清除导航事件（消费后）
     */
    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    /**
     * 切换底部导航栏可见性
     */
    fun toggleBottomBarVisibility(visible: Boolean) {
        _navigationState.update { it.copy(isBottomBarVisible = visible) }
    }

    /**
     * 清除导航历史
     */
    fun clearNavigationHistory() {
        _navigationState.update { state ->
            state.copy(navigationHistory = listOf(state.currentRoute))
        }
    }
}
```

---

## 4. BottomNavBar

```kotlin
// navigation/BottomNavBar.kt
package com.mathknowledge.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * 底部导航栏组件
 */
@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onItemSelected: (Int) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        Screen.bottomNavItems.forEachIndexed { index, screen ->
            val isSelected = currentRoute?.startsWith(
                screen.route.substringBefore("_screen")
            ) == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    onItemSelected(index)
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = stringResource(id = screen.titleResId)
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = screen.titleResId),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

/**
 * 带角标的底部导航栏
 */
@Composable
fun BottomNavBarWithBadge(
    navController: NavController,
    badges: Map<Int, Int> = emptyMap(), // index -> badge count
    modifier: Modifier = Modifier,
    onItemSelected: (Int) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Screen.bottomNavItems.forEachIndexed { index, screen ->
            val isSelected = currentRoute?.startsWith(
                screen.route.substringBefore("_screen")
            ) == true
            val badgeCount = badges[index] ?: 0

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = stringResource(id = screen.titleResId)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(id = screen.titleResId),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
```

---

## 5. NavGraph 导航图

```kotlin
// navigation/NavGraph.kt
package com.mathknowledge.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mathknowledge.app.ui.screen.home.HomeScreen
import com.mathknowledge.app.ui.screen.knowledge.KnowledgeScreen
import com.mathknowledge.app.ui.screen.practice.PracticeScreen
import com.mathknowledge.app.ui.screen.profile.ProfileScreen

/**
 * 导航动画配置
 */
object NavAnimations {
    private const val ANIM_DURATION = 300

    val enterTransition: EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeIn(animationSpec = tween(ANIM_DURATION))

    val exitTransition: ExitTransition = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeOut(animationSpec = tween(ANIM_DURATION))

    val popEnterTransition: EnterTransition = slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeIn(animationSpec = tween(ANIM_DURATION))

    val popExitTransition: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(ANIM_DURATION)
    ) + fadeOut(animationSpec = tween(ANIM_DURATION))

    // 底部导航切换动画（淡入淡出）
    val bottomNavEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(ANIM_DURATION)
    )
    val bottomNavExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(ANIM_DURATION)
    )
}

/**
 * 主导航图
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onRouteChanged: (String?) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.HOME,
        modifier = modifier,
        enterTransition = { NavAnimations.enterTransition },
        exitTransition = { NavAnimations.exitTransition },
        popEnterTransition = { NavAnimations.popEnterTransition },
        popExitTransition = { NavAnimations.popExitTransition }
    ) {

        // ==================== 首页模块 ====================
        composable(
            route = Route.Home.HOME,
            enterTransition = { NavAnimations.bottomNavEnterTransition },
            exitTransition = { NavAnimations.bottomNavExitTransition }
        ) {
            onRouteChanged(Route.Home.HOME)
            HomeScreen(
                onNavigateToDetail = { route ->
                    navController.navigate(route)
                },
                onNavigateToKnowledge = {
                    navController.navigate(Route.Knowledge.KNOWLEDGE) {
                        popUpTo(Route.Home.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToPractice = {
                    navController.navigate(Route.Practice.PRACTICE) {
                        popUpTo(Route.Home.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(
            route = Route.Home.NOTIFICATION
        ) {
            onRouteChanged(Route.Home.NOTIFICATION)
            // NotificationScreen()
        }

        composable(
            route = Route.Home.SEARCH
        ) {
            onRouteChanged(Route.Home.SEARCH)
            // SearchScreen()
        }

        composable(
            route = Route.Home.DAILY_RECOMMEND
        ) {
            onRouteChanged(Route.Home.DAILY_RECOMMEND)
            // DailyRecommendScreen()
        }

        composable(
            route = Route.Home.STUDY_PLAN
        ) {
            onRouteChanged(Route.Home.STUDY_PLAN)
            // StudyPlanScreen()
        }

        // ==================== 知识模块 ====================
        composable(
            route = Route.Knowledge.KNOWLEDGE,
            enterTransition = { NavAnimations.bottomNavEnterTransition },
            exitTransition = { NavAnimations.bottomNavExitTransition }
        ) {
            onRouteChanged(Route.Knowledge.KNOWLEDGE)
            KnowledgeScreen(
                onNavigateToCategoryDetail = { categoryId ->
                    navController.navigate(Route.Knowledge.categoryDetail(categoryId))
                },
                onNavigateToArticleDetail = { articleId ->
                    navController.navigate(Route.Knowledge.articleDetail(articleId))
                },
                onNavigateToSearch = {
                    navController.navigate(Route.Knowledge.SEARCH)
                },
                onNavigateToBookmarks = {
                    navController.navigate(Route.Knowledge.BOOKMARK_LIST)
                }
            )
        }

        composable(
            route = Route.Knowledge.CATEGORY_LIST
        ) {
            onRouteChanged(Route.Knowledge.CATEGORY_LIST)
            // CategoryListScreen()
        }

        composable(
            route = Route.Knowledge.CATEGORY_DETAIL,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            onRouteChanged(Route.Knowledge.categoryDetail(categoryId))
            // CategoryDetailScreen(categoryId = categoryId)
        }

        composable(
            route = Route.Knowledge.ARTICLE_DETAIL,
            arguments = listOf(
                navArgument("articleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
            onRouteChanged(Route.Knowledge.articleDetail(articleId))
            // ArticleDetailScreen(articleId = articleId)
        }

        composable(
            route = Route.Knowledge.BOOKMARK_LIST
        ) {
            onRouteChanged(Route.Knowledge.BOOKMARK_LIST)
            // BookmarkListScreen()
        }

        composable(
            route = Route.Knowledge.SEARCH
        ) {
            onRouteChanged(Route.Knowledge.SEARCH)
            // KnowledgeSearchScreen()
        }

        // ==================== 练习模块 ====================
        composable(
            route = Route.Practice.PRACTICE,
            enterTransition = { NavAnimations.bottomNavEnterTransition },
            exitTransition = { NavAnimations.bottomNavExitTransition }
        ) {
            onRouteChanged(Route.Practice.PRACTICE)
            PracticeScreen(
                onNavigateToExerciseList = {
                    navController.navigate(Route.Practice.EXERCISE_LIST)
                },
                onNavigateToExerciseDetail = { exerciseId ->
                    navController.navigate(Route.Practice.exerciseDetail(exerciseId))
                },
                onNavigateToExamMode = { examId ->
                    navController.navigate(Route.Practice.examMode(examId))
                },
                onNavigateToErrorBook = {
                    navController.navigate(Route.Practice.ERROR_BOOK)
                },
                onNavigateToCollection = {
                    navController.navigate(Route.Practice.COLLECTION)
                },
                onNavigateToRanking = {
                    navController.navigate(Route.Practice.RANKING)
                }
            )
        }

        composable(
            route = Route.Practice.EXERCISE_LIST
        ) {
            onRouteChanged(Route.Practice.EXERCISE_LIST)
            // ExerciseListScreen()
        }

        composable(
            route = Route.Practice.EXERCISE_DETAIL,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            onRouteChanged(Route.Practice.exerciseDetail(exerciseId))
            // ExerciseDetailScreen(exerciseId = exerciseId)
        }

        composable(
            route = Route.Practice.EXERCISE_RESULT,
            arguments = listOf(
                navArgument("resultId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
            onRouteChanged(Route.Practice.exerciseResult(resultId))
            // ExerciseResultScreen(resultId = resultId)
        }

        composable(
            route = Route.Practice.EXAM_MODE,
            arguments = listOf(
                navArgument("examId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: ""
            onRouteChanged(Route.Practice.examMode(examId))
            // ExamModeScreen(examId = examId)
        }

        composable(
            route = Route.Practice.EXAM_RESULT,
            arguments = listOf(
                navArgument("examId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: ""
            onRouteChanged(Route.Practice.examResult(examId))
            // ExamResultScreen(examId = examId)
        }

        composable(
            route = Route.Practice.ERROR_BOOK
        ) {
            onRouteChanged(Route.Practice.ERROR_BOOK)
            // ErrorBookScreen()
        }

        composable(
            route = Route.Practice.COLLECTION
        ) {
            onRouteChanged(Route.Practice.COLLECTION)
            // CollectionScreen()
        }

        composable(
            route = Route.Practice.RANKING
        ) {
            onRouteChanged(Route.Practice.RANKING)
            // RankingScreen()
        }

        // ==================== 我的模块 ====================
        composable(
            route = Route.Profile.PROFILE,
            enterTransition = { NavAnimations.bottomNavEnterTransition },
            exitTransition = { NavAnimations.bottomNavExitTransition }
        ) {
            onRouteChanged(Route.Profile.PROFILE)
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate(Route.Profile.SETTINGS)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Route.Profile.EDIT_PROFILE)
                },
                onNavigateToStudyHistory = {
                    navController.navigate(Route.Profile.STUDY_HISTORY)
                },
                onNavigateToStatistics = {
                    navController.navigate(Route.Profile.STUDY_STATISTICS)
                },
                onNavigateToAchievement = {
                    navController.navigate(Route.Profile.ACHIEVEMENT)
                },
                onNavigateToAbout = {
                    navController.navigate(Route.Profile.ABOUT)
                },
                onNavigateToFeedback = {
                    navController.navigate(Route.Profile.FEEDBACK)
                },
                onNavigateToLogin = {
                    navController.navigate(Route.Profile.LOGIN) {
                        popUpTo(Route.Profile.PROFILE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Route.Profile.SETTINGS
        ) {
            onRouteChanged(Route.Profile.SETTINGS)
            // SettingsScreen()
        }

        composable(
            route = Route.Profile.EDIT_PROFILE
        ) {
            onRouteChanged(Route.Profile.EDIT_PROFILE)
            // EditProfileScreen()
        }

        composable(
            route = Route.Profile.STUDY_HISTORY
        ) {
            onRouteChanged(Route.Profile.STUDY_HISTORY)
            // StudyHistoryScreen()
        }

        composable(
            route = Route.Profile.STUDY_STATISTICS
        ) {
            onRouteChanged(Route.Profile.STUDY_STATISTICS)
            // StudyStatisticsScreen()
        }

        composable(
            route = Route.Profile.ACHIEVEMENT
        ) {
            onRouteChanged(Route.Profile.ACHIEVEMENT)
            // AchievementScreen()
        }

        composable(
            route = Route.Profile.ABOUT