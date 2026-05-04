# 知识点速查模块代码

## 1. 数据模型 (Entity)

```kotlin
// data/model/KnowledgePoint.kt
package com.mathknowledge.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_points")
data class KnowledgePoint(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String, // 包含KaTeX公式的Markdown内容
    val category: String,
    val subcategory: String? = null,
    val difficulty: Int = 1, // 1-5
    val tags: List<String> = emptyList(),
    val relatedPracticeIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// data/model/KnowledgePointCategory.kt
data class KnowledgePointCategory(
    val id: String,
    val name: String,
    val icon: String? = null,
    val count: Int = 0
)

// data/model/SearchResult.kt
data class SearchResult(
    val knowledgePoints: List<KnowledgePoint>,
    val totalCount: Int,
    val searchQuery: String
)
```

## 2. Repository 接口与实现

```kotlin
// domain/repository/KnowledgePointRepository.kt
package com.mathknowledge.app.domain.repository

import com.mathknowledge.app.data.model.KnowledgePoint
import com.mathknowledge.app.data.model.KnowledgePointCategory
import com.mathknowledge.app.data.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface KnowledgePointRepository {
    // 获取所有知识点
    fun getAllKnowledgePoints(): Flow<List<KnowledgePoint>>
    
    // 根据ID获取知识点
    suspend fun getKnowledgePointById(id: String): KnowledgePoint?
    
    // 获取所有分类
    fun getAllCategories(): Flow<List<KnowledgePointCategory>>
    
    // 根据分类获取知识点
    fun getKnowledgePointsByCategory(category: String): Flow<List<KnowledgePoint>>
    
    // 搜索知识点
    suspend fun searchKnowledgePoints(query: String): SearchResult
    
    // 获取相关知识点
    suspend fun getRelatedKnowledgePoints(id: String): List<KnowledgePoint>
    
    // 获取推荐知识点
    suspend fun getRecommendedKnowledgePoints(limit: Int = 5): List<KnowledgePoint>
}

// data/repository/KnowledgePointRepositoryImpl.kt
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.KnowledgePointDao
import com.mathknowledge.app.data.model.KnowledgePoint
import com.mathknowledge.app.data.model.KnowledgePointCategory
import com.mathknowledge.app.data.model.SearchResult
import com.mathknowledge.app.domain.repository.KnowledgePointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgePointRepositoryImpl @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao
) : KnowledgePointRepository {
    
    override fun getAllKnowledgePoints(): Flow<List<KnowledgePoint>> {
        return knowledgePointDao.getAllKnowledgePoints()
    }
    
    override suspend fun getKnowledgePointById(id: String): KnowledgePoint? {
        return knowledgePointDao.getKnowledgePointById(id)
    }
    
    override fun getAllCategories(): Flow<List<KnowledgePointCategory>> {
        return knowledgePointDao.getAllCategories().map { categories ->
            categories.map { category ->
                KnowledgePointCategory(
                    id = category,
                    name = category,
                    count = knowledgePointDao.getKnowledgePointCountByCategory(category)
                )
            }
        }
    }
    
    override fun getKnowledgePointsByCategory(category: String): Flow<List<KnowledgePoint>> {
        return knowledgePointDao.getKnowledgePointsByCategory(category)
    }
    
    override suspend fun searchKnowledgePoints(query: String): SearchResult {
        val results = knowledgePointDao.searchKnowledgePoints(query)
        return SearchResult(
            knowledgePoints = results,
            totalCount = results.size,
            searchQuery = query
        )
    }
    
    override suspend fun getRelatedKnowledgePoints(id: String): List<KnowledgePoint> {
        val knowledgePoint = knowledgePointDao.getKnowledgePointById(id) ?: return emptyList()
        return knowledgePointDao.getKnowledgePointsByCategory(knowledgePoint.category)
            .map { it.filter { kp -> kp.id != id } }
            .first()
    }
    
    override suspend fun getRecommendedKnowledgePoints(limit: Int): List<KnowledgePoint> {
        return knowledgePointDao.getRecommendedKnowledgePoints(limit)
    }
}
```

## 3. DAO (数据访问对象)

```kotlin
// data/local/KnowledgePointDao.kt
package com.mathknowledge.app.data.local

import androidx.room.*
import com.mathknowledge.app.data.model.KnowledgePoint
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgePointDao {
    @Query("SELECT * FROM knowledge_points ORDER BY title ASC")
    fun getAllKnowledgePoints(): Flow<List<KnowledgePoint>>
    
    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    suspend fun getKnowledgePointById(id: String): KnowledgePoint?
    
    @Query("SELECT DISTINCT category FROM knowledge_points ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
    
    @Query("SELECT COUNT(*) FROM knowledge_points WHERE category = :category")
    suspend fun getKnowledgePointCountByCategory(category: String): Int
    
    @Query("SELECT * FROM knowledge_points WHERE category = :category ORDER BY title ASC")
    fun getKnowledgePointsByCategory(category: String): Flow<List<KnowledgePoint>>
    
    @Query("""
        SELECT * FROM knowledge_points 
        WHERE title LIKE '%' || :query || '%' 
        OR content LIKE '%' || :query || '%'
        OR category LIKE '%' || :query || '%'
        OR tags LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    suspend fun searchKnowledgePoints(query: String): List<KnowledgePoint>
    
    @Query("SELECT * FROM knowledge_points ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRecommendedKnowledgePoints(limit: Int): List<KnowledgePoint>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoint(knowledgePoint: KnowledgePoint)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoints(knowledgePoints: List<KnowledgePoint>)
    
    @Update
    suspend fun updateKnowledgePoint(knowledgePoint: KnowledgePoint)
    
    @Delete
    suspend fun deleteKnowledgePoint(knowledgePoint: KnowledgePoint)
    
    @Query("DELETE FROM knowledge_points")
    suspend fun deleteAllKnowledgePoints()
}
```

## 4. ViewModel

```kotlin
// presentation/knowledgepoint/KnowledgePointViewModel.kt
package com.mathknowledge.app.presentation.knowledgepoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathknowledge.app.data.model.KnowledgePoint
import com.mathknowledge.app.data.model.KnowledgePointCategory
import com.mathknowledge.app.domain.repository.KnowledgePointRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgePointUiState(
    val isLoading: Boolean = false,
    val knowledgePoints: List<KnowledgePoint> = emptyList(),
    val categories: List<KnowledgePointCategory> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val searchResults: List<KnowledgePoint> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val selectedKnowledgePoint: KnowledgePoint? = null,
    val relatedKnowledgePoints: List<KnowledgePoint> = emptyList()
)

@HiltViewModel
class KnowledgePointViewModel @Inject constructor(
    private val repository: KnowledgePointRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(KnowledgePointUiState())
    val uiState: StateFlow<KnowledgePointUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 加载分类
                repository.getAllCategories().collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
                
                // 加载所有知识点
                repository.getAllKnowledgePoints().collect { knowledgePoints ->
                    _uiState.update { 
                        it.copy(
                            knowledgePoints = knowledgePoints,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载数据失败"
                    )
                }
            }
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.isBlank()) {
            _uiState.update { 
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false
                )
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            
            try {
                val searchResult = repository.searchKnowledgePoints(query)
                _uiState.update { 
                    it.copy(
                        searchResults = searchResult.knowledgePoints,
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = e.message ?: "搜索失败"
                    )
                }
            }
        }
    }
    
    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        
        viewModelScope.launch {
            try {
                if (category == null) {
                    // 显示所有知识点
                    repository.getAllKnowledgePoints().collect { knowledgePoints ->
                        _uiState.update { it.copy(knowledgePoints = knowledgePoints) }
                    }
                } else {
                    // 显示特定分类的知识点
                    repository.getKnowledgePointsByCategory(category).collect { knowledgePoints ->
                        _uiState.update { it.copy(knowledgePoints = knowledgePoints) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "加载分类数据失败")
                }
            }
        }
    }
    
    fun onKnowledgePointSelected(knowledgePoint: KnowledgePoint) {
        _uiState.update { it.copy(selectedKnowledgePoint = knowledgePoint) }
        
        viewModelScope.launch {
            try {
                val relatedPoints = repository.getRelatedKnowledgePoints(knowledgePoint.id)
                _uiState.update { it.copy(relatedKnowledgePoints = relatedPoints) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "加载相关知识点失败")
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedKnowledgePoint = null,
                relatedKnowledgePoints = emptyList()
            )
        }
    }
    
    fun refreshData() {
        loadInitialData()
    }
}
```

## 5. UI组件

### 5.1 搜索栏组件

```kotlin
// presentation/knowledgepoint/components/SearchBar.kt
package com.mathknowledge.app.presentation.knowledgepoint.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgePointSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索知识点..."
) {
    var isFocused by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChanged("") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch(query) }
        )
    )
}
```

### 5.2 分类Tab组件

```kotlin
// presentation/knowledgepoint/components/CategoryTabs.kt
package com.mathknowledge.app.presentation.knowledgepoint.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mathknowledge.app.data.model.KnowledgePointCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabs(
    categories: List<KnowledgePointCategory>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = categories.indexOfFirst { it.id == selectedCategory }.coerceAtLeast(0),
        modifier = modifier,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        // "全部" Tab
        Tab(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            text = {
                Text(
                    text = "全部",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        )
        
        // 分类 Tab
        categories.forEach { category ->
            Tab(
                selected = selectedCategory == category.id,
                onClick = { onCategorySelected(category.id) },
                text = {
                    Row {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (category.count > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${category.count})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }
}
```

### 5.3 知识点列表项组件

```kotlin
// presentation/knowledgepoint/components/KnowledgePointListItem.kt
package com.mathknowledge.app.presentation.knowledgepoint.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mathknowledge.app.data.model.KnowledgePoint

@Composable
fun KnowledgePointListItem(
    knowledgePoint: KnowledgePoint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题和难度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = knowledgePoint.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 难度指示器
                DifficultyIndicator(difficulty = knowledgePoint.difficulty)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 分类标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = knowledgePoint.category,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
                
                if (knowledgePoint.subcategory != null) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = knowledgePoint.subcategory,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 内容预览（去除Markdown格式）
            Text(
                text = knowledgePoint.content.take(150).replace(Regex("[#*`\\[\\]]"), ""),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 标签
            if (knowledgePoint.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    knowledgePoint.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyIndicator(difficulty: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (index < difficulty) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        }
    }
}
```

### 5.4 KaTeX渲染组件

```kotlin
// presentation/knowledgepoint/components/KatexRenderer.kt
package com.mathknowledge.app.presentation.knowledgepoint.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun KatexRenderer(
    formula: String,
    modifier: Modifier = Modifier,
    displayMode: Boolean = false
) {
    val htmlContent = """
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
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    margin: 0;
                    padding: 8px;
                    background-color: transparent;
                }
                .katex-display {
                    margin: 8px 0;
                    overflow-x: auto;
                }
            </style>
        </head>
        <body>
            <div id="formula"></div>
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    const formulaElement = document.getElementById('formula');
                    try {
                        katex.render(
                            `${formula.replace("`", "\\`").replace("$", "\\$")}`,
                            formulaElement,
                            {
                                displayMode: ${displayMode},
                                throwOnError: false,
                                trust: true
                            }
                        );
                    } catch (e) {
                        formulaElement.innerHTML = `<span style="color: red;">公式渲染错误: ${e.message}</span>`;
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = modifier
    )
}
```

## 6. 页面组件

### 6.1 知识点列表页面

```kotlin
// presentation/knowledgepoint/KnowledgePointListScreen.kt
package com.mathknowledge.app.presentation.knowledgepoint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mathknowledge.app.presentation.knowledgepoint.components.CategoryTabs
import com.mathknowledge.app.presentation.knowledgepoint.components.KnowledgePointListItem
import com.mathknowledge.app.presentation.knowledgepoint.components.KnowledgePointSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgePointListScreen(
    onKnowledgePointClick: (String) -> Unit,
    viewModel: KnowledgePointViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "知识点速查",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏
            KnowledgePointSearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onSearch = viewModel::onSearchQueryChanged,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 分类Tab
            CategoryTabs(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::onCategorySelected,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // 内容区域
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.error != null -> {
                    ErrorContent(
                        message = uiState.error ?: "未知错误",
                        onRetry = viewModel::refreshData,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "搜索中...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                uiState.searchQuery.isNotEmpty() && uiState.searchResults.isEmpty() -> {
                    EmptySearchResults(
                        query = uiState.searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    val displayList = if (uiState.searchQuery.isNotEmpty()) {
                        uiState.searchResults
                    } else {
                        uiState.knowledgePoints
                    }
                    
                    if (displayList.isEmpty()) {
                        EmptyContent(modifier =