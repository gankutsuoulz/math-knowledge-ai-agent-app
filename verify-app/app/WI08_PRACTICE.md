# 例题练习模块代码

## 1. 领域层 (Domain)

### 实体类
```kotlin
// com.mathknowledge.app.domain.model/Problem.kt
package com.mathknowledge.app.domain.model

data class Problem(
    val id: String,
    val title: String,
    val content: String,
    val type: ProblemType,
    val difficulty: Difficulty,
    val category: String,
    val options: List<String>? = null,
    val correctAnswer: String,
    val explanation: String,
    val formula: String? = null
)

enum class ProblemType {
    MULTIPLE_CHOICE,  // 选择题
    FILL_IN_BLANK,    // 填空题
    SHORT_ANSWER      // 解答题
}

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

data class UserAnswer(
    val problemId: String,
    val answer: String,
    val isCorrect: Boolean,
    val timeSpent: Long // 毫秒
)
```

### Repository接口
```kotlin
// com.mathknowledge.app.domain.repository/PracticeRepository.kt
package com.mathknowledge.app.domain.repository

import com.mathknowledge.app.domain.model.Difficulty
import com.mathknowledge.app.domain.model.Problem
import com.mathknowledge.app.domain.model.UserAnswer
import kotlinx.coroutines.flow.Flow

interface PracticeRepository {
    fun getProblems(category: String? = null, difficulty: Difficulty? = null): Flow<List<Problem>>
    fun getProblemById(id: String): Flow<Problem?>
    suspend fun submitAnswer(userAnswer: UserAnswer): Boolean
    fun getUserAnswers(): Flow<List<UserAnswer>>
    suspend fun getProblemStatistics(): Flow<Map<String, Int>>
}
```

### Use Cases
```kotlin
// com.mathknowledge.app.domain.usecase/GetProblemsUseCase.kt
package com.mathknowledge.app.domain.usecase

import com.mathknowledge.app.domain.model.Difficulty
import com.mathknowledge.app.domain.model.Problem
import com.mathknowledge.app.domain.repository.PracticeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProblemsUseCase @Inject constructor(
    private val repository: PracticeRepository
) {
    operator fun invoke(
        category: String? = null,
        difficulty: Difficulty? = null
    ): Flow<List<Problem>> {
        return repository.getProblems(category, difficulty)
    }
}

// com.mathknowledge.app.domain.usecase/SubmitAnswerUseCase.kt
package com.mathknowledge.app.domain.usecase

import com.mathknowledge.app.domain.model.UserAnswer
import com.mathknowledge.app.domain.repository.PracticeRepository
import javax.inject.Inject

class SubmitAnswerUseCase @Inject constructor(
    private val repository: PracticeRepository
) {
    suspend operator fun invoke(userAnswer: UserAnswer): Boolean {
        return repository.submitAnswer(userAnswer)
    }
}
```

## 2. 数据层 (Data)

### Repository实现
```kotlin
// com.mathknowledge.app.data.repository/PracticeRepositoryImpl.kt
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.ProblemDao
import com.mathknowledge.app.data.remote.ProblemApi
import com.mathknowledge.app.domain.model.Difficulty
import com.mathknowledge.app.domain.model.Problem
import com.mathknowledge.app.domain.model.UserAnswer
import com.mathknowledge.app.domain.repository.PracticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PracticeRepositoryImpl @Inject constructor(
    private val problemDao: ProblemDao,
    private val problemApi: ProblemApi
) : PracticeRepository {
    
    override fun getProblems(category: String?, difficulty: Difficulty?): Flow<List<Problem>> = flow {
        // 实际项目中应从本地数据库或网络获取
        val problems = problemDao.getProblems(category, difficulty)
        emit(problems)
    }
    
    override fun getProblemById(id: String): Flow<Problem?> = flow {
        val problem = problemDao.getProblemById(id)
        emit(problem)
    }
    
    override suspend fun submitAnswer(userAnswer: UserAnswer): Boolean {
        // 验证答案并保存
        val isCorrect = problemDao.validateAnswer(userAnswer.problemId, userAnswer.answer)
        problemDao.saveUserAnswer(userAnswer.copy(isCorrect = isCorrect))
        return isCorrect
    }
    
    override fun getUserAnswers(): Flow<List<UserAnswer>> = flow {
        val answers = problemDao.getUserAnswers()
        emit(answers)
    }
    
    override suspend fun getProblemStatistics(): Flow<Map<String, Int>> = flow {
        val stats = problemDao.getProblemStatistics()
        emit(stats)
    }
}
```

### 本地数据源
```kotlin
// com.mathknowledge.app.data.local/ProblemDao.kt
package com.mathknowledge.app.data.local

import androidx.room.*
import com.mathknowledge.app.domain.model.Difficulty
import com.mathknowledge.app.domain.model.Problem
import com.mathknowledge.app.domain.model.UserAnswer
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problems WHERE (:category IS NULL OR category = :category) AND (:difficulty IS NULL OR difficulty = :difficulty)")
    fun getProblems(category: String?, difficulty: Difficulty?): List<Problem>
    
    @Query("SELECT * FROM problems WHERE id = :id")
    suspend fun getProblemById(id: String): Problem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: Problem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserAnswer(userAnswer: UserAnswer)
    
    @Query("SELECT * FROM user_answers")
    fun getUserAnswers(): Flow<List<UserAnswer>>
    
    @Query("SELECT category, COUNT(*) as count FROM problems GROUP BY category")
    suspend fun getProblemStatistics(): Map<String, Int>
    
    @Query("SELECT correctAnswer FROM problems WHERE id = :problemId")
    suspend fun getCorrectAnswer(problemId: String): String?
    
    suspend fun validateAnswer(problemId: String, userAnswer: String): Boolean {
        val correctAnswer = getCorrectAnswer(problemId)
        return correctAnswer?.equals(userAnswer, ignoreCase = true) ?: false
    }
}
```

### 数据库实体
```kotlin
// com.mathknowledge.app.data.local/ProblemEntity.kt
package com.mathknowledge.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mathknowledge.app.domain.model.Difficulty
import com.mathknowledge.app.domain.model.Problem
import com.mathknowledge.app.domain.model.ProblemType

@Entity(tableName = "problems")
data class ProblemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val type: ProblemType,
    val difficulty: Difficulty,
    val category: String,
    val options: String?, // JSON格式存储
    val correctAnswer: String,
    val explanation: String,
    val formula: String?
) {
    fun toDomain(): Problem {
        return Problem(
            id = id,
            title = title,
            content = content,
            type = type,
            difficulty = difficulty,
            category = category,
            options = options?.split(","),
            correctAnswer = correctAnswer,
            explanation = explanation,
            formula = formula
        )
    }
    
    companion object {
        fun fromDomain(problem: Problem): ProblemEntity {
            return ProblemEntity(
                id = problem.id,
                title = problem.title,
                content = problem.content,
                type = problem.type,
                difficulty = problem.difficulty,
                category = problem.category,
                options = problem.options?.joinToString(","),
                correctAnswer = problem.correctAnswer,
                explanation = problem.explanation,
                formula = problem.formula
            )
        }
    }
}

@Entity(tableName = "user_answers")
data class UserAnswerEntity(
    @PrimaryKey
    val problemId: String,
    val answer: String,
    val isCorrect: Boolean,
    val timeSpent: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): UserAnswer {
        return UserAnswer(
            problemId = problemId,
            answer = answer,
            isCorrect = isCorrect,
            timeSpent = timeSpent
        )
    }
}
```

## 3. 表现层 (Presentation)

### ViewModel
```kotlin
// com.mathknowledge.app.presentation.practice/PracticeViewModel.kt
package com.mathknowledge.app.presentation.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathknowledge.app.domain.model.Difficulty
import com.mathknowledge.app.domain.model.Problem
import com.mathknowledge.app.domain.model.ProblemType
import com.mathknowledge.app.domain.model.UserAnswer
import com.mathknowledge.app.domain.usecase.GetProblemsUseCase
import com.mathknowledge.app.domain.usecase.SubmitAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PracticeUiState(
    val isLoading: Boolean = true,
    val problems: List<Problem> = emptyList(),
    val currentProblem: Problem? = null,
    val userAnswer: String = "",
    val isCorrect: Boolean? = null,
    val showExplanation: Boolean = false,
    val selectedCategory: String? = null,
    val selectedDifficulty: Difficulty? = null,
    val error: String? = null,
    val currentIndex: Int = 0,
    val totalProblems: Int = 0,
    val correctCount: Int = 0,
    val startTime: Long = 0L
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val getProblemsUseCase: GetProblemsUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    
    init {
        loadProblems()
    }
    
    fun loadProblems(category: String? = null, difficulty: Difficulty? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getProblemsUseCase(category, difficulty)
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = e.message ?: "加载题目失败"
                        ) 
                    }
                }
                .collect { problems ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            problems = problems,
                            totalProblems = problems.size,
                            currentProblem = problems.firstOrNull(),
                            currentIndex = 0,
                            startTime = System.currentTimeMillis()
                        ) 
                    }
                }
        }
    }
    
    fun updateAnswer(answer: String) {
        _uiState.update { it.copy(userAnswer = answer) }
    }
    
    fun submitAnswer() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val problem = currentState.currentProblem ?: return@launch
            
            val userAnswer = UserAnswer(
                problemId = problem.id,
                answer = currentState.userAnswer,
                isCorrect = false, // 将在提交后更新
                timeSpent = System.currentTimeMillis() - currentState.startTime
            )
            
            val isCorrect = submitAnswerUseCase(userAnswer)
            
            _uiState.update { 
                it.copy(
                    isCorrect = isCorrect,
                    showExplanation = true,
                    correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount
                ) 
            }
        }
    }
    
    fun nextProblem() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentIndex + 1
        
        if (nextIndex < currentState.problems.size) {
            _uiState.update { 
                it.copy(
                    currentIndex = nextIndex,
                    currentProblem = currentState.problems[nextIndex],
                    userAnswer = "",
                    isCorrect = null,
                    showExplanation = false,
                    startTime = System.currentTimeMillis()
                ) 
            }
        }
    }
    
    fun previousProblem() {
        val currentState = _uiState.value
        val prevIndex = currentState.currentIndex - 1
        
        if (prevIndex >= 0) {
            _uiState.update { 
                it.copy(
                    currentIndex = prevIndex,
                    currentProblem = currentState.problems[prevIndex],
                    userAnswer = "",
                    isCorrect = null,
                    showExplanation = false,
                    startTime = System.currentTimeMillis()
                ) 
            }
        }
    }
    
    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadProblems(category, _uiState.value.selectedDifficulty)
    }
    
    fun selectDifficulty(difficulty: Difficulty?) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
        loadProblems(_uiState.value.selectedCategory, difficulty)
    }
    
    fun resetPractice() {
        _uiState.update { 
            it.copy(
                userAnswer = "",
                isCorrect = null,
                showExplanation = false,
                startTime = System.currentTimeMillis()
            ) 
        }
    }
    
    fun getProgress(): Float {
        val state = _uiState.value
        return if (state.totalProblems > 0) {
            state.currentIndex.toFloat() / state.totalProblems
        } else {
            0f
        }
    }
}
```

### 题目列表页面
```kotlin
// com.mathknowledge.app.presentation.practice/ProblemListScreen.kt
package com.mathknowledge.app.presentation.practice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathknowledge.app.domain.model.Difficulty
import com.mathknowledge.app.domain.model.Problem
import com.mathknowledge.app.presentation.components.DifficultyChip
import com.mathknowledge.app.presentation.components.FormulaText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemListScreen(
    onProblemClick: (String) -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("题库浏览") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 筛选条件显示
            FilterChips(
                selectedCategory = uiState.selectedCategory,
                selectedDifficulty = uiState.selectedDifficulty,
                onCategoryClick = { viewModel.selectCategory(it) },
                onDifficultyClick = { viewModel.selectDifficulty(it) }
            )
            
            // 题目列表
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.problems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无题目")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.problems) { problem ->
                        ProblemCard(
                            problem = problem,
                            onClick = { onProblemClick(problem.id) }
                        )
                    }
                }
            }
        }
    }
    
    if (showFilterDialog) {
        FilterDialog(
            selectedCategory = uiState.selectedCategory,
            selectedDifficulty = uiState.selectedDifficulty,
            onDismiss = { showFilterDialog = false },
            onConfirm = { category, difficulty ->
                viewModel.selectCategory(category)
                viewModel.selectDifficulty(difficulty)
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun ProblemCard(
    problem: Problem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = problem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                DifficultyChip(difficulty = problem.difficulty)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 题目内容预览
            if (problem.formula != null) {
                FormulaText(
                    formula = problem.formula,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = problem.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(problem.category) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                AssistChip(
                    onClick = { },
                    label = { Text(problem.type.name) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedCategory: String?,
    selectedDifficulty: Difficulty?,
    onCategoryClick: (String?) -> Unit,
    onDifficultyClick: (Difficulty?) -> Unit
) {
    val categories = listOf("代数", "几何", "概率统计", "数论", "分析")
    val difficulties = Difficulty.values()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 分类筛选
        Text(
            text = "分类",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryClick(null) },
                    label = { Text("全部") }
                )
            }
            
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryClick(category) },
                    label = { Text(category) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 难度筛选
        Text(
            text = "难度",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedDifficulty == null,
                    onClick = { onDifficultyClick(null) },
                    label = { Text("全部") }
                )
            }
            
            items(difficulties.toList()) { difficulty ->
                FilterChip(
                    selected = selectedDifficulty == difficulty,
                    onClick = { onDifficultyClick(difficulty) },
                    label = { Text(difficulty.name) }
                )
            }
        }
    }
}

@Composable
fun FilterDialog(
    selectedCategory: String?,
    selectedDifficulty: Difficulty?,
    onDismiss: () -> Unit,
    onConfirm: (String?, Difficulty?) -> Unit
) {
    var category by remember { mutableStateOf(selectedCategory) }
    var difficulty by remember { mutableStateOf(selectedDifficulty) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选题目") },
        text = {
            Column {
                Text("分类", style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = category == null,
                            onClick = { category = null },
                            label = { Text("全部") }
                        )
                    }
                    items(listOf("代数", "几何", "概率统计", "数论", "分析")) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                
                Text("难度", style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = difficulty == null,
                            onClick = { difficulty = null },
                            label = { Text("全部") }
                        )
                    }
                    items(Difficulty.values().toList()) { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(category, difficulty) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
```

### 做题页面
```kotlin
// com.mathknowledge.app.presentation.practice/ProblemDetailScreen.kt
package com.mathknowledge.app.presentation.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathknowledge.app.domain.model.ProblemType
import com.mathknowledge.app.presentation.components.FormulaText
import com.mathknowledge.app.presentation.components.KatexWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailScreen(
    problemId: String,
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("做题") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 进度指示器
                    Text(
                        text = "${uiState.currentIndex + 1}/${uiState.totalProblems}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // 进度条
            LinearProgressIndicator(
                progress = { viewModel.getProgress() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // 题目信息
            uiState.currentProblem?.let { problem ->
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 题目标题和难度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = problem.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        AssistChip(
                            onClick = { },
                            label = { Text(problem.difficulty.name) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 题目内容
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (problem.formula != null) {
                                // KaTeX公式渲染
                                KatexWebView(
                                    formula = problem.formula,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp)
                                )
                            } else {
                                Text(
                                    text = problem.content,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 答案输入区域
                    when (problem.type) {
                        ProblemType.MULTIPLE_CHOICE -> {
                            MultipleChoiceInput(
                                options = problem.options ?: emptyList(),
                                selectedAnswer = uiState.userAnswer,
                                onAnswerSelected = { viewModel.updateAnswer(it) },
                                enabled = uiState.isCorrect == null
                            )
                        }
                        
                        ProblemType.FILL_IN_BLANK -> {
                            FillInBlankInput(
                                answer = uiState.userAnswer,
                                onAnswerChanged = { viewModel.updateAnswer(it) },
                                enabled = uiState.isCorrect == null
                            )
                        }
                        
                        ProblemType.SHORT_ANSWER -> {
                            ShortAnswerInput(
                                answer = uiState.userAnswer,
                                onAnswerChanged = { viewModel.updateAnswer(it) },
                                enabled = uiState.isCorrect == null
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 提交按钮
                    Button(
                        onClick = { viewModel.submitAnswer() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.userAnswer.isNotBlank() && uiState.isCorrect == null
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null