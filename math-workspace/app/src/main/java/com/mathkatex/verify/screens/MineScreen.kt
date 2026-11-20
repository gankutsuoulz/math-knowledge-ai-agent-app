package com.mathkatex.verify.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mathkatex.verify.R
import com.mathkatex.verify.Screen
import com.mathkatex.verify.data.local.ExampleRepository
import com.mathkatex.verify.ui.theme.AppColors
import com.mathkatex.verify.ui.theme.LocalAppColors

private data class MineEntry(val title: String, val icon: ImageVector, val route: String)

@Composable
fun MineScreen(navController: NavHostController) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var practiced by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }
    var favoritesCount by remember { mutableStateOf(0) }
    var wrongCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        ExampleRepository.init(context)
        val (total, right) = ExampleRepository.stats()
        practiced = total
        correct = right
        favoritesCount = ExampleRepository.favoriteIds().size
        wrongCount = ExampleRepository.wrongExamples().size
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.avatar_jingxi),
                contentDescription = "鲸喜",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("同学，你好", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Spacer(Modifier.height(4.dp))
                Text("今天也慢慢来，先把一道题想明白", fontSize = 12.sp, color = colors.text3)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(colors, "$practiced", "已练题数", Modifier.weight(1f))
            StatCard(colors, if (practiced == 0) "—" else "${correct * 100 / practiced}%", "正确率", Modifier.weight(1f))
            StatCard(colors, "$favoritesCount", "收藏例题", Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.line),
        ) {
            Column {
                MineRow(colors, MineEntry("我的收藏", Icons.Default.Favorite, Screen.Favorites.route), navController)
                MineRow(colors, MineEntry("错题本", Icons.Default.Layers, Screen.WrongBook.route), navController)
                MineRow(colors, MineEntry("学习记录", Icons.Default.Schedule, Screen.Report.route), navController)
                MineRow(colors, MineEntry("设置", Icons.Default.Settings, Screen.Settings.route), navController, showDivider = false)
            }
        }
    }
}

@Composable
private fun StatCard(colors: AppColors, value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.line),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Spacer(Modifier.height(3.dp))
            Text(label, fontSize = 11.sp, color = colors.text3)
        }
    }
}

@Composable
private fun MineRow(
    colors: AppColors,
    entry: MineEntry,
    navController: NavHostController,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(entry.route) }
                .padding(horizontal = 15.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(entry.icon, contentDescription = null, tint = colors.text2, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(entry.title, fontSize = 13.5.sp, color = colors.text)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.text3, modifier = Modifier.size(18.dp))
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line),
            )
        }
    }
}
