package com.mathkatex.verify.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.data.local.ExampleRepository
import com.mathkatex.verify.data.local.LearningRepository
import com.mathkatex.verify.ui.theme.LocalAppColors

/** 学习报告：掌握度、复习、答题数据一页看完。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit = {},
    onReview: () -> Unit = {},
    onPractice: () -> Unit = {},
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var mastered by remember { mutableStateOf(0) }
    var avg by remember { mutableStateOf(0f) }
    var due by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        ExampleRepository.init(context)
        LearningRepository.init(context)
        val (m, a) = LearningRepository.stats()
        mastered = m; avg = a
        due = LearningRepository.dueCount()
        val (t, c) = ExampleRepository.stats()
        total = t; correct = c
    }
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("学习报告", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background, titleContentColor = colors.text
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(colors.background).padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportStat("已掌握", "$mastered", colors, Modifier.weight(1f))
                ReportStat("平均掌握度", "${(avg * 100).toInt()}%", colors, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportStat("累计答题", "$total", colors, Modifier.weight(1f))
                ReportStat("正确率", if (total == 0) "—" else "${correct * 100 / total}%", colors, Modifier.weight(1f))
            }
            ReportStat("待复习知识点", "$due", colors, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Button(onClick = onReview, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("去复习")
            }
            OutlinedButton(onClick = onPractice, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("去练题")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "掌握度按答题表现与复习阶段粗算，后续会用更细的模型校准。",
                fontSize = 11.sp, color = colors.text3, lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun ReportStat(label: String, value: String, colors: com.mathkatex.verify.ui.theme.AppColors, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.line),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Spacer(Modifier.height(3.dp))
            Text(label, fontSize = 11.5.sp, color = colors.text3)
        }
    }
}
