package com.mathkatex.verify.screens

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.mathkatex.verify.data.local.LearningRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mathkatex.verify.R
import com.mathkatex.verify.Screen
import com.mathkatex.verify.ui.theme.AppColors
import com.mathkatex.verify.ui.theme.LocalAppColors
import java.util.Calendar

private data class QuickEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

@Composable
fun HomeScreen(navController: NavHostController) {
    val colors = LocalAppColors.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val hello = when {
        hour < 6 -> "夜深了，同学"
        hour < 12 -> "早上好，同学"
        hour < 18 -> "下午好，同学"
        else -> "晚上好，同学"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HomeHeader(colors, hello) }
        item { PhotoSolveCta(colors) { navController.navigate(Screen.PhotoSolve.route) } }
        item { QuickGrid(colors, navController) }
        item {
            TodayStepsCard(
                colors = colors,
                onReview = { navController.navigate(Screen.Review.route) },
                onPractice = { navController.navigate(Screen.Practice.route) },
                onPaths = { navController.navigate(Screen.Paths.route) },
            )
        }
        item { ContinueCard(colors) { navController.navigate(Screen.Paths.route) } }
        item { DailyStrip(colors) }
    }
}

@Composable
private fun HomeHeader(colors: AppColors, hello: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hello,
                color = colors.text3,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "今天想解决\n哪道题？",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text,
            )
        }
        Image(
            painter = painterResource(R.drawable.avatar_jingxi),
            contentDescription = "鲸喜",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(1.dp, colors.primary.copy(alpha = 0.16f), RoundedCornerShape(15.dp)),
        )
    }
}

@Composable
private fun PhotoSolveCta(colors: AppColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    // 深色模式下 primary 会变浅，实心 CTA 固定用深海蓝保证白字对比度
                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF3B82F6)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text("AI 引导式辅导", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Text("拍照解题", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("拍下题目，卡住了再给提示", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(50.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = colors.primaryStrong,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}

@Composable
private fun QuickGrid(colors: AppColors, navController: NavHostController) {
    val entries = listOf(
        QuickEntry("知识点速查", "公式 · 分类", Icons.Default.Book, Screen.Knowledge.route),
        QuickEntry("错题本", "3 道待复习", Icons.Default.Layers, Screen.WrongBook.route),
        QuickEntry("我的收藏", "12 个知识点", Icons.Outlined.BookmarkBorder, Screen.Favorites.route),
        QuickEntry("例题练习", "分难度刷题", Icons.Default.Schedule, Screen.Practice.route),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { entry ->
                    QuickCard(colors, entry, Modifier.weight(1f)) { navController.navigate(entry.route) }
                }
            }
        }
    }
}

@Composable
private fun QuickCard(colors: AppColors, entry: QuickEntry, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(entry.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    entry.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(entry.subtitle, fontSize = 11.sp, color = colors.text3, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ContinueCard(colors: AppColors, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("继续上次", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Spacer(Modifier.weight(1f))
            Text("全部", fontSize = 12.sp, color = colors.text3)
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.line),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.primarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("3/8", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("数与代数 · 一元二次方程", fontSize = 11.sp, color = colors.text3)
                    Spacer(Modifier.height(3.dp))
                    Text("求根公式与判别式", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.text3)
            }
        }
    }
}

@Composable
private fun DailyStrip(colors: AppColors) {
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (index < 2) colors.primary else colors.surface2),
                )
                Spacer(Modifier.width(5.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text("今日目标 2 / 4 题", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            Spacer(Modifier.weight(1f))
            Text("约 10 分钟", fontSize = 11.sp, color = colors.text3)
        }
    }
}


@Composable
private fun ReviewHomeCard(colors: AppColors, onClick: () -> Unit) {
    val context = LocalContext.current
    var due by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        LearningRepository.init(context)
        due = LearningRepository.dueCount()
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) { Text("复", fontSize = 15.sp, color = colors.primary, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (due > 0) "今日复习 $due 个知识点" else "今日复习",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (due > 0) "按掌握度安排，趁热打铁" else "暂无到期，去练几道新题",
                    fontSize = 11.5.sp, color = colors.text3,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.text3)
        }
    }
}


@Composable
private fun TodayStepsCard(
    colors: AppColors,
    onReview: () -> Unit,
    onPractice: () -> Unit,
    onPaths: () -> Unit,
) {
    val context = LocalContext.current
    var due by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        LearningRepository.init(context)
        due = LearningRepository.dueCount()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(colors.primarySoft),
                    contentAlignment = Alignment.Center,
                ) { Text("今", fontSize = 14.sp, color = colors.primary, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("今日三步", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                    Text(
                        if (due > 0) "先复习 $due 个到期知识点，再练新题" else "复习 · 练习 · 看路径，照着走就行",
                        fontSize = 11.sp, color = colors.text3,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            TodayStepRow("1", if (due > 0) "复习 $due 个到期知识点" else "今天没有到期复习", colors, onReview)
            TodayStepRow("2", "练 3 道题，巩固手感", colors, onPractice)
            TodayStepRow("3", "看一条专题路径，知道下一步学什么", colors, onPaths)
        }
    }
}

@Composable
private fun TodayStepRow(no: String, text: String, colors: AppColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(99.dp)).background(colors.primarySoft),
            contentAlignment = Alignment.Center,
        ) { Text(no, fontSize = 10.5.sp, color = colors.primary, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(9.dp))
        Text(text, fontSize = 12.sp, color = colors.text2, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.text3, modifier = Modifier.size(16.dp))
    }
}
