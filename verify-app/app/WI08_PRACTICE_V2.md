# 修复后的例题练习模块代码

## 修复后的完整代码

### 1. 数据层修复（问题1、问题3）

```kotlin
// data/local/entity/ProblemEntity.kt
@Entity(tableName = "problems")
data class ProblemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String, // 包含LaTeX的题目内容
    val options: String, // JSON格式的选项列表
    val correctAnswer: Int, // 正确答案索引
    val explanation: String, // 解析内容
    val difficulty: Int,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

// data/local/dao/ProblemDao.kt
@Dao
interface ProblemDao {
    @Query("SELECT * FROM problems WHERE category = :category ORDER BY RANDOM() LIMIT :limit")
    fun getProblemsByCategory(category: String, limit: Int): Flow<List<ProblemEntity>>
    
    @Query("SELECT * FROM problems WHERE id = :problemId")
    fun getProblemById(problemId: String): Flow<ProblemEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: ProblemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<ProblemEntity>)
    
    @Update
    suspend fun updateProblem(problem: ProblemEntity)
    
    @Delete
    suspend fun deleteProblem(problem: ProblemEntity)
    
    @Query("DELETE FROM problems")
    suspend fun deleteAllProblems()
}

// data/repository/ProblemRepository.kt
class ProblemRepository @Inject constructor(
    private val problemDao: ProblemDao,
    private val json: Json
) {
    fun getProblemsByCategory(category: String, limit: Int): Flow<List<Problem>> {
        return problemDao.getProblemsByCategory(category, limit)
            .map { entities ->
                entities.map { entity ->
                    entity.toDomain(json)
                }
            }
    }
    
    fun getProblemById(problemId: String): Flow<Problem> {
        return problemDao.getProblemById(problemId)
            .map { entity ->
                entity.toDomain(json)
            }
    }
    
    suspend fun insertProblem(problem: Problem) {
        problemDao.insertProblem(problem.toEntity(json))
    }
    
    suspend fun insertProblems(problems: List<Problem>) {
        problemDao.insertProblems(problems.map { it.toEntity(json) })
    }
    
    suspend fun updateProblem(problem: Problem) {
        problemDao.updateProblem(problem.toEntity(json))
    }
    
    suspend fun deleteProblem(problem: Problem) {
        problemDao.deleteProblem(problem.toEntity(json))
    }
    
    suspend fun deleteAllProblems() {
        problemDao.deleteAllProblems()
    }
}

// data/mapper/ProblemMapper.kt
fun ProblemEntity.toDomain(json: Json): Problem {
    return Problem(
        id = id,
        title = title,
        content = content,
        options = json.decodeFromString<List<Option>>(options),
        correctAnswer = correctAnswer,
        explanation = explanation,
        difficulty = difficulty,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Problem.toEntity(json: Json): ProblemEntity {
    return ProblemEntity(
        id = id,
        title = title,
        content = content,
        options = json.encodeToString(options),
        correctAnswer = correctAnswer,
        explanation = explanation,
        difficulty = difficulty,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
```

### 2. 领域层修复（问题2）

```kotlin
// domain/model/Problem.kt
data class Problem(
    val id: String,
    val title: String,
    val content: String,
    val options: List<Option>,
    val correctAnswer: Int,
    val explanation: String,
    val difficulty: Int,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

// domain/model/Option.kt
@Serializable
data class Option(
    val id: Int,
    val content: String, // 包含LaTeX的选项内容
    val isCorrect: Boolean = false
)

// domain/model/PracticeSession.kt
data class PracticeSession(
    val id: String,
    val problems: List<Problem>,
    val currentIndex: Int = 0,
    val answers: Map<String, Int> = emptyMap(), // 问题ID -> 用户选择的选项索引
    val results: Map<String, Boolean> = emptyMap(), // 问题ID -> 是否正确
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val score: Int = 0,
    val totalQuestions: Int = problems.size
)
```

### 3. ViewModel修复（问题6）

```kotlin
// presentation/practice/PracticeViewModel.kt
@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val problemRepository: ProblemRepository,
    private val json: Json
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    
    private val _currentSession = MutableStateFlow<PracticeSession?>(null)
    val currentSession: StateFlow<PracticeSession?> = _currentSession.asStateFlow()
    
    init {
        loadProblems()
    }
    
    private fun loadProblems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            problemRepository.getProblemsByCategory("algebra", 10)
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
                .collect { problems ->
                    if (problems.isNotEmpty()) {
                        val session = PracticeSession(
                            id = UUID.randomUUID().toString(),
                            problems = problems
                        )
                        _currentSession.value = session
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                currentProblem = problems.firstOrNull(),
                                totalProblems = problems.size,
                                currentProblemIndex = 0
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "没有找到练习题"
                            )
                        }
                    }
                }
        }
    }
    
    fun selectAnswer(problemId: String, optionIndex: Int) {
        val session = _currentSession.value ?: return
        
        val updatedAnswers = session.answers.toMutableMap().apply {
            put(problemId, optionIndex)
        }
        
        val problem = session.problems.find { it.id == problemId }
        val isCorrect = problem?.correctAnswer == optionIndex
        
        val updatedResults = session.results.toMutableMap().apply {
            put(problemId, isCorrect)
        }
        
        val updatedSession = session.copy(
            answers = updatedAnswers,
            results = updatedResults
        )
        
        _currentSession.value = updatedSession
        
        // 更新UI状态
        _uiState.update { state ->
            state.copy(
                selectedAnswer = optionIndex,
                isAnswerSubmitted = true,
                isCorrect = isCorrect
            )
        }
    }
    
    fun nextProblem() {
        val session = _currentSession.value ?: return
        val nextIndex = session.currentIndex + 1
        
        if (nextIndex < session.problems.size) {
            val nextProblem = session.problems[nextIndex]
            val updatedSession = session.copy(currentIndex = nextIndex)
            
            _currentSession.value = updatedSession
            _uiState.update { state ->
                state.copy(
                    currentProblem = nextProblem,
                    currentProblemIndex = nextIndex,
                    selectedAnswer = null,
                    isAnswerSubmitted = false,
                    isCorrect = null
                )
            }
        } else {
            // 练习完成
            val finalSession = session.copy(
                endTime = System.currentTimeMillis(),
                score = session.results.values.count { it }
            )
            _currentSession.value = finalSession
            _uiState.update { state ->
                state.copy(
                    isPracticeCompleted = true,
                    score = finalSession.score,
                    totalQuestions = finalSession.totalQuestions
                )
            }
        }
    }
    
    fun previousProblem() {
        val session = _currentSession.value ?: return
        val previousIndex = session.currentIndex - 1
        
        if (previousIndex >= 0) {
            val previousProblem = session.problems[previousIndex]
            val updatedSession = session.copy(currentIndex = previousIndex)
            
            _currentSession.value = updatedSession
            _uiState.update { state ->
                state.copy(
                    currentProblem = previousProblem,
                    currentProblemIndex = previousIndex,
                    selectedAnswer = session.answers[previousProblem.id],
                    isAnswerSubmitted = session.answers.containsKey(previousProblem.id),
                    isCorrect = session.results[previousProblem.id]
                )
            }
        }
    }
    
    fun resetPractice() {
        // 重置所有状态
        _currentSession.value = null
        _uiState.value = PracticeUiState()
        loadProblems()
    }
    
    fun showExplanation() {
        _uiState.update { it.copy(showExplanation = true) }
    }
    
    fun hideExplanation() {
        _uiState.update { it.copy(showExplanation = false) }
    }
}

// presentation/practice/PracticeUiState.kt
data class PracticeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentProblem: Problem? = null,
    val currentProblemIndex: Int = 0,
    val totalProblems: Int = 0,
    val selectedAnswer: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val isCorrect: Boolean? = null,
    val showExplanation: Boolean = false,
    val isPracticeCompleted: Boolean = false,
    val score: Int = 0,
    val totalQuestions: Int = 0
)
```

### 4. UI层修复（问题4、问题5）

```kotlin
// presentation/practice/ProblemDetailScreen.kt
@Composable
fun ProblemDetailScreen(
    viewModel: PracticeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPracticeCompleted: (Int, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.isPracticeCompleted) {
        if (uiState.isPracticeCompleted) {
            onPracticeCompleted(uiState.score, uiState.totalQuestions)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("例题练习") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        text = "${uiState.currentProblemIndex + 1}/${uiState.totalProblems}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.error ?: "未知错误",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetPractice() }) {
                        Text("重试")
                    }
                }
            }
        } else {
            uiState.currentProblem?.let { problem ->
                ProblemContent(
                    problem = problem,
                    uiState = uiState,
                    onSelectAnswer = { optionIndex ->
                        viewModel.selectAnswer(problem.id, optionIndex)
                    },
                    onNextProblem = { viewModel.nextProblem() },
                    onPreviousProblem = { viewModel.previousProblem() },
                    onShowExplanation = { viewModel.showExplanation() },
                    onHideExplanation = { viewModel.hideExplanation() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun ProblemContent(
    problem: Problem,
    uiState: PracticeUiState,
    onSelectAnswer: (Int) -> Unit,
    onNextProblem: () -> Unit,
    onPreviousProblem: () -> Unit,
    onShowExplanation: () -> Unit,
    onHideExplanation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 题目标题
        Text(
            text = problem.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // 题目内容（支持KaTeX渲染）
        KaTeXText(
            text = problem.content,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 选项列表
        Text(
            text = "请选择答案：",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        problem.options.forEachIndexed { index, option ->
            OptionItem(
                option = option,
                isSelected = uiState.selectedAnswer == index,
                isCorrect = uiState.isAnswerSubmitted && index == problem.correctAnswer,
                isWrong = uiState.isAnswerSubmitted && uiState.selectedAnswer == index && !uiState.isCorrect,
                onClick = { onSelectAnswer(index) },
                enabled = !uiState.isAnswerSubmitted
            )
        }
        
        // 答案反馈
        if (uiState.isAnswerSubmitted) {
            AnswerFeedback(
                isCorrect = uiState.isCorrect ?: false,
                correctAnswer = problem.options[problem.correctAnswer].content
            )
        }
        
        // 解析部分
        if (uiState.isAnswerSubmitted) {
            ExplanationSection(
                explanation = problem.explanation,
                showExplanation = uiState.showExplanation,
                onToggleExplanation = {
                    if (uiState.showExplanation) {
                        onHideExplanation()
                    } else {
                        onShowExplanation()
                    }
                }
            )
        }
        
        // 导航按钮
        NavigationButtons(
            currentIndex = uiState.currentProblemIndex,
            totalProblems = uiState.totalProblems,
            isAnswerSubmitted = uiState.isAnswerSubmitted,
            onPrevious = onPreviousProblem,
            onNext = onNextProblem
        )
    }
}

@Composable
fun OptionItem(
    option: Option,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val backgroundColor = when {
        isCorrect -> MaterialTheme.colorScheme.primaryContainer
        isWrong -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val borderColor = when {
        isCorrect -> MaterialTheme.colorScheme.primary
        isWrong -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = enabled
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            KaTeXText(
                text = option.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            if (isCorrect) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "正确",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (isWrong) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "错误",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AnswerFeedback(
    isCorrect: Boolean,
    correctAnswer: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isCorrect) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (isCorrect) "回答正确！" else "回答错误",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            
            if (!isCorrect) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "正确答案：",
                    style = MaterialTheme.typography.bodyMedium
                )
                KaTeXText(
                    text = correctAnswer,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ExplanationSection(
    explanation: String,
    showExplanation: Boolean,
    onToggleExplanation: () -> Unit
) {
    Column {
        Button(
            onClick = onToggleExplanation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (showExplanation) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (showExplanation) "隐藏解析" else "查看解析")
        }
        
        if (showExplanation) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "解析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    KaTeXText(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationButtons(
    currentIndex: Int,
    totalProblems: Int,
    isAnswerSubmitted: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onPrevious,
            enabled = currentIndex > 0,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "上一题")
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Button(
            onClick = onNext,
            enabled = isAnswerSubmitted,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = if (currentIndex == totalProblems - 1) "完成" else "下一题")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (currentIndex == totalProblems - 1) 
                    Icons.Default.Check 
                else 
                    Icons.Default.ArrowForward,
                contentDescription = null
            )
        }
    }
}
```

### 5. KaTeX渲染组件（问题4）

```kotlin
// presentation/components/KaTeXText.kt
@Composable
fun KaTeXText(
    text: String,
    style: TextStyle = TextStyle.Default,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val context = LocalContext.current
    
    // 解析文本中的LaTeX表达式
    val segments = remember(text) {
        parseKaTeXSegments(text)
    }
    
    Column(modifier = modifier) {
        segments.forEach { segment ->
            when (segment) {
                is KaTeXSegment.Text -> {
                    Text(
                        text = segment.content,
                        style = style,
                        color = if (color != Color.Unspecified) color else style.color
                    )
                }
                is KaTeXSegment.LaTeX -> {
                    KaTeXView(
                        latex = segment.content,
                        style = style,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun KaTeXView(
    latex: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember { WebView(context) }
    
    LaunchedEffect(latex) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
                <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
                <style>
                    body {
                        margin: 0;
                        padding: 8px;
                        font-family: sans-serif;
                        font-size: ${style.fontSize?.value ?: 16f}px;
                        line-height: 1.5;
                    }
                    .katex-display {
                        margin: 8px 0;
                    }
                </style>
            </head>
            <body>
                <div id="content">$latex</div>
                <script>
                    renderMathInElement(document.getElementById('content'), {
                        delimiters: [
                            {left: '$$', right: '$$', display: true},
                            {left: '$', right: '$', display: false},
                            {left: '\\(', right: '\\)', display: false},
                            {left: '\\[', right: '\\]', display: true}
                        ],
                        throwOnError: false
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(
            null,
            html,
            "text/html",
            "UTF-8",
            null
        )
    }
    
    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent),
        update = { webView ->
            // 如果需要更新，可以在这里处理
        }
    )
}

// KaTeX解析器
sealed class KaTeXSegment {
    data class Text(val content: String) : KaTeXSegment()
    data class LaTeX(val content: String) : KaTeXSegment()
}

fun parseKaTeXSegments(text: String): List<KaTeXSegment> {
    val segments = mutableListOf<KaTeXSegment>()
    val pattern = Regex("""(\$\$[\s\S]*?\$\$|\$[^$]+?\$|\\\([\s\S]*?\\\)|\\\[[\s\S]*?\\\])""")
    
    var lastIndex = 0
    pattern.findAll(text).forEach { match ->
        // 添加LaTeX表达式前的文本
        if (match.range.first > lastIndex) {
            val textBefore = text.substring(lastIndex, match.range.first)
            if (textBefore.isNotBlank()) {
                segments.add(KaTeXSegment.Text(textBefore))
            }
        }
        
        // 添加LaTeX表达式
        val latex = match.value
        segments.add(KaTeXSegment.LaTeX(latex))
        
        lastIndex = match.range.last + 1
    }
    
    // 添加最后的文本
    if (lastIndex < text.length) {
        val textAfter = text.substring(lastIndex)
        if (textAfter.isNotBlank()) {
            segments.add(KaTeXSegment.Text(textAfter))
        }
    }
    
    // 如果没有找到任何LaTeX表达式，返回整个文本
    if (segments.isEmpty()) {
        segments.add(KaTeXSegment.Text(text))
    }
    
    return segments
}
```

### 6. 依赖配置

```gradle
// build.gradle.kts (app)
dependencies {
    // Room数据库
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Kotlin序列