package com.mathkatex.verify.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

enum class ErrorCategory(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CALCULATION("计算错误", Icons.Default.Calculate),
    CONCEPT("概念错误", Icons.Default.Lightbulb),
    METHOD("方法错误", Icons.Default.Route)
}

data class CommonError(
    val id: String,
    val category: ErrorCategory,
    val title: String,
    val example: String,
    val wrongAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val guidance: List<String>
)

data class AnalysisResult(
    val input: String,
    val detectedCategory: ErrorCategory,
    val detectedType: String,
    val steps: List<AnalysisStep>,
    val guidance: List<String>
)

data class AnalysisStep(
    val stepNumber: Int,
    val title: String,
    val content: String,
    val isError: Boolean = false,
    val suggestion: String? = null
)

// ============================================================================
// Common Errors Database
// ============================================================================

private val commonErrors = listOf(
    // 计算错误
    CommonError(
        id = "calc_001",
        category = ErrorCategory.CALCULATION,
        title = "去括号时符号错误",
        example = "2(x + 3) = 2x + 3",
        wrongAnswer = "2x + 3",
        correctAnswer = "2x + 6",
        explanation = "去括号时，每一项都要乘以括号外的系数，漏掉了第二项。",
        guidance = listOf(
            "去括号时，用括号外的系数分别乘以括号内的每一项",
            "不要只乘第一项就结束",
            "可以用分配律验证：2×x + 2×3 = 2x + 6"
        )
    ),
    CommonError(
        id = "calc_002",
        category = ErrorCategory.CALCULATION,
        title = "移项时符号不变",
        example = "x + 5 = 12 → x = 12 + 5",
        wrongAnswer = "x = 17",
        correctAnswer = "x = 7",
        explanation = "移项时要变号，加5移到另一边应变成减5。",
        guidance = listOf(
            "移项就是等式两边同时加减同一个数",
            "从左边移到右边要变号（+变-，-变+）",
            "或者记住：x = 12 - 5 = 7"
        )
    ),
    CommonError(
        id = "calc_003",
        category = ErrorCategory.CALCULATION,
        title = "分数通分错误",
        example = "1/2 + 1/3 = 1/5",
        wrongAnswer = "1/5",
        correctAnswer = "5/6",
        explanation = "分数加法需要先通分再分子相加，不是分母直接相加。",
        guidance = listOf(
            "同分母分数相加：分子相加，分母不变",
            "异分母分数相加：先找最小公倍数作为通分后的分母",
            "1/2 + 1/3 → 通分后 = 3/6 + 2/6 = 5/6"
        )
    ),
    CommonError(
        id = "calc_004",
        category = ErrorCategory.CALCULATION,
        title = "幂运算错误",
        example = "(x²)³ = x⁵",
        wrongAnswer = "x⁵",
        correctAnswer = "x⁶",
        explanation = "幂的乘方应该指数相乘，不是相加。",
        guidance = listOf(
            "(aᵐ)ⁿ = aᵐⁿ（同底数幂相乘，指数相乘）",
            "(x²)³ = x²ˣ³ = x⁶",
            "可以用展开验证：x²·x²·x² = x⁶"
        )
    ),
    CommonError(
        id = "calc_005",
        category = ErrorCategory.CALCULATION,
        title = "负数平方错误",
        example = "(-3)² = -9",
        wrongAnswer = "-9",
        correctAnswer = "9",
        explanation = "负数的平方是正数，因为负负得正。",
        guidance = listOf(
            "(-a)² = a²（任何数的平方都是非负数）",
            "(-3)² = (-3)×(-3) = 9",
            "注意区别：-3² = -(3²) = -9，但(-3)² = 9"
        )
    ),
    // 概念错误
    CommonError(
        id = "conc_001",
        category = ErrorCategory.CONCEPT,
        title = "混淆方程与等式",
        example = "认为 x + 2 就是一个方程",
        wrongAnswer = "x + 2 = 方程",
        correctAnswer = "x + 2 = ? 需有等号才是方程",
        explanation = "方程必须是含有未知数的等式，需要有等号。",
        guidance = listOf(
            "方程定义：含有未知数的等式",
            "必须有等号才能叫方程",
            "x + 2 只是代数式，不是方程；x + 2 = 5 才是方程"
        )
    ),
    CommonError(
        id = "conc_002",
        category = ErrorCategory.CONCEPT,
        title = "不懂因式分解的目的",
        example = "x² - 4 = (x - 2)(x + 2) 后停止",
        wrongAnswer = "直接写出因式分解结果",
        correctAnswer = "需要进一步求解 x = ±2",
        explanation = "因式分解是为了解方程或化简，需要知道因式分解后如何用。",
        guidance = listOf(
            "因式分解是手段，不是目的",
            "分解后令每个因式为0来求方程的解",
            "x² - 4 = (x-2)(x+2) = 0 → x = 2 或 x = -2"
        )
    ),
    CommonError(
        id = "conc_003",
        category = ErrorCategory.CONCEPT,
        title = "函数与方程混淆",
        example = "y = 2x + 1，认为 x 是 y 的函数",
        wrongAnswer = "x 是 y 的函数",
        correctAnswer = "y 是 x 的函数",
        explanation = "函数关系中，y 随 x 变化，所以 y 是 x 的函数。",
        guidance = listOf(
            "函数定义：对于每一个 x 的值，有唯一确定的 y 值",
            "y 随 x 变化，所以 y 是 x 的函数",
            "x 是自变量，y 是因变量（函数值）"
        )
    ),
    CommonError(
        id = "conc_004",
        category = ErrorCategory.CONCEPT,
        title = "平行线概念不清",
        example = "认为相交的两条线也有平行线",
        wrongAnswer = "两条相交的线",
        correctAnswer = "同一平面内不相交的两条直线",
        explanation = "平行线必须在同一平面内且永不相交。",
        guidance = listOf(
            "平行线定义：同一平面内不相交的两条直线",
            "关键要素：①同一平面 ②永不相交",
            "相交的两条线不可能平行"
        )
    ),
    // 方法错误
    CommonError(
        id = "meth_001",
        category = ErrorCategory.METHOD,
        title = "解方程跳步",
        example = "3x + 7 = 22，直接写 x = 5",
        wrongAnswer = "x = 5（跳步）",
        correctAnswer = "x = 5（需先移项：3x = 15）",
        explanation = "解方程需要清晰的步骤，跳步容易出错。",
        guidance = listOf(
            "解方程的标准步骤：去分母→去括号→移项→合并→系数化为1",
            "每一步都要写清楚等式变换",
            "完整过程：3x + 7 = 22 → 3x = 15 → x = 5"
        )
    ),
    CommonError(
        id = "meth_002",
        category = ErrorCategory.METHOD,
        title = "几何证明缺少条件",
        example = "证明三角形内角和180°时直接写结论",
        wrongAnswer = "省略推理过程",
        correctAnswer = "需要作辅助线并详细推导",
        explanation = "几何证明需要严格的逻辑推导，每一步都要有依据。",
        guidance = listOf(
            "几何证明要有理有据，每一步都要有理由",
            "常见依据：已知条件、公理、定理、定义",
            "不能跳步或省略证明过程"
        )
    ),
    CommonError(
        id = "meth_003",
        category = ErrorCategory.METHOD,
        title = "应用题不写单位",
        example = "答案是 5，没有写单位",
        wrongAnswer = "5",
        correctAnswer = "5米（或具体单位）",
        explanation = "应用题的答案必须带上单位，否则不完整。",
        guidance = listOf(
            "应用题答案要注明单位",
            "单位要前后一致，注意换算",
            "答案格式：数字 + 单位（如 5 米、3 千克）"
        )
    ),
    CommonError(
        id = "meth_004",
        category = ErrorCategory.METHOD,
        title = "分式方程不检验",
        example = "解 (x-1)/(x+2) = 3，得到 x = -3.5",
        wrongAnswer = "x = -3.5",
        correctAnswer = "x = -3.5（但需检验 x ≠ -2）",
        explanation = "分式方程求解后必须检验是否产生增根。",
        guidance = listOf(
            "分式方程必须检验：将解代入分母不能为0",
            "检验可以发现增根（无效的解）",
            "本题 x = -3.5 代入分母 ≠ 0，是有效解"
        )
    )
)

// ============================================================================
// Error Guide Screen
// ============================================================================

@Composable
fun ErrorGuideScreen() {
    var errorInput by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var selectedCategory by remember { mutableStateOf<ErrorCategory?>(null) }
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "错误辅导",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = "F-07 · AI分析解题错误，引导自主解决",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Error Input Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFFFF8F00),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "输入你的错误题目或描述",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5D4037)
                    )
                }
                
                OutlinedTextField(
                    value = errorInput,
                    onValueChange = { errorInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = {
                        Text(
                            text = "例如：解方程 2(x+3) = 12，我算出 x = 3",
                            fontSize = 14.sp,
                            color = Color(0xFFBDBDBD)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF8F00),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (errorInput.isNotBlank()) {
                                isAnalyzing = true
                                // Simulate AI analysis
                                analysisResult = simulateAnalysis(errorInput)
                                isAnalyzing = false
                            }
                        },
                        enabled = errorInput.isNotBlank() && !isAnalyzing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF8F00)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("分析中...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("开始分析")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            errorInput = ""
                            analysisResult = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("清除")
                    }
                }
            }
        }
        
        // Analysis Result
        AnimatedVisibility(
            visible = analysisResult != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            analysisResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                AnalysisResultCard(result = result)
            }
        }
        
        // Common Errors Section
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LibraryBooks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "常见错误速查",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("全部") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            ErrorCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { 
                        selectedCategory = if (selectedCategory == category) null else category 
                    },
                    label = { Text(category.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
        
        // Common Errors List
        val filteredErrors = commonErrors.filter { 
            selectedCategory == null || it.category == selectedCategory 
        }
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filteredErrors.forEach { error ->
                CommonErrorItem(
                    error = error,
                    isExpanded = expandedCategoryId == error.id,
                    onToggle = {
                        expandedCategoryId = if (expandedCategoryId == error.id) null else error.id
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ============================================================================
// Analysis Result Card
// ============================================================================

@Composable
fun AnalysisResultCard(result: AnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI 分析结果",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )
                    Text(
                        text = "检测类型：${result.detectedCategory.displayName} · ${result.detectedType}",
                        fontSize = 12.sp,
                        color = Color(0xFF795548)
                    )
                }
            }
            
            Divider(color = Color(0xFFFFCC80), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Input summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📝 你的输入",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8D6E63),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = result.input,
                        fontSize = 13.sp,
                        color = Color(0xFF4E342E)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Analysis Steps
            Text(
                text = "📋 分析步骤",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4E342E),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            result.steps.forEachIndexed { index, step ->
                AnalysisStepItem(step = step, index = index)
                if (index < result.steps.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = Color(0xFFFFCC80), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Guidance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFF8F00),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "💡 解决建议",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4E342E)
                )
            }
            
            result.guidance.forEachIndexed { index, tip ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${index + 1}. ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF8F00)
                    )
                    Text(
                        text = tip,
                        fontSize = 13.sp,
                        color = Color(0xFF5D4037)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisStepItem(step: AnalysisStep, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (step.isError) Color(0xFFFFEBEE) else Color.White,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        // Step number
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (step.isError) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.stepNumber.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (step.isError) Color(0xFFC62828) else Color(0xFF37474F)
            )
            Text(
                text = step.content,
                fontSize = 12.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp)
            )
            step.suggestion?.let { suggestion ->
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF43A047),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = suggestion,
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Common Error Item
// ============================================================================

@Composable
fun CommonErrorItem(
    error: CommonError,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val categoryColor = when (error.category) {
        ErrorCategory.CALCULATION -> Color(0xFFE53935)
        ErrorCategory.CONCEPT -> Color(0xFF1E88E5)
        ErrorCategory.METHOD -> Color(0xFF43A047)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(categoryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = error.category.icon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = error.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = error.category.displayName,
                        fontSize = 11.sp,
                        color = categoryColor
                    )
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Example preview
            Text(
                text = "示例：${error.example}",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp, start = 42.dp)
            )
            
            // Expanded content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Example
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "📝 错误示例",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF8F00),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = error.example,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5D4037)
                            )
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "错误答案：",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE53935)
                                )
                                Text(
                                    text = error.wrongAnswer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935)
                                )
                            }
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "正确答案：",
                                    fontSize = 12.sp,
                                    color = Color(0xFF43A047)
                                )
                                Text(
                                    text = error.correctAnswer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF43A047)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Explanation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 原因分析",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E88E5),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = error.explanation,
                                fontSize = 12.sp,
                                color = Color(0xFF0D47A1)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Guidance
                    Text(
                        text = "📌 解决方法",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF37474F),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    error.guidance.forEachIndexed { index, tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(Color(0xFF43A047), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tip,
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// AI Analysis Simulation
// ============================================================================

private fun simulateAnalysis(input: String): AnalysisResult {
    val lowerInput = input.lowercase()
    
    // Detect category and type based on keywords
    val (category, type) = when {
        lowerInput.contains("去括号") || lowerInput.contains("展开") || 
        lowerInput.contains("移项") || lowerInput.contains("分数") || 
        lowerInput.contains("幂") || lowerInput.contains("平方") ||
        lowerInput.contains("计算") || lowerInput.contains("等于") -> 
            ErrorCategory.CALCULATION to "运算步骤错误"
        lowerInput.contains("概念") || lowerInput.contains("定义") || 
        lowerInput.contains("函数") || lowerInput.contains("方程") ||
        lowerInput.contains("平行") || lowerInput.contains("知道") ->
            ErrorCategory.CONCEPT to "概念理解偏差"
        else -> ErrorCategory.METHOD to "解题方法不当"
    }
    
    // Generate steps based on category
    val steps = when (category) {
        ErrorCategory.CALCULATION -> listOf(
            AnalysisStep(1, "识别题目结构", "提取题目中的已知条件和运算关系"),
            AnalysisStep(2, "检查运算顺序", "按照先括号后乘除再加减的顺序检查", false, "遵循运算优先级规则"),
            AnalysisStep(3, "验证每步计算", "检查是否有跳步或遗漏的计算", true, "将每一步都写出来再检查"),
            AnalysisStep(4, "代回原式验证", "把答案代入原式检查是否成立")
        )
        ErrorCategory.CONCEPT -> listOf(
            AnalysisStep(1, "明确数学定义", "确认题目涉及的核心概念定义"),
            AnalysisStep(2, "区分相近概念", "区分容易混淆的概念（如方程与等式）", true, "对比两个概念的异同点"),
            AnalysisStep(3, "理解概念本质", "理解为什么是这样的定义"),
            AnalysisStep(4, "举例验证理解", "用具体例子验证自己的理解是否正确")
        )
        ErrorCategory.METHOD -> listOf(
            AnalysisStep(1, "明确解题目标", "确定最终要求的是什么"),
            AnalysisStep(2, "选择合适方法", "根据题目类型选择对应的解题方法"),
            AnalysisStep(3, "规范解题步骤", "按照标准步骤一步步书写", true, "不要跳步，每步都要写清楚"),
            AnalysisStep(4, "检查答案完整性", "检查是否遗漏单位或检验增根")
        )
    }
    
    // Generate guidance based on category
    val guidance = when (category) {
        ErrorCategory.CALCULATION -> listOf(
            "重新检查运算过程中的每一步",
            "注意符号变化，去括号时每一项都要乘",
            "可以用逆运算验证答案（如用除法验证乘法结果）",
            "养成验算习惯，考试时至少快速检查一遍"
        )
        ErrorCategory.CONCEPT -> listOf(
            "回顾教材中相关概念的定义",
            "找出概念中的关键词和限制条件",
            "举一个正例和一个反例来加深理解",
            "把概念用自己的话复述一遍确保真正理解"
        )
        ErrorCategory.METHOD -> listOf(
            "规范解题流程，不能跳步",
            "每一步都要有清晰的数学依据",
            "应用题记得写单位",
            "分式方程求解后必须检验"
        )
    }
    
    return AnalysisResult(
        input = input,
        detectedCategory = category,
        detectedType = type,
        steps = steps,
        guidance = guidance
    )
}