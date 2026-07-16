package com.mathkatex.verify.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.data.local.KnowledgeEntity
import com.mathkatex.verify.data.local.KnowledgeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DetailTitleBlue = Color(0xFF1565C0)
private val CardLightGray = Color(0xFFF5F5F5)
private val DividerGray = Color(0xFFE0E0E0)

/**
 * 知识点速查页面
 * 
 * 修复 v2：全量加载数据库 → 内存中分组过滤，避免 LaunchedEffect 竞态 + SQL 查询字符串不匹配问题
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen() {
    val context = LocalContext.current

    // ── 全量数据状态（一次性加载）──
    var blocks by remember { mutableStateOf<List<String>>(emptyList()) }
    var allPoints by remember { mutableStateOf<List<KnowledgeEntity>>(emptyList()) }
    var selectedBlock by remember { mutableStateOf("") }
    var selectedLv1 by remember { mutableStateOf("") }
    var selectedLv2 by remember { mutableStateOf("") }
    var selectedLv2Detail by remember { mutableStateOf("") }
    var selectedKnowledge by remember { mutableStateOf<KnowledgeEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }

    // ── 内存分组数据（由 allPoints 派生）──
    val effectiveBlock = selectedBlock.ifEmpty {
        blocks.firstOrNull() ?: ""
    }
    val blockLv1Map = remember(allPoints, effectiveBlock) {
        if (allPoints.isEmpty()) emptyMap()
        else {
            val map = mutableMapOf<String, List<String>>()
            allPoints.filter { it.block == effectiveBlock }
                .groupBy { it.categoryLv1 }
                .forEach { (lv1, pts) ->
                    val lv2List = pts.map { it.categoryLv2 }.filter { it.isNotEmpty() }.distinct()
                    map[lv1] = lv2List
                }
            map
        }
    }

    val lv1List = remember(blockLv1Map) { blockLv1Map.keys.toList() }

    val currentLv2List = remember(selectedLv1, blockLv1Map) {
        if (selectedLv1.isEmpty()) emptyList()
        else blockLv1Map[selectedLv1] ?: emptyList()
    }

    val currentKnowledgeList = remember(allPoints, selectedBlock, selectedLv1, selectedLv2) {
        if (selectedLv2.isEmpty()) emptyList()
        else allPoints.filter {
            it.block == selectedBlock &&
            it.categoryLv1 == selectedLv1 &&
            it.categoryLv2 == selectedLv2
        }.sortedBy { it.id }
    }

    val lv2DetailPoints = remember(allPoints, selectedBlock, selectedLv1, selectedLv2Detail) {
        if (selectedLv2Detail.isEmpty()) emptyList()
        else allPoints.filter {
            it.block == selectedBlock &&
            it.categoryLv1 == selectedLv1 &&
            it.categoryLv2 == selectedLv2Detail
        }.sortedBy { it.id }
    }

    // ── 一次性全量加载 ──
    LaunchedEffect(retryCount) {
        isLoading = true
        errorMsg = ""
        try {
            if (!KnowledgeRepository.isReady.value) {
                Log.d("KnowledgeScreen", "Repository not ready, forcing reimport...")
                withContext(Dispatchers.IO) { KnowledgeRepository.forceReimport(context) }
                Log.d("KnowledgeScreen", "Reimport done, ready=${KnowledgeRepository.isReady.value}")
            }
            val loadedBlocks = withContext(Dispatchers.IO) { KnowledgeRepository.getBlocks() }
            val dbCount = withContext(Dispatchers.IO) { KnowledgeRepository.count() }
            Log.d("KnowledgeScreen", "blocks=$loadedBlocks dbCount=$dbCount")

            // 全量加载所有知识点
            val all = withContext(Dispatchers.IO) { KnowledgeRepository.getAll() }
            Log.d("KnowledgeScreen", "allPoints loaded=${all.size}")

            // 调试：检查第一条数据的 categoryLv2 是否为空
            if (all.isNotEmpty()) {
                val first = all.first()
                Log.d("KnowledgeScreen", "First point: block='${first.block}' lv1='${first.categoryLv1}' lv2='${first.categoryLv2}' title='${first.title}'")
                // 检查所有 categoryLv2 的分布
                val lv2Samples = all.map { it.categoryLv2 }.filter { it.isNotEmpty() }.distinct().take(10)
                Log.d("KnowledgeScreen", "Sample categoryLv2 values (non-empty): $lv2Samples")
                val emptyLv2Count = all.count { it.categoryLv2.isEmpty() }
                Log.d("KnowledgeScreen", "Points with empty categoryLv2: $emptyLv2Count / ${all.size}")
            }

            blocks = loadedBlocks
            allPoints = all
            if (loadedBlocks.isNotEmpty()) {
                selectedBlock = loadedBlocks[0]
                selectedLv1 = ""
                selectedLv2 = ""
            } else {
                errorMsg = "未找到知识点数据"
            }
        } catch (e: Exception) {
            Log.e("KnowledgeScreen", "Load failed", e)
            errorMsg = "加载失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // ── UI ──
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知识点速查", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> LoadingView(paddingValues, onRetry = { retryCount++ })
            errorMsg.isNotEmpty() -> ErrorView(errorMsg, paddingValues, onRetry = { errorMsg = ""; retryCount++ })
            selectedLv2Detail.isNotEmpty() && lv2DetailPoints.isNotEmpty() -> {
                BackHandler { selectedLv2Detail = "" }
                Lv2DetailListScreen(
                    lv2Name = selectedLv2Detail,
                    knowledgeList = lv2DetailPoints,
                    onBack = { selectedLv2Detail = "" },
                    onKnowledgeClick = { selectedKnowledge = it }
                )
            }
            selectedKnowledge != null -> {
                BackHandler { selectedKnowledge = null }
                KnowledgeDetailContent(
                    knowledge = selectedKnowledge!!,
                    onBack = { selectedKnowledge = null }
                )
            }
            else -> ContentView(
                paddingValues = paddingValues,
                blocks = blocks,
                selectedBlock = selectedBlock,
                onBlockSelected = { selectedBlock = it; selectedLv1 = ""; selectedLv2 = ""; selectedLv2Detail = "" },
                lv1List = lv1List,
                selectedLv1 = selectedLv1,
                onLv1Selected = { selectedLv1 = if (selectedLv1 == it) "" else it; selectedLv2 = ""; selectedLv2Detail = "" },
                lv2List = currentLv2List,
                selectedLv2 = selectedLv2,
                onLv2Selected = { selectedLv1 = selectedLv1; selectedLv2Detail = it },
                knowledgeList = currentKnowledgeList,
                onKnowledgeClick = { selectedKnowledge = it }
            )
        }
    }
}

// ── 子组件 ──

@Composable
private fun LoadingView(paddingValues: PaddingValues, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("正在加载知识点数据...")
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun ErrorView(msg: String, paddingValues: PaddingValues, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(msg, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("重试")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Lv2DetailListScreen(
    lv2Name: String,
    knowledgeList: List<KnowledgeEntity>,
    onBack: () -> Unit,
    onKnowledgeClick: (KnowledgeEntity) -> Unit
) {
    val fullMarkdown = remember(knowledgeList) {
        buildString {
            val sorted = knowledgeList.sortedBy { it.id }
            sorted.groupBy { it.categoryLv3.ifEmpty { it.title } }
                .forEach { (lv3Name, points) ->
                    appendLine("## **$lv3Name**")
                    appendLine()
                    points.forEach { point ->
                        append("### ${point.title}")
                        if (point.grade.isNotEmpty()) append("（${point.grade}）")
                        appendLine()
                        appendLine()
                        if (point.formula.isNotEmpty()) {
                            append("\$\$${point.formula}\$\$")
                            appendLine()
                            appendLine()
                        }
                        if (point.content.isNotEmpty()) {
                            appendLine(point.content)
                            appendLine()
                        }
                        appendLine("---")
                        appendLine()
                    }
                }
        }.trim()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lv2Name, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 17.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            Text("共 ${knowledgeList.size} 个知识点", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            MarkdownWebView(markdown = fullMarkdown, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ContentView(
    paddingValues: PaddingValues,
    blocks: List<String>,
    selectedBlock: String,
    onBlockSelected: (String) -> Unit,
    lv1List: List<String>,
    selectedLv1: String,
    onLv1Selected: (String) -> Unit,
    lv2List: List<String>,
    selectedLv2: String,
    onLv2Selected: (String) -> Unit,
    knowledgeList: List<KnowledgeEntity>,
    onKnowledgeClick: (KnowledgeEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(paddingValues)) {
        if (blocks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无知识点数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        // 板块 Tab
        ScrollableTabRow(
            selectedTabIndex = blocks.indexOf(selectedBlock).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 8.dp
        ) {
            blocks.forEach { block ->
                Tab(
                    selected = block == selectedBlock,
                    onClick = { onBlockSelected(block) },
                    text = { Text(block, fontSize = 13.sp, maxLines = 1) }
                )
            }
        }

        // Lv1 列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(lv1List) { _, lv1 ->
                val subCats = if (lv1 == selectedLv1) lv2List else emptyList()
                val kList = if (selectedLv2.isNotEmpty() && lv1 == selectedLv1) knowledgeList else emptyList()

                CategoryLv1Card(
                    title = lv1,
                    isExpanded = lv1 == selectedLv1,
                    subCategories = subCats,
                    onClick = { onLv1Selected(lv1) },
                    onLv2Click = { onLv2Selected(it) }
                )
            }
        }
    }
}

// ── 以下卡片组件与之前保持一致 ──

@Composable
private fun CategoryLv1Card(
    title: String,
    isExpanded: Boolean,
    subCategories: List<String>,
    onClick: () -> Unit,
    onLv2Click: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 1.dp else 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
            if (isExpanded) {
                Column(modifier = Modifier.padding(start = 32.dp, end = 12.dp, bottom = 8.dp)) {
                    if (subCategories.isEmpty()) {
                        Text("该分类暂无子分类", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }
                    subCategories.forEach { lv2 ->
                        Lv2Section(
                            lv2 = lv2, isSelected = false,
                            onClick = { onLv2Click(lv2) },
                            knowledgeList = emptyList(),
                            onKnowledgeClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Lv2Section(
    lv2: String, isSelected: Boolean, onClick: () -> Unit,
    knowledgeList: List<KnowledgeEntity>, onKnowledgeClick: (KnowledgeEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(lv2, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KnowledgeItemCard(knowledge: KnowledgeEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Science, contentDescription = null,
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(knowledge.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (knowledge.formula.isNotEmpty()) {
                    Text(knowledge.formula, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(knowledge.grade, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnowledgeDetailContent(knowledge: KnowledgeEntity, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(knowledge.title, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 17.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(knowledge.fullPath, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            if (knowledge.formula.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardLightGray),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    MarkdownWebView(
                        markdown = "$$${knowledge.formula}$$",
                        modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(min = 60.dp, max = 300.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (knowledge.content.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("详细说明", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = DetailTitleBlue)
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = DividerGray, thickness = 1.dp)
                        Spacer(Modifier.height(8.dp))
                        MarkdownWebView(
                            markdown = knowledge.content,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 800.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(onClick = {}, label = { Text(knowledge.block, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Category, null, modifier = Modifier.size(16.dp)) })
                AssistChip(onClick = {}, label = { Text(knowledge.grade.ifEmpty { "通用" }, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp)) })
            }
        }
    }
}

