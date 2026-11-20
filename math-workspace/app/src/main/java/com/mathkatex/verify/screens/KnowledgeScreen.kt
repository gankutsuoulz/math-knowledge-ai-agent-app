package com.mathkatex.verify.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mathkatex.verify.data.local.ExampleEntity
import com.mathkatex.verify.data.local.LearningRepository
import com.mathkatex.verify.data.local.MasteryEntity
import com.mathkatex.verify.data.local.KnowledgeEntity
import com.mathkatex.verify.data.local.KnowledgeRepository
import com.mathkatex.verify.ui.theme.AppColors
import com.mathkatex.verify.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 知识点速查页面
 * 
 * 修复 v2：全量加载数据库 → 内存中分组过滤，避免 LaunchedEffect 竞态 + SQL 查询字符串不匹配问题
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(onStartPractice: (String) -> Unit = {}) {
    val context = LocalContext.current

    // ── 全量数据状态（一次性加载）──
    var blocks by remember { mutableStateOf<List<String>>(emptyList()) }
    var allPoints by remember { mutableStateOf<List<KnowledgeEntity>>(emptyList()) }
    var selectedBlock by remember { mutableStateOf("") }
    var selectedLv1 by remember { mutableStateOf("") }
    var selectedLv2 by remember { mutableStateOf("") }
    var selectedLv2Detail by remember { mutableStateOf("") }
    var selectedKnowledge by remember { mutableStateOf<KnowledgeEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
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
    val lv1Counts = remember(allPoints) {
        allPoints.groupBy { it.categoryLv1 }.mapValues { it.value.size }
    }

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
                withContext(Dispatchers.IO) { KnowledgeRepository.forceReimport(context) }
            }
            val loadedBlocks = withContext(Dispatchers.IO) { KnowledgeRepository.getBlocks() }
            val dbCount = withContext(Dispatchers.IO) { KnowledgeRepository.count() }

            // 全量加载所有知识点
            val all = withContext(Dispatchers.IO) { KnowledgeRepository.getAll() }

            // 调试：检查第一条数据的 categoryLv2 是否为空
            if (all.isNotEmpty()) {
                val first = all.first()
                // 检查所有 categoryLv2 的分布
                val lv2Samples = all.map { it.categoryLv2 }.filter { it.isNotEmpty() }.distinct().take(10)
                Log.d("KnowledgeScreen", "Sample categoryLv2 values (non-empty): $lv2Samples")
                val emptyLv2Count = all.count { it.categoryLv2.isEmpty() }
            }

            val preferredOrder = listOf("数与代数", "图形与几何", "统计与概率", "综合与实践")
            val sortedBlocks = loadedBlocks.sortedBy { block ->
                val index = preferredOrder.indexOf(block)
                if (index < 0) preferredOrder.size else index
            }
            blocks = sortedBlocks
            allPoints = all
            if (sortedBlocks.isNotEmpty()) {
                selectedBlock = sortedBlocks[0]
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
    val colors = LocalAppColors.current
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("知识点", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> LoadingView(paddingValues, onRetry = { retryCount++ })
            errorMsg.isNotEmpty() -> ErrorView(errorMsg, paddingValues, onRetry = { errorMsg = ""; retryCount++ })
            selectedKnowledge != null -> {
                BackHandler { selectedKnowledge = null }
                KnowledgeDetailContent(
                    knowledge = selectedKnowledge!!,
                    onBack = { selectedKnowledge = null },
                    onStartPractice = onStartPractice,
                    onOpenKnowledge = { selectedKnowledge = it }
                )
            }
            selectedLv2Detail.isNotEmpty() && lv2DetailPoints.isNotEmpty() -> {
                BackHandler { selectedLv2Detail = "" }
                Lv2DetailListScreen(
                    lv2Name = selectedLv2Detail,
                    knowledgeList = lv2DetailPoints,
                    onBack = { selectedLv2Detail = "" },
                    onKnowledgeClick = { selectedKnowledge = it }
                )
            }
            else -> ContentView(
                paddingValues = paddingValues,
                blocks = blocks,
                selectedBlock = selectedBlock,
                onBlockSelected = { selectedBlock = it; selectedLv1 = ""; selectedLv2 = ""; selectedLv2Detail = "" },
                lv1List = lv1List,
                lv1Counts = lv1Counts,
                selectedLv1 = selectedLv1,
                onLv1Selected = { selectedLv1 = if (selectedLv1 == it) "" else it; selectedLv2 = ""; selectedLv2Detail = "" },
                lv2List = currentLv2List,
                selectedLv2 = selectedLv2,
                onLv2Selected = { selectedLv1 = selectedLv1; selectedLv2Detail = it },
                knowledgeList = currentKnowledgeList,
                allPoints = allPoints,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onKnowledgeClick = { selectedKnowledge = it }
            )
        }
    }
}

/** 板块 Tab 上用短名，底层过滤仍用完整名。 */
private fun shortBlockName(block: String): String = when {
    block.contains("代数") -> "代数"
    block.contains("几何") -> "几何"
    block.contains("统计") -> "统计"
    block.contains("综合") -> "综合"
    else -> block
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
    val colors = LocalAppColors.current
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(lv2Name, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    "共 ${knowledgeList.size} 个知识点",
                    fontSize = 12.sp,
                    color = colors.text3,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            items(knowledgeList.sortedBy { it.id }, key = { it.id }) { knowledge ->
                KnowledgeItemCard(knowledge = knowledge, onClick = { onKnowledgeClick(knowledge) })
            }
            item { Spacer(Modifier.height(24.dp)) }
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
    lv1Counts: Map<String, Int>,
    selectedLv1: String,
    onLv1Selected: (String) -> Unit,
    lv2List: List<String>,
    selectedLv2: String,
    onLv2Selected: (String) -> Unit,
    knowledgeList: List<KnowledgeEntity>,
    allPoints: List<KnowledgeEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onKnowledgeClick: (KnowledgeEntity) -> Unit
) {
    val colors = LocalAppColors.current
    Column(Modifier.fillMaxSize().background(colors.background).padding(paddingValues)) {
        if (blocks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无知识点数据", color = colors.text3)
            }
            return
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            placeholder = { Text("搜索知识点、公式", fontSize = 13.sp, color = colors.text3) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = colors.text3, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.line,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
            ),
        )

        if (searchQuery.isNotBlank()) {
            val results = remember(searchQuery, allPoints) {
                val q = searchQuery.trim()
                allPoints.filter {
                    it.title.contains(q, ignoreCase = true) ||
                        it.fullPath.contains(q, ignoreCase = true) ||
                        it.formula.contains(q, ignoreCase = true)
                }.take(60)
            }
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到「${searchQuery.trim()}」", color = colors.text3, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(results) { knowledge ->
                        KnowledgeItemCard(knowledge) { onKnowledgeClick(knowledge) }
                    }
                }
            }
        } else {
        // 板块切换：胶囊按钮，显示短名，底层仍用完整板块名过滤
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            blocks.forEach { block ->
                val selected = block == selectedBlock
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) colors.primary else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (selected) colors.primary else colors.line,
                            shape = RoundedCornerShape(999.dp),
                        )
                        .clickable { onBlockSelected(block) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        shortBlockName(block),
                        fontSize = 12.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else colors.text2,
                        maxLines = 1,
                    )
                }
            }
        }

        // Lv1 列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(lv1List) { _, lv1 ->
                val subCats = if (lv1 == selectedLv1) lv2List else emptyList()
                val kList = if (selectedLv2.isNotEmpty() && lv1 == selectedLv1) knowledgeList else emptyList()

                CategoryLv1Card(
                    title = lv1,
                    count = lv1Counts[lv1] ?: 0,
                    isExpanded = lv1 == selectedLv1,
                    subCategories = subCats,
                    onClick = { onLv1Selected(lv1) },
                    onLv2Click = { onLv2Selected(it) }
                )
            }
        }
        }
    }
}

// ── 以下卡片组件与之前保持一致 ──

@Composable
private fun CategoryLv1Card(
    title: String,
    count: Int,
    isExpanded: Boolean,
    subCategories: List<String>,
    onClick: () -> Unit,
    onLv2Click: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) colors.primarySoft else colors.surface
        ),
        border = BorderStroke(1.dp, colors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title.take(1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.text)
                    Spacer(Modifier.height(2.dp))
                    Text("$count 个知识点", fontSize = 11.sp, color = colors.text3)
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.text3,
                )
            }
            if (isExpanded) {
                Column(modifier = Modifier.padding(start = 32.dp, end = 12.dp, bottom = 8.dp)) {
                    if (subCategories.isEmpty()) {
                        Text("该分类暂无子分类", fontSize = 12.sp,
                            color = colors.text3,
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
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface2),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = null, modifier = Modifier.size(18.dp),
                tint = colors.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(lv2, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = colors.text, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null, modifier = Modifier.size(16.dp),
                tint = colors.text3
            )
        }
    }
}

@Composable
private fun KnowledgeItemCard(knowledge: KnowledgeEntity, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Science, contentDescription = null,
                modifier = Modifier.size(18.dp), tint = colors.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(knowledge.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                if (knowledge.formula.isNotEmpty()) {
                    Text(knowledge.formula, fontSize = 12.sp, color = colors.text3)
                }
            }
            Text(knowledge.grade, fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnowledgeDetailContent(
    knowledge: KnowledgeEntity,
    onBack: () -> Unit,
    onStartPractice: (String) -> Unit = {},
    onOpenKnowledge: (KnowledgeEntity) -> Unit = {},
) {
    val colors = LocalAppColors.current
    var examples by remember(knowledge.id) { mutableStateOf<List<ExampleEntity>>(emptyList()) }
    var prerequisites by remember(knowledge.id) { mutableStateOf<List<KnowledgeEntity>>(emptyList()) }
    var related by remember(knowledge.id) { mutableStateOf<List<KnowledgeEntity>>(emptyList()) }
    var models by remember(knowledge.id) { mutableStateOf<List<String>>(emptyList()) }
    var mastery by remember(knowledge.id) { mutableStateOf<MasteryEntity?>(null) }
    LaunchedEffect(knowledge.id) {
        mastery = LearningRepository.mastery(knowledge.id)
        examples = KnowledgeRepository.examplesFor(knowledge.id)
        prerequisites = KnowledgeRepository.prerequisitesOf(knowledge.id)
        related = KnowledgeRepository.relatedOf(knowledge.id)
        models = KnowledgeRepository.modelsOf(knowledge.id)
    }
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(knowledge.title, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 17.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(colors.background).padding(paddingValues).padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(knowledge.fullPath, fontSize = 12.sp, color = colors.text3)
            Spacer(Modifier.height(12.dp))

            if (knowledge.formula.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface2),
                    border = BorderStroke(1.dp, colors.line),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    MarkdownWebView(
                        markdown = "$$${knowledge.formula}$$",
                        modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(min = 60.dp, max = 300.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (knowledge.content.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.line),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("详细说明", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.primary)
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = colors.line, thickness = 1.dp)
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
                if (knowledge.abilityLevel.isNotEmpty()) {
                    AssistChip(onClick = {}, label = { Text(knowledge.abilityLevel, fontSize = 12.sp) })
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("掌握度", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.text)
            Spacer(Modifier.height(6.dp))
            val m = mastery
            LinearProgressIndicator(
                progress = { m?.score ?: 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                color = colors.primary,
                trackColor = colors.surface2,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                if (m == null || m.attempts == 0) "还没练过，点下面「练这个知识点」试试"
                else "掌握度 ${(m.score * 100).toInt()}% · 已练 ${m.attempts} 次 · 复习阶段 ${m.stage + 1}",
                fontSize = 11.sp, color = colors.text3,
            )

            if (models.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("常考题型", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.text)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    models.take(6).forEach { m ->
                        AssistChip(onClick = {}, label = { Text(m, fontSize = 11.5.sp) })
                    }
                }
            }

            if (prerequisites.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("前置知识", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.text)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prerequisites.take(8).forEach { p ->
                        AssistChip(onClick = { onOpenKnowledge(p) }, label = { Text(p.title, fontSize = 11.5.sp) })
                    }
                }
            }

            if (related.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("相关知识点", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.text)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    related.take(8).forEach { r ->
                        AssistChip(onClick = { onOpenKnowledge(r) }, label = { Text(r.title, fontSize = 11.5.sp) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.line),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("相关例题", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.text)
                    Spacer(Modifier.height(8.dp))
                    if (examples.isEmpty()) {
                        Text("还没有例题，先去练一练吧", fontSize = 12.sp, color = colors.text3)
                    } else {
                        examples.take(3).forEach { ex ->
                            Text(
                                text = "· " + ex.question.replace(Regex("\\$[^$]*\\$"), "").replace(Regex("[#*`]"), "").take(60),
                                fontSize = 12.sp, color = colors.text2, lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onStartPractice(knowledge.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("练这个知识点", fontSize = 13.sp) }
                }
            }
        }
    }
}

