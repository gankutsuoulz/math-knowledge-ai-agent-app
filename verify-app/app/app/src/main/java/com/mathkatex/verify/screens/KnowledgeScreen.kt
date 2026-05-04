package com.mathkatex.verify.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Data Models
// ============================================================================

enum class Topic(val displayName: String) {
    代数("代数"),
    几何("几何"),
    函数("函数"),
    概率统计("概率统计"),
    数论("数论"),
    逻辑推理("逻辑推理")
}

enum class Grade(val displayName: String, val shortName: String) {
    五年级("五年级", "五"),
    六年级("六年级", "六"),
    初一("初一", "七"),
    初二("初二", "八"),
    初三("初三", "九"),
    高一("高一", "十"),
    高二("高二", "十一"),
    高三("高三", "十二")
}

data class KnowledgePoint(
    val id: String,
    val topic: Topic,
    val grades: List<Grade>,
    val title: String,
    val latex: String,
    val description: String,
    val content: String
)

// ============================================================================
// Knowledge Content Database (离线内容)
// ============================================================================

private val knowledgeDatabase = listOf(
    // -------------------- 代数 --------------------
    KnowledgePoint(
        id = "alg_001",
        topic = Topic.代数,
        grades = listOf(Grade.初一, Grade.初二),
        title = "平方差公式",
        latex = "(a+b)(a-b)=a^2-b^2",
        description = "两个数的和与差的乘积等于它们的平方差",
        content = "平方差公式是乘法公式之一：\n(a+b)(a-b)=a^2-b^2\n广泛应用于因式分解和计算化简。"
    ),
    KnowledgePoint(
        id = "alg_002",
        topic = Topic.代数,
        grades = listOf(Grade.初一, Grade.初二, Grade.初三),
        title = "完全平方公式",
        latex = "(a\\pm b)^2=a^2\\pm 2ab+b^2",
        description = "完全平方展开公式",
        content = "完全平方公式：\n(a+b)^2=a^2+2ab+b^2\n(a-b)^2=a^2-2ab+b^2"
    ),
    KnowledgePoint(
        id = "alg_003",
        topic = Topic.代数,
        grades = listOf(Grade.初一, Grade.初二),
        title = "一元一次方程",
        latex = "ax+b=0 \\Rightarrow x=-\\frac{b}{a}",
        description = "一元一次方程的标准解法",
        content = "一元一次方程 ax+b=0（a不为0）的解为：\nx=-b/a\n解题步骤：移项->合并同类项->系数化为1"
    ),
    KnowledgePoint(
        id = "alg_004",
        topic = Topic.代数,
        grades = listOf(Grade.初二, Grade.初三),
        title = "一元二次方程求根公式",
        latex = "x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}",
        description = "一元二次方程一般式的根",
        content = "对于方程 ax^2+bx+c=0（a不为0）：\n判别式 delta=b^2-4ac\n当 delta>=0 时，根为 x=(-b+/-sqrt(delta))/(2a)"
    ),
    KnowledgePoint(
        id = "alg_005",
        topic = Topic.代数,
        grades = listOf(Grade.初一, Grade.初二),
        title = "幂的运算法则",
        latex = "a^m \\cdot a^n=a^{m+n}",
        description = "同底数幂的乘除法则",
        content = "同底数幂运算：\na^m * a^n = a^{m+n}\na^m / a^n = a^{m-n}\na^0 = 1（a不为0）"
    ),
    KnowledgePoint(
        id = "alg_006",
        topic = Topic.代数,
        grades = listOf(Grade.高二, Grade.高三),
        title = "对数运算法则",
        latex = "\\log_a(MN)=\\log_aM+\\log_aN",
        description = "对数的基本运算性质",
        content = "对数运算法则：\nlog_a(MN)=log_aM+log_aN\nlog_a(M/N)=log_aM-log_aN\nlog_aM^n=n*log_aM"
    ),

    // -------------------- 几何 --------------------
    KnowledgePoint(
        id = "geo_001",
        topic = Topic.几何,
        grades = listOf(Grade.五年级, Grade.六年级, Grade.初一),
        title = "勾股定理",
        latex = "a^2+b^2=c^2",
        description = "直角三角形三边关系",
        content = "勾股定理（毕达哥拉斯定理）：\n在直角三角形中，两直角边的平方和等于斜边的平方。\na^2+b^2=c^2"
    ),
    KnowledgePoint(
        id = "geo_002",
        topic = Topic.几何,
        grades = listOf(Grade.初一, Grade.初二),
        title = "三角形内角和",
        latex = "\\angle A+\\angle B+\\angle C=180^\\circ",
        description = "三角形三个内角的和为180度",
        content = "三角形内角和定理：\n三角形三个内角的和等于180度\n即 angle A + angle B + angle C = 180"
    ),
    KnowledgePoint(
        id = "geo_003",
        topic = Topic.几何,
        grades = listOf(Grade.初二, Grade.初三),
        title = "圆面积公式",
        latex = "S=\\pi r^2",
        description = "圆面积等于pi乘以半径的平方",
        content = "圆面积公式：\nS = pi * r^2\n其中 r 为圆的半径，pi约为3.14159"
    ),
    KnowledgePoint(
        id = "geo_004",
        topic = Topic.几何,
        grades = listOf(Grade.初三, Grade.高一),
        title = "正弦定理",
        latex = "\\frac{a}{\\sin A}=\\frac{b}{\\sin B}=\\frac{c}{\\sin C}=2R",
        description = "三角形边长与对角正弦的比值相等",
        content = "正弦定理：\na/sin A = b/sin B = c/sin C = 2R\n其中 R 为三角形外接圆半径"
    ),
    KnowledgePoint(
        id = "geo_005",
        topic = Topic.几何,
        grades = listOf(Grade.初三, Grade.高一),
        title = "余弦定理",
        latex = "c^2=a^2+b^2-2ab\\cos C",
        description = "三角形边长与余弦的关系",
        content = "余弦定理：\nc^2 = a^2 + b^2 - 2ab*cos C\n用于已知两边及其夹角求第三边"
    ),
    KnowledgePoint(
        id = "geo_006",
        topic = Topic.几何,
        grades = listOf(Grade.六年级, Grade.初一),
        title = "平行线性质",
        latex = "\\angle 1 = \\angle 2",
        description = "两直线平行，同位角相等",
        content = "平行线性质定理：\n两直线平行时，同位角相等、内错角相等、同旁内角互补"
    ),

    // -------------------- 函数 --------------------
    KnowledgePoint(
        id = "func_001",
        topic = Topic.函数,
        grades = listOf(Grade.初二, Grade.初三),
        title = "一次函数",
        latex = "y=kx+b \\quad (k\\neq 0)",
        description = "斜率为k，截距为b的一次函数",
        content = "一次函数一般式：y=kx+b（k不为0）\nk>0 时图像上升，k<0 时图像下降\nb 是函数在y轴上的截距"
    ),
    KnowledgePoint(
        id = "func_002",
        topic = Topic.函数,
        grades = listOf(Grade.初三, Grade.高一),
        title = "二次函数顶点式",
        latex = "y=a(x-h)^2+k",
        description = "以(h,k)为顶点的抛物线",
        content = "二次函数顶点式：y=a(x-h)^2+k\n顶点坐标为 (h, k)\na>0 开口向上，a<0 开口向下"
    ),
    KnowledgePoint(
        id = "func_003",
        topic = Topic.函数,
        grades = listOf(Grade.高一, Grade.高二),
        title = "指数函数",
        latex = "y=a^x \\quad (a>0, a\\neq 1)",
        description = "底数为a，指数为x的指数函数",
        content = "指数函数：y=a^x（a>0 且 a不为1）\n定义域：R\n值域：(0, +infinity)\na>1 时递增，0<a<1 时递减"
    ),
    KnowledgePoint(
        id = "func_004",
        topic = Topic.函数,
        grades = listOf(Grade.高二, Grade.高三),
        title = "对数函数",
        latex = "y=\\log_a x \\quad (a>0, a\\neq 1)",
        description = "底数为a的对数函数",
        content = "对数函数：y=log_a x（a>0 且 a不为1）\n定义域：(0, +infinity)\n值域：R\n与 y=a^x 互为反函数"
    ),
    KnowledgePoint(
        id = "func_005",
        topic = Topic.函数,
        grades = listOf(Grade.高一, Grade.高二),
        title = "三角函数诱导公式",
        latex = "\\sin(\\frac{\\pi}{2}-x)=\\cos x",
        description = "三角函数诱导公式之一",
        content = "诱导公式：\nsin(pi/2 - x) = cos x\ncos(pi/2 - x) = sin x\n记忆口诀：奇变偶不变，符号看象限"
    ),

    // -------------------- 概率统计 --------------------
    KnowledgePoint(
        id = "prob_001",
        topic = Topic.概率统计,
        grades = listOf(Grade.初一, Grade.初二),
        title = "概率基本公式",
        latex = "P(A)=\\frac{n(A)}{n(U)}",
        description = "事件A的概率等于有利情况数除以总情况数",
        content = "概率基本公式：\nP(A) = n(A) / n(U)\n其中 n(A) 为事件A包含的基本事件数，n(U) 为总的基本事件数"
    ),
    KnowledgePoint(
        id = "prob_002",
        topic = Topic.概率统计,
        grades = listOf(Grade.高二, Grade.高三),
        title = "排列数公式",
        latex = "A_n^m=\\frac{n!}{(n-m)!}",
        description = "从n个不同元素中取m个的排列数",
        content = "排列数公式：\nA_n^m = n! / (n-m)!\n当 m=n 时，A_n^n = n!（全排列）"
    ),
    KnowledgePoint(
        id = "prob_003",
        topic = Topic.概率统计,
        grades = listOf(Grade.高二, Grade.高三),
        title = "组合数公式",
        latex = "C_n^m=\\frac{n!}{m!(n-m)!}",
        description = "从n个不同元素中取m个的组合数",
        content = "组合数公式：\nC_n^m = n! / [m!(n-m)!]\n组合与排列的区别：组合不计顺序"
    ),
    KnowledgePoint(
        id = "prob_004",
        topic = Topic.概率统计,
        grades = listOf(Grade.六年级, Grade.初一),
        title = "平均数公式",
        latex = "\\bar{x}=\\frac{x_1+x_2+\\cdots+x_n}{n}",
        description = "n个数的算术平均数",
        content = "算术平均数：\nx_bar = (x_1 + x_2 + ... + x_n) / n\n表示一组数据的平均水平"
    ),

    // -------------------- 数论 --------------------
    KnowledgePoint(
        id = "numth_001",
        topic = Topic.数论,
        grades = listOf(Grade.初一, Grade.初二),
        title = "最大公约数",
        latex = "gcd(a,b)",
        description = "两个正整数的最大公约数",
        content = "最大公约数（gcd）：\n设 a,b 为正整数，gcd(a,b) 为能整除 a 和 b 的最大正整数\n常用算法：辗转相除法"
    ),
    KnowledgePoint(
        id = "numth_002",
        topic = Topic.数论,
        grades = listOf(Grade.初一, Grade.初二),
        title = "最小公倍数",
        latex = "lcm(a,b)=\\frac{a \\cdot b}{gcd(a,b)}",
        description = "两个正整数的最小公倍数",
        content = "最小公倍数（lcm）：\n设 a,b 为正整数，lcm(a,b) 为能同时被 a 和 b 整除的最小正整数\n公式：lcm(a,b) = a*b / gcd(a,b)"
    ),
    KnowledgePoint(
        id = "numth_003",
        topic = Topic.数论,
        grades = listOf(Grade.初三, Grade.高一, Grade.高二),
        title = "等差数列求和",
        latex = "S_n=\\frac{n(a_1+a_n)}{2}",
        description = "等差数列前n项和公式",
        content = "等差数列求和公式：\nS_n = n(a_1+a_n) / 2\n= n*a_1 + n(n-1)*d/2\n其中 d 为公差"
    ),
    KnowledgePoint(
        id = "numth_004",
        topic = Topic.数论,
        grades = listOf(Grade.高一, Grade.高二, Grade.高三),
        title = "等比数列求和",
        latex = "S_n=a_1 \\cdot \\frac{1-q^n}{1-q} \\quad (q\\neq 1)",
        description = "等比数列前n项和公式",
        content = "等比数列求和公式（q不为1）：\nS_n = a_1*(1-q^n) / (1-q)\n当 q=1 时，S_n = n*a_1\n当 |q|<1 时，infinity时 S = a_1/(1-q)"
    ),

    // -------------------- 逻辑推理 --------------------
    KnowledgePoint(
        id = "logic_001",
        topic = Topic.逻辑推理,
        grades = listOf(Grade.初一, Grade.初二, Grade.初三),
        title = "命题逻辑",
        latex = "P \\Rightarrow Q",
        description = "若P则Q的蕴含关系",
        content = "命题蕴含 P=>Q：\n当 P 为真且 Q 为假时，P=>Q 为假\n其余情况均为真\n也叫充分条件推必要条件"
    ),
    KnowledgePoint(
        id = "logic_002",
        topic = Topic.逻辑推理,
        grades = listOf(Grade.初二, Grade.初三, Grade.高一),
        title = "反证法",
        latex = "\\neg Q \\Rightarrow \\neg P",
        description = "通过否定结论推出否定前提",
        content = "反证法步骤：\n1. 假设结论 Q 的否定 not Q 成立\n2. 通过逻辑推导得到前提 P 的否定 not P\n3. 与已知前提矛盾，故原结论 Q 成立"
    ),
    KnowledgePoint(
        id = "logic_003",
        topic = Topic.逻辑推理,
        grades = listOf(Grade.初一, Grade.初二, Grade.初三),
        title = "数学归纳法",
        latex = "S_k \\Rightarrow S_{k+1}",
        description = "证明关于自然数的命题",
        content = "数学归纳法步骤：\n1. 验证 n=1 时命题 S_1 成立（base case）\n2. 假设 S_k 成立，推导 S_{k+1} 也成立（inductive step）\n3. 由1和2，对所有自然数 n 命题均成立"
    )
)

// ============================================================================
// UI Components
// ============================================================================

private val topicColorMap = mapOf(
    Topic.代数 to Color(0xFF2196F3),
    Topic.几何 to Color(0xFF4CAF50),
    Topic.函数 to Color(0xFFFF9800),
    Topic.概率统计 to Color(0xFF9C27B0),
    Topic.数论 to Color(0xFFE91E63),
    Topic.逻辑推理 to Color(0xFF00BCD4)
)

@Composable
private fun FilterChipBar(
    grades: List<Grade>,
    selectedGrade: Grade?,
    onGradeSelected: (Grade?) -> Unit,
    topics: List<Topic>,
    selectedTopic: Topic?,
    onTopicSelected: (Topic?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Grade filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedGrade == null,
                onClick = { onGradeSelected(null) },
                label = { Text("全部年级", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedLabelColor = Color.White
                )
            )
            grades.forEach { grade ->
                FilterChip(
                    selected = selectedGrade == grade,
                    onClick = { onGradeSelected(if (selectedGrade == grade) null else grade) },
                    label = { Text(grade.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2196F3),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Topic filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTopic == null,
                onClick = { onTopicSelected(null) },
                label = { Text("全部分类", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50),
                    selectedLabelColor = Color.White
                )
            )
            topics.forEach { topic ->
                FilterChip(
                    selected = selectedTopic == topic,
                    onClick = { onTopicSelected(if (selectedTopic == topic) null else topic) },
                    label = { Text(topic.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = topicColorMap[topic] ?: Color(0xFF4CAF50),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun KnowledgeCard(
    point: KnowledgePoint,
    modifier: Modifier = Modifier
) {
    val topicColor = topicColorMap[point.topic] ?: Color(0xFF2196F3)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: topic badge + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = topicColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = point.topic.displayName,
                            fontSize = 11.sp,
                            color = topicColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = point.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF333333)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grade levels
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                point.grades.forEach { grade ->
                    Surface(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = grade.shortName,
                            fontSize = 10.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // KaTeX formula rendering
            KaTeXWebView(
                latex = point.latex,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = point.description,
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Content text
            Text(
                text = point.content,
                fontSize = 12.sp,
                color = Color(0xFF555555),
                lineHeight = 18.sp
            )
        }
    }
}

// ============================================================================
// Main Screen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen() {
    var selectedGrade by remember { mutableStateOf<Grade?>(null) }
    var selectedTopic by remember { mutableStateOf<Topic?>(null) }

    // Filter logic
    val filteredPoints = remember(selectedGrade, selectedTopic) {
        knowledgeDatabase.filter { point ->
            (selectedGrade == null || point.grades.contains(selectedGrade)) &&
            (selectedTopic == null || point.topic == selectedTopic)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("知识点速查", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF2196F3).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "F-02",
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
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter section
            Surface(
                color = Color(0xFFFAFAFA),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChipBar(
                    grades = Grade.entries.toList(),
                    selectedGrade = selectedGrade,
                    onGradeSelected = { selectedGrade = it },
                    topics = Topic.entries.toList(),
                    selectedTopic = selectedTopic,
                    onTopicSelected = { selectedTopic = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Results count
            Text(
                text = "共 ${filteredPoints.size} 个知识点",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Knowledge list
            if (filteredPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No results",
                            fontSize = 16.sp,
                            color = Color(0xFF888888)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting filter conditions",
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredPoints, key = { it.id }) { point ->
                        KnowledgeCard(point = point)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}
