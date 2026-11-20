package com.mathkatex.verify.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.data.local.KnowledgeEntity
import com.mathkatex.verify.data.local.KnowledgePathEntity
import com.mathkatex.verify.data.local.KnowledgeRepository
import com.mathkatex.verify.ui.theme.LocalAppColors

/** 专题路径：按初一/初二计算专项排列知识点，点开可以看到顺序并直接练。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathsScreen(onBack: () -> Unit = {}, onStartPractice: (String) -> Unit = {}) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var paths by remember { mutableStateOf<List<KnowledgePathEntity>>(emptyList()) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var nodes by remember { mutableStateOf<List<KnowledgeEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        paths = KnowledgeRepository.getPaths()
        loaded = true
    }
    LaunchedEffect(expanded) {
        nodes = expanded?.let { KnowledgeRepository.pathNodes(it) } ?: emptyList()
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("专题路径", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(colors.background).padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("按教材顺序排好的学习路线，建议从前往后推进。", fontSize = 12.sp, color = colors.text3,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
                items(paths, key = { it.id }) { path ->
                    val isOpen = expanded == path.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.line),
                    ) {
                        Column(Modifier.fillMaxWidth().clickable {
                            expanded = if (isOpen) null else path.id
                        }.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(path.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                                    Spacer(Modifier.height(3.dp))
                                    Text("${path.grade} · 先学后练", fontSize = 11.sp, color = colors.text3)
                                }
                                Icon(if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null, tint = colors.text3)
                            }
                            if (isOpen) {
                                Spacer(Modifier.height(10.dp))
                                if (nodes.isEmpty()) {
                                    Text("加载中…", fontSize = 12.sp, color = colors.text3)
                                } else {
                                    nodes.forEachIndexed { index, node ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("${index + 1}.", fontSize = 11.sp, color = colors.text3,
                                                modifier = Modifier.width(22.dp))
                                            Text(node.title, fontSize = 12.sp, color = colors.text, modifier = Modifier.weight(1f))
                                            TextButton(onClick = { onStartPractice(node.id) }) {
                                                Text("练", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
