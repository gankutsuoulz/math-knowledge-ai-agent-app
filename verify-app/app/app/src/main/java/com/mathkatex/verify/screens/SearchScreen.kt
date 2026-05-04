package com.mathkatex.verify.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ============================================================
// Search Data Models
// ============================================================

data class SearchItem(
    val id: String,
    val title: String,
    val topic: String,
    val grade: String,
    val category: String,      // "knowledge" or "problem"
    val content: String,      // plain text preview
    val latex: String? = null // formula if any
)

data class SearchResult(
    val item: SearchItem,
    val matchedText: String,
    val highlightRanges: List<IntRange> = emptyList()
)

// ============================================================
// Local Search Data Source
// ============================================================

object SearchDataSource {
    
    private val knowledgeItems = listOf(
        // 五年级 - 小数
        SearchItem(
            id = "k001",
            title = "小数乘整数",
            topic = "数与运算",
            grade = "五年级",
            category = "knowledge",
            content = "小数乘整数：先按整数乘法算出积，再根据被乘数小数位数确定积的小数点位置。",
            latex = "a \\times b = \\frac{(a \\times 10^m) \\times b}{10^m}"
        ),
        SearchItem(
            id = "k002",
            title = "小数乘小数",
            topic = "数与运算",
            grade = "五年级",
            category = "knowledge",
            content = "小数乘小数：先按整数乘法算出积，再根据乘数小数位数之和确定积的小数点位置。",
            latex = "\\text{积的小数位数} = \\text{被乘数小数位数} + \\text{乘数小数位数}"
        ),
        SearchItem(
            id = "k003",
            title = "小数除以整数",
            topic = "数与运算",
            grade = "五年级",
            category = "knowledge",
            content = "小数除以整数：将被除数化为整数，除数同时扩大相同倍数，商不变。",
            latex = "a \\div b = \\frac{a \\times 10^k}{b \\times 10^k}"
        ),
        SearchItem(
            id = "k004",
            title = "商不变性质",
            topic = "数与运算",
            grade = "五年级",
            category = "knowledge",
            content = "商不变性质：被除数和除数同时乘或除以同一个非零数，商不变。",
            latex = "a \\div b = (a \\times c) \\div (b \\times c)"
        ),
        
        // 六年级 - 分数
        SearchItem(
            id = "k005",
            title = "分数乘分数",
            topic = "数与运算",
            grade = "六年级",
            category = "knowledge",
            content = "分数乘分数：分子乘分子，分母乘分母，能约分先约分。",
            latex = "\\frac{a}{b} \\times \\frac{c}{d} = \\frac{a \\cdot c}{b \\cdot d}"
        ),
        SearchItem(
            id = "k006",
            title = "分数除以分数",
            topic = "数与运算",
            grade = "六年级",
            category = "knowledge",
            content = "分数除以分数：被除数乘除数的倒数，转化为乘法计算。",
            latex = "\\frac{a}{b} \\div \\frac{c}{d} = \\frac{a}{b} \\times \\frac{d}{c}"
        ),
        SearchItem(
            id = "k007",
            title = "分数运算定律",
            topic = "数与运算",
            grade = "六年级",
            category = "knowledge",
            content = "分数运算定律：同分母分数加减，分母不变分子相加减；乘法分配律。",
            latex = "\\frac{a}{b} \\times (\\frac{c}{d} + \\frac{e}{f}) = \\frac{a}{b} \\times \\frac{c}{d} + \\frac{a}{b} \\times \\frac{e}{f}"
        ),
        
        // 六年级 - 百分数
        SearchItem(
            id = "k008",
            title = "百分数定义",
            topic = "数与运算",
            grade = "六年级",
            category = "knowledge",
            content = "百分数定义：表示一个数是另一个数的百分之几，符号为%。",
            latex = "p\\% = \\frac{p}{100}"
        ),
        SearchItem(
            id = "k009",
            title = "增长率",
            topic = "数与运算",
            grade = "六年级",
            category = "knowledge",
            content = "增长率：增长量除以原量，表示增长的百分比。",
            latex = "\\text{增长率} = \\frac{\\text{现量} - \\text{原量}}{\\text{原量}} \\times 100\\%"
        ),
        SearchItem(
            id = "k010",
            title = "利息计算",
            topic = "数与运算",
            grade = "六年级",
            category = "knowledge",
            content = "利息计算：本金乘利率乘时间。",
            latex = "\\text{本息和} = \\text{本金} \\times (1 + \\text{利率} \\times \\text{时间})"
        ),
        
        // 六年级 - 比和比例
        SearchItem(
            id = "k011",
            title = "比的意义",
            topic = "比和比例",
            grade = "六年级",
            category = "knowledge",
            content = "比的意义：表示两个数相除关系，a比b记作a:b。",
            latex = "a : b = \\frac{a}{b}"
        ),
        SearchItem(
            id = "k012",
            title = "比例的基本性质",
            topic = "比和比例",
            grade = "六年级",
            category = "knowledge",
            content = "比例的基本性质：内项之积等于外项之积。",
            latex = "\\frac{a}{b} = \\frac{c}{d} \\Rightarrow ad = bc"
        ),
        
        // 初一 - 有理数
        SearchItem(
            id = "k013",
            title = "绝对值",
            topic = "有理数",
            grade = "初一",
            category = "knowledge",
            content = "绝对值：数轴上点到原点的距离，非负数。",
            latex = "|a| = \\begin{cases} a, & a \\ge 0 \\\\ -a, & a < 0 \\end{cases}"
        ),
        SearchItem(
            id = "k014",
            title = "数轴",
            topic = "有理数",
            grade = "初一",
            category = "knowledge",
            content = "数轴：规定了原点、正方向和单位长度的直线。",
            latex = "|a - b| = \\text{点}A(a)\\text{到点}B(b)\\text{的距离}"
        ),
        SearchItem(
            id = "k015",
            title = "幂运算",
            topic = "有理数运算",
            grade = "初一",
            category = "knowledge",
            content = "幂运算：同底数幂相乘，底数不变指数相加。",
            latex = "a^m \\cdot a^n = a^{m+n}"
        ),
        
        // 初一 - 整式
        SearchItem(
            id = "k016",
            title = "幂的乘方",
            topic = "整式运算",
            grade = "初一",
            category = "knowledge",
            content = "幂的乘方：底数不变，指数相乘。",
            latex = "(a^m)^n = a^{mn}"
        ),
        SearchItem(
            id = "k017",
            title = "积的乘方",
            topic = "整式运算",
            grade = "初一",
            category = "knowledge",
            content = "积的乘方：每个因式分别乘方。",
            latex = "(ab)^n = a^n b^n"
        ),
        
        // 初二 - 一次函数
        SearchItem(
            id = "k018",
            title = "一次函数",
            topic = "函数",
            grade = "初二",
            category = "knowledge",
            content = "一次函数：y = kx + b (k≠0)，k为斜率，b为截距。",
            latex = "y = kx + b \\quad (k \\neq 0)"
        ),
        SearchItem(
            id = "k019",
            title = "正比例函数",
            topic = "函数",
            grade = "初二",
            category = "knowledge",
            content = "正比例函数：y = kx (k≠0)，图像过原点。",
            latex = "y = kx"
        ),
        
        // 初三 - 二次函数
        SearchItem(
            id = "k020",
            title = "二次函数",
            topic = "函数",
            grade = "初三",
            category = "knowledge",
            content = "二次函数：y = ax² + bx + c (a≠0)，开口方向由a决定。",
            latex = "y = ax^2 + bx + c \\quad (a \\neq 0)"
        ),
        SearchItem(
            id = "k021",
            title = "顶点坐标",
            topic = "函数",
            grade = "初三",
            category = "knowledge",
            content = "顶点坐标：二次函数顶点为(-b/2a, (4ac-b²)/4a)。",
            latex = "顶点 = \\left(-\\frac{b}{2a}, \\frac{4ac-b^2}{4a}\\right)"
        ),
        
        // 高一 - 三角函数
        SearchItem(
            id = "k022",
            title = "正弦函数",
            topic = "三角函数",
            grade = "高一",
            category = "knowledge",
            content = "正弦函数：sin θ = 对边/斜边，周期2π。",
            latex = "\\sin\\theta = \\frac{\\text{对边}}{\\text{斜边}}"
        ),
        SearchItem(
            id = "k023",
            title = "余弦函数",
            topic = "三角函数",
            grade = "高一",
            category = "knowledge",
            content = "余弦函数：cos θ = 邻边/斜边，周期2π。",
            latex = "\\cos\\theta = \\frac{\\text{邻边}}{\\text{斜边}}"
        ),
        SearchItem(
            id = "k024",
            title = "正切函数",
            topic = "三角函数",
            grade = "高一",
            category = "knowledge",
            content = "正切函数：tan θ = sinθ/cosθ，周期π。",
            latex = "\\tan\\theta = \\frac{\\sin\\theta}{\\cos\\theta}"
        ),
        
        // 高二 - 求导公式
        SearchItem(
            id = "k025",
            title = "导数定义",
            topic = "导数",
            grade = "高二",
            category = "knowledge",
            content = "导数定义：函数增量与自变量增量之比的极限。",
            latex = "f'(x) = \\lim_{\\Delta x \\to 0} \\frac{f(x+\\Delta x) - f(x)}{\\Delta x}"
        ),
        SearchItem(
            id = "k026",
            title = "幂函数求导",
            topic = "导数",
            grade = "高二",
            category = "knowledge",
            content = "幂函数求导：(xⁿ)' = nxⁿ⁻¹。",
            latex = "(x^n)' = nx^{n-1}"
        ),
        SearchItem(
            id = "k027",
            title = "链式法则",
            topic = "导数",
            grade = "高二",
            category = "knowledge",
            content = "链式法则：复合函数的导数等于外层函数对中间变量的导数乘中间变量对自变量的导数。",
            latex = "\\frac{dy}{dx} = \\frac{dy}{du} \\cdot \\frac{du}{dx}"
        ),
        
        // 高三 - 不等式
        SearchItem(
            id = "k028",
            title = "基本不等式",
            topic = "不等式",
            grade = "高三",
            category = "knowledge",
            content = "基本不等式：a² + b² ≥ 2ab，当且仅当a=b时取等。",
            latex = "a^2 + b^2 \\ge 2ab"
        ),
        SearchItem(
            id = "k029",
            title = "均值不等式",
            topic = "不等式",
            grade = "高三",
            category = "knowledge",
            content = "均值不等式：算术平均数不小于几何平均数。",
            latex = "\\frac{a+b}{2} \\ge \\sqrt{ab}"
        )
    )
    
    private val problemItems = listOf(
        SearchItem(
            id = "p001",
            title = "小数乘法应用",
            topic = "应用题",
            grade = "五年级",
            category = "problem",
            content = "小明买了2.5千克苹果，每千克6.8元，需要付多少元？",
            latex = "2.5 \\times 6.8 = ?"
        ),
        SearchItem(
            id = "p002",
            title = "分数除法应用",
            topic = "应用题",
            grade = "六年级",
            category = "problem",
            content = "一桶油重4/5千克，用去1/4，还剩多少千克？",
            latex = "\\frac{4}{5} - \\frac{4}{5} \\times \\frac{1}{4}"
        ),
        SearchItem(
            id = "p003",
            title = "百分数应用",
            topic = "应用题",
            grade = "六年级",
            category = "problem",
            content = "某商品原价200元，打八折后售价多少？",
            latex = "200 \\times 80\\% = ?"
        ),
        SearchItem(
            id = "p004",
            title = "比例应用",
            topic = "应用题",
            grade = "六年级",
            category = "problem",
            content = "一块地按3:5比例种植玉米和小麦，种玉米15亩，小麦种多少亩？",
            latex = "\\frac{3}{5} = \\frac{15}{x} \\Rightarrow x = 25"
        ),
        SearchItem(
            id = "p005",
            title = "绝对值方程",
            topic = "方程",
            grade = "初一",
            category = "problem",
            content = "解方程：|x-3| = 5",
            latex = "|x - 3| = 5 \\Rightarrow x - 3 = \\pm 5"
        ),
        SearchItem(
            id = "p006",
            title = "幂的运算",
            topic = "计算",
            grade = "初一",
            category = "problem",
            content = "计算：2³ × 2⁴ = ?",
            latex = "2^3 \\times 2^4 = 2^{3+4} = 2^7"
        ),
        SearchItem(
            id = "p007",
            title = "一次函数图像",
            topic = "函数",
            grade = "初二",
            category = "problem",
            content = "求直线y=2x+1与x轴交点坐标。",
            latex = "y=0 \\Rightarrow 2x+1=0 \\Rightarrow x=-\\frac{1}{2}"
        ),
        SearchItem(
            id = "p008",
            title = "二次函数顶点",
            topic = "函数",
            grade = "初三",
            category = "problem",
            content = "求y=x²-4x+3的顶点坐标。",
            latex = "顶点 = (-\\frac{b}{2a}, \\frac{4ac-b^2}{4a})"
        ),
        SearchItem(
            id = "p009",
            title = "三角函数值",
            topic = "计算",
            grade = "高一",
            category = "problem",
            content = "已知sinθ=3/5，cosθ=?（θ在第一象限）",
            latex = "\\cos\\theta = \\sqrt{1 - \\sin^2\\theta} = \\frac{4}{5}"
        ),
        SearchItem(
            id = "p010",
            title = "求函数导数",
            topic = "导数",
            grade = "高二",
            category = "problem",
            content = "求y=x³+2x的导数。",
            latex = "y' = 3x^2 + 2"
        ),
        SearchItem(
            id = "p011",
            title = "基本不等式证明",
            topic = "证明",
            grade = "高三",
            category = "problem",
            content = "证明：a²+b²≥2ab",
            latex = "a^2 + b^2 - 2ab = (a-b)^2 \\ge 0"
        ),
        SearchItem(
            id = "p012",
            title = "解二次不等式",
            topic = "不等式",
            grade = "高一",
            category = "problem",
            content = "解不等式：x²-5x+6<0",
            latex = "(x-2)(x-3) < 0 \\Rightarrow 2 < x < 3"
        )
    )
    
    val allItems: List<SearchItem> = knowledgeItems + problemItems
    
    // Filter options
    val grades = listOf("全部", "五年级", "六年级", "初一", "初二", "初三", "高一", "高二", "高三")
    val topics = listOf("全部", "数与运算", "比和比例", "有理数", "有理数运算", "整式运算", "函数", "三角函数", "导数", "不等式", "应用题", "方程", "计算", "证明")
    val categories = listOf("全部", "知识点", "例题")
    
    fun search(query: String, grade: String, topic: String, category: String): List<SearchItem> {
        if (query.isBlank()) return emptyList()
        
        val lowerQuery = query.lowercase()
        
        return allItems.filter { item ->
            val matchesGrade = grade == "全部" || item.grade == grade
            val matchesTopic = topic == "全部" || item.topic == topic
            val matchesCategory = category == "全部" || 
                (category == "知识点" && item.category == "knowledge") ||
                (category == "例题" && item.category == "problem")
            
            val matchesQuery = item.title.contains(query, ignoreCase = true) ||
                item.content.contains(query, ignoreCase = true) ||
                item.topic.contains(query, ignoreCase = true) ||
                (item.latex?.contains(lowerQuery) == true)
            
            matchesGrade && matchesTopic && matchesCategory && matchesQuery
        }
    }
}

// ============================================================
// Search Screen
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("全部") }
    var selectedTopic by remember { mutableStateOf("全部") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var searchResults by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    
    // Debounced search
    LaunchedEffect(searchQuery, selectedGrade, selectedTopic, selectedCategory) {
        isSearching = true
        delay(300) // 300ms debounce
        searchResults = SearchDataSource.search(
            query = searchQuery,
            grade = selectedGrade,
            topic = selectedTopic,
            category = selectedCategory
        )
        isSearching = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索知识点、例题...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                // Filter toggle button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "筛选",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = showFilters,
                        onClick = { showFilters = !showFilters },
                        label = { Text(if (showFilters) "收起" else "展开筛选") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                
                // Filter options
                if (showFilters) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category filter
                        FilterSection(
                            title = "类型",
                            options = SearchDataSource.categories,
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it }
                        )
                        
                        // Grade filter
                        FilterSection(
                            title = "年级",
                            options = SearchDataSource.grades,
                            selected = selectedGrade,
                            onSelect = { selectedGrade = it }
                        )
                        
                        // Topic filter
                        FilterSection(
                            title = "专题",
                            options = SearchDataSource.topics,
                            selected = selectedTopic,
                            onSelect = { selectedTopic = it }
                        )
                    }
                }
            }
        }
        
        // Results count
        if (searchQuery.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSearching) "搜索中..." else "找到 ${searchResults.size} 个结果",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Quick clear filters
                if (selectedGrade != "全部" || selectedTopic != "全部" || selectedCategory != "全部") {
                    TextButton(
                        onClick = {
                            selectedGrade = "全部"
                            selectedTopic = "全部"
                            selectedCategory = "全部"
                        }
                    ) {
                        Text("清除筛选", fontSize = 12.sp)
                    }
                }
            }
        }
        
        // Results
        if (searchResults.isEmpty() && searchQuery.isNotEmpty() && !isSearching) {
            // No results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "未找到相关结果",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "试试其他关键词或调整筛选条件",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.id }) { item ->
                    SearchResultCard(
                        item = item,
                        highlightQuery = searchQuery
                    )
                }
            }
        }
        
        // Empty state hint
        if (searchQuery.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "输入关键词搜索",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "支持知识点名称、公式、内容关键词",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Hot search tags
                    Text(
                        text = "热门搜索",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        listOf("分数", "函数", "三角函数", "导数").forEach { tag ->
                            SuggestionChip(
                                onClick = { searchQuery = tag },
                                label = { Text(tag, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option, fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchItem,
    highlightQuery: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category badge
                    Surface(
                        color = if (item.category == "knowledge") 
                            Color(0xFFE3F2FD) else Color(0xFFFCE4EC),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (item.category == "knowledge") "知识点" else "例题",
                            fontSize = 10.sp,
                            color = if (item.category == "knowledge") 
                                Color(0xFF1976D2) else Color(0xFFC2185B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Title with highlight
                    Text(
                        text = highlightText(item.title, highlightQuery),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Grade badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.grade,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Topic
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.topic,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content preview
            Text(
                text = highlightText(item.content, highlightQuery),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            
            // LaTeX formula if exists
            if (!item.latex.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    KaTeXWebView(
                        latex = item.latex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }
    }
}

// Highlight matching text
@Composable
private fun highlightText(text: String, query: String) = buildAnnotatedString {
    if (query.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var lastIndex = 0
    
    var index = lowerText.indexOf(lowerQuery)
    while (index >= 0) {
        // Append text before match
        append(text.substring(lastIndex, index))
        // Append highlighted match
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            append(text.substring(index, index + query.length))
        }
        lastIndex = index + query.length
        index = lowerText.indexOf(lowerQuery, lastIndex)
    }
    
    // Append remaining text
    append(text.substring(lastIndex))
}