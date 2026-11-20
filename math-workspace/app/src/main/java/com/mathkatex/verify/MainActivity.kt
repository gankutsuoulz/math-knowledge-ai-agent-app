package com.mathkatex.verify

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mathkatex.verify.data.SettingsManager
import com.mathkatex.verify.data.local.ExampleRepository
import com.mathkatex.verify.data.local.LearningRepository
import com.mathkatex.verify.data.local.KnowledgeRepository
import com.mathkatex.verify.screens.ErrorGuideScreen
import com.mathkatex.verify.screens.FavoritesScreen
import com.mathkatex.verify.screens.FormulaConvertScreen
import com.mathkatex.verify.screens.HomeScreen
import com.mathkatex.verify.screens.ImageEditScreen
import com.mathkatex.verify.screens.KnowledgeScreen
import com.mathkatex.verify.screens.LibraryScreen
import com.mathkatex.verify.screens.MineScreen
import com.mathkatex.verify.screens.PhotoSolveScreen
import com.mathkatex.verify.screens.RegionSelectScreen
import com.mathkatex.verify.screens.PathsScreen
import com.mathkatex.verify.screens.ReportScreen
import com.mathkatex.verify.screens.ReviewScreen
import com.mathkatex.verify.screens.PracticeScreen
import com.mathkatex.verify.screens.SearchScreen
import com.mathkatex.verify.screens.SettingsScreen
import com.mathkatex.verify.screens.WrongBookScreen
import com.mathkatex.verify.ui.theme.KaTeXVerifyTheme
import com.mathkatex.verify.util.LogFileManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 状态栏/导航栏透明，内容延伸到系统栏，颜色跟随页面背景
        enableEdgeToEdge()

        SettingsManager.init(applicationContext)
        LogFileManager.init(applicationContext)
        lifecycleScope.launchWhenStarted {
            KnowledgeRepository.init(applicationContext)
            // 启动即灌例题，保证知识详情能立刻看到相关例题
            ExampleRepository.init(applicationContext)
            LearningRepository.init(applicationContext)
        }

        setContent {
            KaTeXVerifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MathAppNavigation()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "首页")
    object PhotoSolve : Screen("photo_solve", "拍照解题")
    object ImageEdit : Screen("image_edit", "图片编辑")
    object RegionSelect : Screen("region_select", "圈选题目区域")
    object Knowledge : Screen("knowledge", "知识点速查")
    object Practice : Screen("practice", "例题练习")
    object Library : Screen("library", "例题库管理")
    object Favorites : Screen("favorites", "收藏夹")
    object WrongBook : Screen("wrong_book", "错题本")
    object Search : Screen("search", "搜索")
    object ErrorGuide : Screen("error_guide", "错误辅导")
    object FormulaConvert : Screen("formula_convert", "拍照转公式")
    object Mine : Screen("mine", "我的")
    object Review : Screen("review", "今日复习")
    object Report : Screen("report", "学习报告")
    object Paths : Screen("paths", "专题路径")
    object Settings : Screen("settings", "设置")
}

private data class TabItem(val route: String, val title: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Screen.Home.route, "解题", Icons.Default.CameraAlt),
    TabItem(Screen.Knowledge.route, "知识", Icons.AutoMirrored.Filled.MenuBook),
    TabItem(Screen.Mine.route, "我的", Icons.Default.Person),
)

// 图片编辑相关变量
private var imageEditSourceBitmap: Bitmap? = null
private var pendingEditResult: Bitmap? = null

@Composable
fun MathAppNavigation() {
    val navController = rememberNavController()
    // 圈选：来源图 / 回填结果

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val showTabs = tabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // 顶层只负责底部 Tab，系统栏交给各页面自己处理，避免内层 Scaffold 重复加 padding
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showTabs) {
                AppBottomBar(navController, currentRoute)
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Knowledge.route) {
                KnowledgeScreen(
                    onStartPractice = { id ->
                        navController.navigate("${Screen.Practice.route}?knowledgeId=$id")
                    }
                )
            }
            composable(Screen.Mine.route) { MineScreen(navController) }
            composable(Screen.PhotoSolve.route) {
                PhotoSolveScreen(
                    onStartPractice = { id ->
                        navController.navigate("${Screen.Practice.route}?knowledgeId=$id")
                    },
                    onNavigateToImageEdit = { bitmap ->
                        imageEditSourceBitmap = bitmap
                        navController.navigate(Screen.ImageEdit.route)
                    },

                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.ImageEdit.route) {
                val bitmapToEdit = imageEditSourceBitmap
                if (bitmapToEdit != null) {
                    ImageEditScreen(
                        originalBitmap = bitmapToEdit,
                        onConfirm = { croppedBitmap ->
                            pendingEditResult = croppedBitmap
                            imageEditSourceBitmap = null
                            navController.popBackStack()
                        },
                        onCancel = {
                            imageEditSourceBitmap = null
                            navController.popBackStack()
                        },
                    )
                } else {
                    // 来源位图已被消费（如确认后置空）：保持空白，避免二次 pop 掉上一页
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize())
                }
            }
            composable(
                route = "${Screen.Practice.route}?knowledgeId={knowledgeId}",
                arguments = listOf(navArgument("knowledgeId") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                PracticeScreen(
                    knowledgeId = entry.arguments?.getString("knowledgeId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Library.route) { LibraryScreen() }
            composable(Screen.Favorites.route) { FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onStartPractice = { id -> navController.navigate("${Screen.Practice.route}?knowledgeId=$id") }
                ) }
            composable(Screen.WrongBook.route) { WrongBookScreen(
                    onBack = { navController.popBackStack() },
                    onStartPractice = { id -> navController.navigate("${Screen.Practice.route}?knowledgeId=$id") }
                ) }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.ErrorGuide.route) { ErrorGuideScreen() }
            composable(Screen.FormulaConvert.route) { FormulaConvertScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController, currentRoute: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title) },
            )
        }
    }
}
