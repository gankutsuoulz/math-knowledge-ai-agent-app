package com.mathkatex.verify.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mathkatex.verify.data.local.LearningRepository
import com.mathkatex.verify.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 今日复习：按知识点给出到期队列，点进去就是该知识点的练习。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(onBack: () -> Unit = {}, onStartPractice: (String) -> Unit = {}) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<Pair<com.mathkatex.verify.data.local.KnowledgeEntity, com.mathkatex.verify.data.local.ReviewItemEntity>>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        LearningRepository.init(context)
        items = LearningRepository.dueReviews()
        loaded = true
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("今日复习", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text,
                )
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("今天没有到期的复习", fontSize = 15.sp, color = colors.text)
                    Spacer(Modifier.height(6.dp))
                    Text("去做几道新题，系统会按掌握度安排复习", fontSize = 12.sp, color = colors.text3)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(colors.background).padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "共 ${items.size} 个知识点到期，先复习再刷新题效果更好。",
                        fontSize = 12.sp, color = colors.text2, modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(items, key = { it.first.id }) { (knowledge, review) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.line),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(knowledge.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                                Spacer(Modifier.height(4.dp))
                                val dueText = SimpleDateFormat("MM-dd", Locale.CHINA).format(Date(review.dueAt))
                                Text("复习阶段 ${review.stage + 1} · 到期 $dueText", fontSize = 11.sp, color = colors.text3)
                            }
                            TextButton(onClick = { onStartPractice(knowledge.id) }) {
                                Text("去复习", fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
