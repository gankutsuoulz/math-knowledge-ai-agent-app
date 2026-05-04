package com.mathkatex.verify.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Data Models
// ============================================================================

data class LibraryTopic(
    val id: String,
    val name: String,
    val icon: String,
    val category: String // "代数" "几何" "函数" etc.
)

data class LibraryQuestion(
    val id: String,
    val topicId: String,
    val topicName: String,
    val title: String,
    val difficulty: String, // "easy" "medium" "hard"
    val status: QuestionStatus, // UN_STARTED, IN_PROGRESS, COMPLETED
    val latex: String
)

enum class QuestionStatus {
    UN_STARTED, IN_PROGRESS, COMPLETED
}

data class LibraryStats(
    val totalQuestions: Int,
    val completedCount: Int,
    val inProgressCount: Int
) {
    val remainingCount: Int get() = totalQuestions - completedCount - inProgressCount
    val completionRate: Float get() = if (totalQuestions > 0) completedCount.toFloat() / totalQuestions else 0f
}

// ============================================================================
// Sample Data
// ============================================================================

private val libraryTopics = listOf(
    LibraryTopic("algebra_eq1", "一元一次方程", "🔢", "代数"),
    LibraryTopic("algebra_eq2", "一元二次方程", "📐", "代数"),
    LibraryTopic("algebra_poly", "多项式运算", "📊", "代数"),
    LibraryTopic("geometry_basic", "平面几何基础", "📐", "几何"),
    LibraryTopic("geometry_triangle", "三角形", "🔺", "几何"),
    LibraryTopic("geometry_circle", "圆", "⭕", "几何"),
    LibraryTopic("function_linear", "一次函数", "📈", "函数"),
    LibraryTopic("function_quad", "二次函数", "📊", "函数"),
    LibraryTopic("prob_basic", "概率初步", "🎲", "概率统计"),
    LibraryTopic("stat_basic", "统计初步", "📊", "概率统计"),
    LibraryTopic("number_theory", "数论基础", "🔢", "数论"),
    LibraryTopic("fraction_ops", "分数运算", "➗", "数论")
)

private fun generateSampleQuestions(): List<LibraryQuestion> {
    val questions = mutableListOf<LibraryQuestion>()
    
    // 一元一次方程
    questions.addAll(listOf(
        LibraryQuestion("lq001", "algebra_eq1", "一元一次方程", "求解：2x+5=13", "easy", QuestionStatus.COMPLETED, "2x+5=13"),
        LibraryQuestion("lq002", "algebra_eq1", "一元一次方程", "求解：3(x-2)=12", "medium", QuestionStatus.COMPLETED, "3(x-2)=12"),
        LibraryQuestion("lq003", "algebra_eq1", "一元一次方程", "求解：5x-7=3x+1", "medium", QuestionStatus.IN_PROGRESS, "5x-7=3x+1"),
        LibraryQuestion("lq004", "algebra_eq1", "一元一次方程", "求解：2(3x+1)=4(x-1)", "hard", QuestionStatus.UN_STARTED, "2(3x+1)=4(x-1)")
    ))
    
    // 一元二次方程
    questions.addAll(listOf(
        LibraryQuestion("lq005", "algebra_eq2", "一元二次方程", "求解：x²-5x+6=0", "medium", QuestionStatus.COMPLETED, "x^2-5x+6=0"),
        LibraryQuestion("lq006", "algebra_eq2", "一元二次方程", "求解：2x²+4x-6=0", "hard", QuestionStatus.IN_PROGRESS, "2x^2+4x-6=0"),
        LibraryQuestion("lq007", "algebra_eq2", "一元二次方程", "求解：x²+6x+9=0", "easy", QuestionStatus.COMPLETED, "x^2+6x+9=0")
    ))
    
    // 平面几何基础
    questions.addAll(listOf(
        LibraryQuestion("lq008", "geometry_basic", "平面几何基础", "三角形内角和是多少？", "easy", QuestionStatus.COMPLETED, "\\angle A+\\angle B+\\angle C=180^\\circ"),
        LibraryQuestion("lq009", "geometry_basic", "平面几何基础", "等腰三角形顶角40°，求底角", "medium", QuestionStatus.UN_STARTED, "\\text{底角}=?"),
        LibraryQuestion("lq010", "geometry_basic", "平面几何基础", "直角三角形两锐角互余", "easy", QuestionStatus.COMPLETED, "\\angle A + \\angle B = 90^\\circ")
    ))
    
    // 三角形
    questions.addAll(listOf(
        LibraryQuestion("lq011", "geometry_triangle", "三角形", "△ABC中DE∥BC，AD:DB=1:2", "medium", QuestionStatus.IN_PROGRESS, "AE:EC=?"),
        LibraryQuestion("lq012", "geometry_triangle", "三角形", "勾股定理：a²+b²=c²", "easy", QuestionStatus.COMPLETED, "a^2+b^2=c^2")
    ))
    
    // 一次函数
    questions.addAll(listOf(
        LibraryQuestion("lq013", "function_linear", "一次函数", "y=2x+1，x=3时y=?", "easy", QuestionStatus.COMPLETED, "y=2(3)+1=?"),
        LibraryQuestion("lq014", "function_linear", "一次函数", "过(1,3)和(2,5)求解析式", "medium", QuestionStatus.IN_PROGRESS, "y=kx+b"),
        LibraryQuestion("lq015", "function_linear", "一次函数", "y=-x+2与x轴交点", "easy", QuestionStatus.UN_STARTED, "y=0\\Rightarrow x=?")
    ))
    
    // 二次函数
    questions.addAll(listOf(
        LibraryQuestion("lq016", "function_quad", "二次函数", "求y=x²-4x+3顶点", "medium", QuestionStatus.UN_STARTED, "y=(x-2)^2-1"),
        LibraryQuestion("lq017", "function_quad", "二次函数", "求y=2x²-8x+1开口方向", "easy", QuestionStatus.COMPLETED, "a=2>0")
    ))
    
    // 概率初步
    questions.addAll(listOf(
        LibraryQuestion("lq018", "prob_basic", "概率初步", "抛硬币正面概率", "easy", QuestionStatus.COMPLETED, "P=\\frac{1}{2}"),
        LibraryQuestion("lq019", "prob_basic", "概率初步", "3红2白摸球概率", "medium", QuestionStatus.UN_STARTED, "P(\\text{红})=\\frac{3}{5}")
    ))
    
    // 统计初步
    questions.addAll(listOf(
        LibraryQuestion("lq020", "stat_basic", "统计初步", "平均数计算", "easy", QuestionStatus.IN_PROGRESS, "\\bar{x}=\\frac{\\sum x_i}{n}")
    ))
    
    // 数论基础
    questions.addAll(listOf(
        LibraryQuestion("lq021", "number_theory", "数论基础", "求12和18的最大公约数", "medium", QuestionStatus.COMPLETED, "GCD(12,18)=6"),
        LibraryQuestion("lq022", "number_theory", "数论基础", "求最小公倍数", "medium", QuestionStatus.UN_STARTED, "LCM(12,18)=36")
    ))
    
    // 分数运算
    questions.addAll(listOf(
        LibraryQuestion("lq023", "fraction_ops", "分数运算", "1/2 + 1/3 = ?", "easy", QuestionStatus.COMPLETED, "\\frac{1}{2}+\\frac{1}{3}=\\frac{5}{6}"),
        LibraryQuestion("lq024", "fraction_ops", "分数运算", "2/3 × 4/5 = ?", "easy", QuestionStatus.COMPLETED, "\\frac{2}{3}\\times\\frac{4}{5}=\\frac{8}{15}")
    ))
    
    return questions
}

private val allQuestions = generateSampleQuestions()

private fun calculateStats(): LibraryStats {
    return LibraryStats(
        totalQuestions = allQuestions.size,
        completedCount = allQuestions.count { it.status == QuestionStatus.COMPLETED },
        inProgressCount = allQuestions.count { it.status == QuestionStatus.IN_PROGRESS }
    )
}

// ============================================================================
// Main Screen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    var stats by remember { mutableStateOf(calculateStats()) }
    var expandedTopicId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    // Filter questions by search query
    val filteredQuestions = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allQuestions
        } else {
            allQuestions.filter { 
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.topicName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Group questions by topic for display
    val questionsByTopic = remember(filteredQuestions) {
        filteredQuestions.groupBy { it.topicId }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("例题库管理")
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF2196F3).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "F-04",
                                fontSize = 11.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF333333)
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Statistics Cards Section
            StatisticsSection(stats = stats)
            
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Topic Grid or Expanded List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show expanded topic's questions or topic grid
                if (expandedTopicId != null) {
                    // Expanded view - show questions for selected topic
                    val topic = libraryTopics.find { it.id == expandedTopicId }
                    val topicQuestions = questionsByTopic[expandedTopicId] ?: emptyList()
                    
                    item {
                        ExpandedTopicSection(
                            topic = topic!!,
                            questions = topicQuestions,
                            onCollapse = { expandedTopicId = null }
                        )
                    }
                } else {
                    // Grid view - show all topics with counts
                    items(libraryTopics.chunked(2)) { rowTopics ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTopics.forEach { topic ->
                                val topicQuestions = questionsByTopic[topic.id] ?: emptyList()
                                TopicGridCard(
                                    topic = topic,
                                    questionCount = topicQuestions.size,
                                    completedCount = topicQuestions.count { it.status == QuestionStatus.COMPLETED },
                                    onClick = { expandedTopicId = topic.id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty space if odd number
                            if (rowTopics.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        // Import Dialog
        if (showImportDialog) {
            ImportConfirmDialog(
                onConfirm = {
                    // Simulated import - update stats
                    stats = stats.copy(
                        totalQuestions = stats.totalQuestions + 5,
                        inProgressCount = stats.inProgressCount + 5
                    )
                    showImportDialog = false
                },
                onDismiss = { showImportDialog = false }
            )
        }
        
        // Export Dialog
        if (showExportDialog) {
            ExportConfirmDialog(
                onConfirm = {
                    // Simulated export
                    showExportDialog = false
                },
                onDismiss = { showExportDialog = false }
            )
        }
    }
    
    // Floating Action Buttons
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showExportDialog = true },
                containerColor = Color(0xFF4CAF50)
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "导出题库",
                    tint = Color.White
                )
            }
            FloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = Color(0xFF2196F3)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "导入题库",
                    tint = Color.White
                )
            }
        }
    }
}

// ============================================================================
// Statistics Section
// ============================================================================

@Composable
private fun StatisticsSection(stats: LibraryStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            label = "总题数",
            value = "${stats.totalQuestions}",
            icon = "📚",
            color = Color(0xFF2196F3)
        )
        StatCard(
            label = "已完成",
            value = "${stats.completedCount}",
            icon = "✅",
            color = Color(0xFF4CAF50)
        )
        StatCard(
            label = "未完成",
            value = "${stats.remainingCount}",
            icon = "📝",
            color = Color(0xFFFF9800)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF888888)
        )
    }
}

// ============================================================================
// Search Bar
// ============================================================================

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜索题库...", fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = Color(0xFF888888)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = Color(0xFF888888)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2196F3),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}

// ============================================================================
// Topic Grid Card
// ============================================================================

@Composable
private fun TopicGridCard(
    topic: LibraryTopic,
    questionCount: Int,
    completedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = topic.icon,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = topic.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF2196F3).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${questionCount}题",
                        fontSize = 11.sp,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (completedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${completedCount}完成",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "查看",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================
// Expanded Topic Section
// ============================================================================

@Composable
private fun ExpandedTopicSection(
    topic: LibraryTopic,
    questions: List<LibraryQuestion>,
    onCollapse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color(0xFF666666)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = topic.icon,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${questions.size}道题目",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFEEEEEE)
            )
            
            // Question list
            if (questions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无相关题目",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }
            } else {
                questions.forEachIndexed { index, question ->
                    QuestionItem(question = question)
                    if (index < questions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFEEEEEE)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Question Item
// ============================================================================

@Composable
private fun QuestionItem(question: LibraryQuestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = when (question.status) {
                        QuestionStatus.COMPLETED -> Color(0xFF4CAF50)
                        QuestionStatus.IN_PROGRESS -> Color(0xFFFF9800)
                        QuestionStatus.UN_STARTED -> Color(0xFFBDBDBD)
                    },
                    shape = RoundedCornerShape(4.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Question content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = question.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty badge
                DifficultyChip(difficulty = question.difficulty)
                // Status chip
                StatusChip(status = question.status)
            }
        }
        
        // LaTeX preview
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 40.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                .padding(4.dp)
        ) {
            KaTeXWebView(
                latex = question.latex,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: String) {
    val (color, text) = when (difficulty) {
        "easy" -> Pair(Color(0xFF4CAF50), "简单")
        "medium" -> Pair(Color(0xFFFF9800), "中等")
        "hard" -> Pair(Color(0xFFF44336), "困难")
        else -> Pair(Color(0xFF9E9E9E), "未知")
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusChip(status: QuestionStatus) {
    val (color, text, icon) = when (status) {
        QuestionStatus.COMPLETED -> Triple(Color(0xFF4CAF50), "已完成", "✓")
        QuestionStatus.IN_PROGRESS -> Triple(Color(0xFFFF9800), "进行中", "▶")
        QuestionStatus.UN_STARTED -> Triple(Color(0xFF9E9E9E), "未开始", "○")
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 10.sp,
                color = color
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                color = color
            )
        }
    }
}

// ============================================================================
// Import/Export Dialogs
// ============================================================================

@Composable
private fun ImportConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = Color(0xFF2196F3)
            )
        },
        title = {
            Text("导入题库")
        },
        text = {
            Text("确定要从外部文件导入题库吗？这将添加新的题目到现有题库中。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认导入", color = Color(0xFF2196F3))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun ExportConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
        },
        title = {
            Text("导出题库")
        },
        text = {
            Text("确定要导出当前题库吗？导出的文件可用于备份或分享。\n\n包含 ${allQuestions.size} 道题目。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认导出", color = Color(0xFF4CAF50))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF666666))
            }
        }
    )
}