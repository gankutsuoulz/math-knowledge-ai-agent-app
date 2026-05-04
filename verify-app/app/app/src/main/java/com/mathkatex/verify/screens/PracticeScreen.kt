package com.mathkatex.verify.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==================== Data Models ====================

data class PracticeTopic(
    val id: String,
    val name: String,
    val icon: String,
    val questionCount: Int
)

data class PracticeQuestion(
    val id: String,
    val topicId: String,
    val topicName: String,
    val difficulty: String, // "easy", "medium", "hard"
    val problemText: String,
    val problemLatex: String,
    val answerText: String,
    val answerLatex: String,
    val solutionText: String,
    val solutionLatex: String
)

enum class PracticeMode {
    RANDOM,      // 随机的题
    BY_TOPIC     // 按知识点刷题
}

data class PracticeState(
    val mode: PracticeMode = PracticeMode.RANDOM,
    val selectedTopic: PracticeTopic? = null,
    val currentQuestion: PracticeQuestion? = null,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val showAnswer: Boolean = false,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val isFinished: Boolean = false
)

// ==================== Sample Data ====================

val practiceTopics = listOf(
    PracticeTopic("algebra_1", "一元一次方程", "🔢", 12),
    PracticeTopic("algebra_2", "一元二次方程", "📐", 15),
    PracticeTopic("geometry_1", "平面几何基础", "📐", 10),
    PracticeTopic("geometry_2", "相似三角形", "🔺", 8),
    PracticeTopic("function_1", "一次函数", "📈", 9),
    PracticeTopic("function_2", "二次函数", "📊", 14),
    PracticeTopic("prob_1", "概率初步", "🎲", 7),
    PracticeTopic("stat_1", "统计初步", "📊", 6),
    PracticeTopic("number_theory", "数论基础", "🔢", 11),
    PracticeTopic("fraction", "分数运算", "➗", 13)
)

val sampleQuestions = listOf(
    // 一元一次方程
    PracticeQuestion(
        id = "q001",
        topicId = "algebra_1",
        topicName = "一元一次方程",
        difficulty = "easy",
        problemText = "求解方程：2x + 5 = 13",
        problemLatex = "2x + 5 = 13",
        answerText = "x = 4",
        answerLatex = "x = 4",
        solutionText = "移项得 2x = 13 - 5 = 8，两边除以2得 x = 4",
        solutionLatex = "2x = 13 - 5 = 8 \\Rightarrow x = \\frac{8}{2} = 4"
    ),
    PracticeQuestion(
        id = "q002",
        topicId = "algebra_1",
        topicName = "一元一次方程",
        difficulty = "medium",
        problemText = "求解方程：3(x - 2) = 12",
        problemLatex = "3(x - 2) = 12",
        answerText = "x = 6",
        answerLatex = "x = 6",
        solutionText = "展开得 3x - 6 = 12，移项得 3x = 18，两边除以3得 x = 6",
        solutionLatex = "3x - 6 = 12 \\Rightarrow 3x = 18 \\Rightarrow x = \\frac{18}{3} = 6"
    ),
    // 一元二次方程
    PracticeQuestion(
        id = "q003",
        topicId = "algebra_2",
        topicName = "一元二次方程",
        difficulty = "medium",
        problemText = "求解方程：x² - 5x + 6 = 0",
        problemLatex = "x^2 - 5x + 6 = 0",
        answerText = "x = 2 或 x = 3",
        answerLatex = "x = 2 \\text{ 或 } x = 3",
        solutionText = "因式分解：(x-2)(x-3)=0，故 x = 2 或 x = 3",
        solutionLatex = "(x-2)(x-3)=0 \\Rightarrow x=2 \\text{ 或 } x=3"
    ),
    PracticeQuestion(
        id = "q004",
        topicId = "algebra_2",
        topicName = "一元二次方程",
        difficulty = "hard",
        problemText = "求解方程：2x² + 4x - 6 = 0",
        problemLatex = "2x^2 + 4x - 6 = 0",
        answerText = "x = 1 或 x = -3",
        answerLatex = "x = 1 \\text{ 或 } x = -3",
        solutionText = "使用求根公式：x = (-4 ± √(16+48)) / 4 = (-4 ± 8) / 4",
        solutionLatex = "x = \\frac{-4 \\pm \\sqrt{16+48}}{4} = \\frac{-4 \\pm 8}{4} \\Rightarrow x = 1 \\text{ 或 } x = -3"
    ),
    // 平面几何基础
    PracticeQuestion(
        id = "q005",
        topicId = "geometry_1",
        topicName = "平面几何基础",
        difficulty = "easy",
        problemText = "三角形内角和是多少度？",
        problemLatex = "\\text{三角形内角和} = ?",
        answerText = "180°",
        answerLatex = "180^\\circ",
        solutionText = "三角形内角和定理：任意三角形的三个内角和等于180度",
        solutionLatex = "\\angle A + \\angle B + \\angle C = 180^\\circ"
    ),
    PracticeQuestion(
        id = "q006",
        topicId = "geometry_1",
        topicName = "平面几何基础",
        difficulty = "medium",
        problemText = "等腰三角形顶角为40°，求底角大小",
        problemLatex = "\\text{等腰三角形，顶角} = 40^\\circ \\Rightarrow \\text{底角} = ?",
        answerText = "70°",
        answerLatex = "70^\\circ",
        solutionText = "等腰三角形两底角相等，设底角为x，则 40° + 2x = 180°，得 x = 70°",
        solutionLatex = "40^\\circ + 2x = 180^\\circ \\Rightarrow x = 70^\\circ"
    ),
    // 相似三角形
    PracticeQuestion(
        id = "q007",
        topicId = "geometry_2",
        topicName = "相似三角形",
        difficulty = "medium",
        problemText = "在△ABC中，DE∥BC，AD:DB=1:2，求AE:EC",
        problemLatex = "\\triangle ABC, DE \\parallel BC, AD:DB = 1:2 \\Rightarrow AE:EC = ?",
        answerText = "1:2",
        answerLatex = "1:2",
        solutionText = "由平行线分线段成比例定理，AD:DB = AE:EC = 1:2",
        solutionLatex = "\\frac{AD}{DB} = \\frac{AE}{EC} = \\frac{1}{2}"
    ),
    // 一次函数
    PracticeQuestion(
        id = "q008",
        topicId = "function_1",
        topicName = "一次函数",
        difficulty = "easy",
        problemText = "已知一次函数 y = 2x + 1，当 x = 3 时，y = ?",
        problemLatex = "y = 2x + 1, x = 3 \\Rightarrow y = ?",
        answerText = "y = 7",
        answerLatex = "y = 7",
        solutionText = "代入 x = 3 得 y = 2×3 + 1 = 7",
        solutionLatex = "y = 2 \\times 3 + 1 = 7"
    ),
    PracticeQuestion(
        id = "q009",
        topicId = "function_1",
        topicName = "一次函数",
        difficulty = "medium",
        problemText = "已知一次函数图像经过点 (1, 3) 和 (2, 5)，求函数解析式",
        problemLatex = "y = kx + b, (1,3), (2,5) \\Rightarrow y = ?",
        answerText = "y = 2x + 1",
        answerLatex = "y = 2x + 1",
        solutionText = "代入两点：3 = k + b, 5 = 2k + b，两式相减得 k = 2，代入得 b = 1",
        solutionLatex = "\\begin{cases} 3 = k + b \\\\ 5 = 2k + b \\end{cases} \\Rightarrow k = 2, b = 1 \\Rightarrow y = 2x + 1"
    ),
    // 二次函数
    PracticeQuestion(
        id = "q010",
        topicId = "function_2",
        topicName = "二次函数",
        difficulty = "medium",
        problemText = "求二次函数 y = x² - 4x + 3 的顶点坐标",
        problemLatex = "y = x^2 - 4x + 3 \\Rightarrow \\text{顶点} = ?",
        answerText = "(2, -1)",
        answerLatex = "(2, -1)",
        solutionText = "配方法：y = (x-2)² - 1，故顶点为 (2, -1)",
        solutionLatex = "y = (x-2)^2 - 1 \\Rightarrow \\text{顶点} = (2, -1)"
    ),
    // 概率初步
    PracticeQuestion(
        id = "q011",
        topicId = "prob_1",
        topicName = "概率初步",
        difficulty = "easy",
        problemText = "抛掷一枚均匀硬币，正面朝上的概率是多少？",
        problemLatex = "\\text{抛硬币，正面概率} = ?",
        answerText = "1/2",
        answerLatex = "\\frac{1}{2}",
        solutionText = "硬币有两面，正面只有1种情况，故概率为 1/2",
        solutionLatex = "P = \\frac{1}{2}"
    ),
    PracticeQuestion(
        id = "q012",
        topicId = "prob_1",
        topicName = "概率初步",
        difficulty = "medium",
        problemText = "袋中有3个红球和2个白球，随机摸一个球，摸到红球的概率是多少？",
        problemLatex = "3\\text{红} + 2\\text{白} = 5\\text{球} \\Rightarrow P(\\text{红}) = ?",
        answerText = "3/5",
        answerLatex = "\\frac{3}{5}",
        solutionText = "总球数5个，红球3个，故 P(红) = 3/5",
        solutionLatex = "P(\\text{红}) = \\frac{3}{5}"
    ),
    // 数论基础
    PracticeQuestion(
        id = "q013",
        topicId = "number_theory",
        topicName = "数论基础",
        difficulty = "medium",
        problemText = "求12和18的最大公约数",
        problemLatex = "GCD(12, 18) = ?",
        answerText = "6",
        answerLatex = "6",
        solutionText = "12的约数：1,2,3,4,6,12；18的约数：1,2,3,6,9,18；最大公共约数为6",
        solutionLatex = "GCD(12, 18) = 6"
    ),
    PracticeQuestion(
        id = "q014",
        topicId = "number_theory",
        topicName = "数论基础",
        difficulty = "medium",
        problemText = "求12和18的最小公倍数",
        problemLatex = "LCM(12, 18) = ?",
        answerText = "36",
        answerLatex = "36",
        solutionText = "12=2²×3，18=2×3²，取最高次幂得 LCM = 2²×3² = 36",
        solutionLatex = "LCM(12, 18) = 36"
    ),
    // 分数运算
    PracticeQuestion(
        id = "q015",
        topicId = "fraction",
        topicName = "分数运算",
        difficulty = "easy",
        problemText = "计算：1/2 + 1/3",
        problemLatex = "\\frac{1}{2} + \\frac{1}{3} = ?",
        answerText = "5/6",
        answerLatex = "\\frac{5}{6}",
        solutionText = "通分：1/2 = 3/6，1/3 = 2/6，故 3/6 + 2/6 = 5/6",
        solutionLatex = "\\frac{1}{2} + \\frac{1}{3} = \\frac{3}{6} + \\frac{2}{6} = \\frac{5}{6}"
    )
)

// ==================== Composable Functions ====================

@Composable
fun PracticeScreen() {
    var state by remember { mutableStateOf(PracticeState()) }
    
    // 处理返回键：当处于题目详情时，拦截返回键，使其返回到题目选择而非首页
    BackHandler(enabled = state.currentQuestion != null) {
        state = state.copy(currentQuestion = null, selectedTopic = null, showAnswer = false)
    }
    
    when {
        state.isFinished -> PracticeFinishedScreen(
            correctCount = state.correctCount,
            incorrectCount = state.incorrectCount,
            onRestart = { state = PracticeState() }
        )
        state.currentQuestion != null -> PracticeQuestionScreen(
            question = state.currentQuestion!!,
            questionIndex = state.questionIndex,
            totalQuestions = state.totalQuestions,
            showAnswer = state.showAnswer,
            onShowAnswer = { state = state.copy(showAnswer = true) },
            onPrev = {
                if (state.questionIndex > 0) {
                    val topicId = state.selectedTopic?.id
                    val questions = if (topicId != null) {
                        sampleQuestions.filter { it.topicId == topicId }.take(10)
                    } else {
                        sampleQuestions.shuffled().take(10)
                    }
                    val prevIndex = state.questionIndex - 1
                    state = state.copy(
                        currentQuestion = questions[prevIndex],
                        questionIndex = prevIndex,
                        showAnswer = false
                    )
                }
            },
            onNext = {
                if (state.questionIndex < state.totalQuestions - 1) {
                    nextQuestion(state) { state = it }
                }
            },
            onMarkCorrect = {
                state = state.copy(correctCount = state.correctCount + 1)
            },
            onMarkIncorrect = {
                state = state.copy(incorrectCount = state.incorrectCount + 1)
            },
            onBack = { state = state.copy(currentQuestion = null, selectedTopic = null, showAnswer = false) }
        )
        state.selectedTopic != null -> startPractice(state, topicMode = true) { state = it }
        else -> PracticeTopicSelectionScreen(
            onTopicSelected = { topic ->
                state = state.copy(selectedTopic = topic)
            },
            onRandomMode = {
                state = state.copy(mode = PracticeMode.RANDOM)
            }
        )
    }
}

@Composable
fun PracticeTopicSelectionScreen(
    onTopicSelected: (PracticeTopic) -> Unit,
    onRandomMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "例题练习 [F-03]",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "选择练习模式",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 模式选择卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeCard(
                title = "随机的题",
                icon = "🎲",
                description = "随机抽取10道题",
                modifier = Modifier.weight(1f),
                onClick = onRandomMode
            )
            ModeCard(
                title = "按知识点刷题",
                icon = "📚",
                description = "选择特定知识点",
                modifier = Modifier.weight(1f),
                onClick = { }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "知识点选择",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(practiceTopics) { topic ->
                TopicCard(
                    topic = topic,
                    onClick = { onTopicSelected(topic) }
                )
            }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    icon: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun TopicCard(
    topic: PracticeTopic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = topic.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = topic.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${topic.questionCount}题",
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )
        }
    }
}

@Composable
fun PracticeQuestionScreen(
    question: PracticeQuestion,
    questionIndex: Int,
    totalQuestions: Int,
    showAnswer: Boolean,
    onShowAnswer: () -> Unit,
    onMarkCorrect: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMarkIncorrect: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 顶部导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "第 ${questionIndex + 1} / $totalQuestions 题",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            TopicBadge(topicName = question.topicName)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 难度标签
        Row {
            DifficultyBadge(difficulty = question.difficulty)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 题目区域
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // 题目卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "题目",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = question.problemText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    KaTeXWebView(
                        latex = question.problemLatex,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 答案区域
            AnimatedVisibility(
                visible = showAnswer,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "答案",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        KaTeXWebView(
                            latex = question.answerLatex,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "解题过程",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = question.solutionText,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        KaTeXWebView(
                            latex = question.solutionLatex,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 底部按钮
        if (!showAnswer) {
            Button(
                onClick = onShowAnswer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("显示答案")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 上一题/下一题 导航
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrev,
                        modifier = Modifier.weight(1f),
                        enabled = questionIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上一题")
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("下一题")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
                
                // 标记正确/错误
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarkIncorrect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("答错了")
                    }
                    Button(
                        onClick = onMarkCorrect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("答对了")
                    }
                }
            }
        }
    }
}

@Composable
fun TopicBadge(topicName: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE3F2FD)
    ) {
        Text(
            text = topicName,
            fontSize = 12.sp,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DifficultyBadge(difficulty: String) {
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
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PracticeFinishedScreen(
    correctCount: Int,
    incorrectCount: Int,
    onRestart: () -> Unit
) {
    val total = correctCount + incorrectCount
    val percentage = if (total > 0) (correctCount * 100 / total) else 0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "练习完成！",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 统计卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "正确率",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "$percentage%",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        percentage >= 80 -> Color(0xFF4CAF50)
                        percentage >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$correctCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "答对",
                            fontSize = 12.sp,
                            color = Color(0xFF888888)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$incorrectCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            text = "答错",
                            fontSize = 12.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("再练一次")
        }
    }
}

// ==================== Helper Functions ====================

private fun startPractice(
    state: PracticeState,
    topicMode: Boolean,
    updateState: (PracticeState) -> Unit
) {
    val questions = if (topicMode && state.selectedTopic != null) {
        sampleQuestions.filter { it.topicId == state.selectedTopic!!.id }.take(10)
    } else {
        sampleQuestions.shuffled().take(10)
    }
    
    if (questions.isEmpty()) {
        updateState(state.copy(isFinished = true))
        return
    }
    
    updateState(
        state.copy(
            currentQuestion = questions.first(),
            totalQuestions = questions.size,
            questionIndex = 0
        )
    )
}

private fun nextQuestion(
    state: PracticeState,
    updateState: (PracticeState) -> Unit
) {
    val topicId = state.selectedTopic?.id
    val questions = if (topicId != null) {
        sampleQuestions.filter { it.topicId == topicId }.take(10)
    } else {
        sampleQuestions.shuffled().take(10)
    }
    
    val nextIndex = state.questionIndex + 1
    
    if (nextIndex >= questions.size) {
        updateState(state.copy(isFinished = true, currentQuestion = null))
    } else {
        updateState(
            state.copy(
                currentQuestion = questions[nextIndex],
                questionIndex = nextIndex
            )
        )
    }
}