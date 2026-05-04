package com.mathkatex.verify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.mathkatex.verify.util.LogFileManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mathkatex.verify.data.SettingsManager
import com.mathkatex.verify.data.PhotoSolveHistoryItem
import com.mathkatex.verify.data.PhotoSolveHistoryManager
import com.mathkatex.verify.screens.*
import com.mathkatex.verify.ui.theme.KaTeXVerifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化设置管理
        SettingsManager.init(applicationContext)
        // 初始化日志文件
        LogFileManager.init(applicationContext)
        // 初始化历史记录管理
        com.mathkatex.verify.data.PhotoSolveHistoryManager.init(applicationContext)
        
        setContent {
            KaTeXVerifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
    object PhotoSolveHistory : Screen("photo_solve_history", "解题历史")
    object PhotoSolveHistoryDetail : Screen("photo_solve_history_detail", "历史详情")
    object Knowledge : Screen("knowledge", "知识点速查")
    object Practice : Screen("practice", "例题练习")
    object Library : Screen("library", "例题库管理")
    object Favorites : Screen("favorites", "收藏夹")
    object Search : Screen("search", "搜索")
    object ErrorGuide : Screen("error_guide", "错误辅导")
    object FormulaConvert : Screen("formula_convert", "拍照转公式")
    object Settings : Screen("settings", "设置")
}

data class MenuItem(
    val screen: String,
    val title: String,
    val icon: ImageVector
)

val menuItems = listOf(
    MenuItem("photo_solve", "拍照解题", Icons.Default.CameraAlt),
    MenuItem("knowledge", "知识点速查", Icons.AutoMirrored.Filled.MenuBook),
    MenuItem("practice", "例题练习", Icons.Default.Edit),
    MenuItem("library", "例题库管理", Icons.AutoMirrored.Filled.LibraryBooks),
    MenuItem("favorites", "收藏夹", Icons.Default.Favorite),
    MenuItem("search", "搜索", Icons.Default.Search),
    MenuItem("error_guide", "错误辅导", Icons.Default.ErrorOutline),
    MenuItem("formula_convert", "拍照转公式", Icons.Default.Functions),
    MenuItem("settings", "设置", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    
    val currentScreen = when (currentRoute) {
        Screen.PhotoSolve.route -> Screen.PhotoSolve
        Screen.Knowledge.route -> Screen.Knowledge
        Screen.Practice.route -> Screen.Practice
        Screen.Library.route -> Screen.Library
        Screen.Favorites.route -> Screen.Favorites
        Screen.Search.route -> Screen.Search
        Screen.ErrorGuide.route -> Screen.ErrorGuide
        Screen.FormulaConvert.route -> Screen.FormulaConvert
        Screen.Settings.route -> Screen.Settings
        else -> Screen.Home
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.PhotoSolve.route) { PhotoSolveScreen(navController) }
            composable(Screen.PhotoSolveHistory.route) { 
                PhotoSolveHistoryScreen(
                    onNavigateBack = { PhotoSolveHistoryManager.clearSelectedItemJson(); navController.popBackStack() },
                    onItemClick = { item -> 
                        // 将 item 转为 JSON
                        val itemJson = com.google.gson.Gson().toJson(item)
                        // 存储到静态变量作为后备方案
                        PhotoSolveHistoryManager.setSelectedItemJson(itemJson)
                        // 先存储到当前 entry，然后导航
                        navController.currentBackStackEntry?.savedStateHandle?.set("history_item_json", itemJson)
                        navController.navigate(Screen.PhotoSolveHistoryDetail.route)
                    }
                )
            }
            composable(Screen.PhotoSolveHistoryDetail.route) { backStackEntry ->
                // 从当前 entry 获取 JSON
                val itemJson = backStackEntry.savedStateHandle.get<String>("history_item_json")
                val parsedItem = itemJson?.let { json -> 
                    try {
                        com.google.gson.Gson().fromJson(json, PhotoSolveHistoryItem::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (parsedItem != null) {
                    PhotoSolveHistoryDetailScreen(
                        item = parsedItem,
                        onNavigateBack = { PhotoSolveHistoryManager.clearSelectedItemJson(); navController.popBackStack() }
                    )
                } else {
                    // 尝试从上一个 entry 获取（兼容旧逻辑）
                    val previousJson = navController.previousBackStackEntry?.savedStateHandle?.get<String>("history_item_json")
                    val previousItem = previousJson?.let { json ->
                        try {
                            com.google.gson.Gson().fromJson(json, PhotoSolveHistoryItem::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (previousItem != null) {
                        PhotoSolveHistoryDetailScreen(
                            item = previousItem,
                            onNavigateBack = { 
                                PhotoSolveHistoryManager.clearSelectedItemJson()
                                navController.popBackStack() 
                            }
                        )
                    } else {
                        // 尝试从静态变量获取（最后的后备方案）
                        val staticJson = PhotoSolveHistoryManager.getSelectedItemJson()
                        val staticItem = staticJson?.let { json ->
                            try {
                                com.google.gson.Gson().fromJson(json, PhotoSolveHistoryItem::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (staticItem != null) {
                            PhotoSolveHistoryDetailScreen(
                                item = staticItem,
                                onNavigateBack = { 
                                    PhotoSolveHistoryManager.clearSelectedItemJson()
                                    navController.popBackStack() 
                                }
                            )
                        } else {
                            // 数据获取失败，返回上一页
                            PhotoSolveHistoryManager.clearSelectedItemJson()
                            navController.popBackStack()
                        }
                    }
                }
            }
            composable(Screen.Knowledge.route) { KnowledgeScreen() }
            composable(Screen.Practice.route) { PracticeScreen() }
            composable(Screen.Library.route) { LibraryScreen() }
            composable(Screen.Favorites.route) { FavoritesScreen() }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.ErrorGuide.route) { ErrorGuideScreen() }
            composable(Screen.FormulaConvert.route) { FormulaConvertScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}