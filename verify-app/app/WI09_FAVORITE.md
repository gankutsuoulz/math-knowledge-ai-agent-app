# 收藏夹模块代码实现

## 1. Domain层 - 实体和接口

### `Favorite.kt`
```kotlin
package com.mathknowledge.app.domain.model

import java.time.LocalDateTime

data class Favorite(
    val id: String,
    val contentId: String,
    val contentType: ContentType,
    val title: String,
    val description: String,
    val addedAt: LocalDateTime,
    val notes: String = "",
    val isFavorite: Boolean = true
)

enum class ContentType {
    QUESTION, // 题目
    KNOWLEDGE_POINT // 知识点
}
```

### `FavoriteRepository.kt`
```kotlin
package com.mathknowledge.app.domain.repository

import com.mathknowledge.app.domain.model.ContentType
import com.mathknowledge.app.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavorites(): Flow<List<Favorite>>
    fun getFavoritesByType(contentType: ContentType): Flow<List<Favorite>>
    fun getFavoriteById(id: String): Flow<Favorite?>
    suspend fun addFavorite(favorite: Favorite)
    suspend fun updateFavorite(favorite: Favorite)
    suspend fun deleteFavorite(id: String)
    suspend fun toggleFavorite(contentId: String, contentType: ContentType): Boolean
    suspend fun isFavorite(contentId: String, contentType: ContentType): Boolean
}
```

## 2. Data层 - Repository实现

### `FavoriteRepositoryImpl.kt`
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.FavoriteDao
import com.mathknowledge.app.data.mapper.toDomain
import com.mathknowledge.app.data.mapper.toEntity
import com.mathknowledge.app.domain.model.ContentType
import com.mathknowledge.app.domain.model.Favorite
import com.mathknowledge.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<Favorite>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoritesByType(contentType: ContentType): Flow<List<Favorite>> {
        return favoriteDao.getFavoritesByType(contentType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteById(id: String): Flow<Favorite?> {
        return favoriteDao.getFavoriteById(id).map { it?.toDomain() }
    }

    override suspend fun addFavorite(favorite: Favorite) {
        val entity = favorite.copy(
            id = UUID.randomUUID().toString(),
            addedAt = LocalDateTime.now()
        ).toEntity()
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun updateFavorite(favorite: Favorite) {
        favoriteDao.updateFavorite(favorite.toEntity())
    }

    override suspend fun deleteFavorite(id: String) {
        favoriteDao.deleteFavoriteById(id)
    }

    override suspend fun toggleFavorite(contentId: String, contentType: ContentType): Boolean {
        val existingFavorite = favoriteDao.getFavoriteByContentId(contentId, contentType.name)
        
        return if (existingFavorite != null) {
            favoriteDao.deleteFavoriteById(existingFavorite.id)
            false
        } else {
            val newFavorite = Favorite(
                id = UUID.randomUUID().toString(),
                contentId = contentId,
                contentType = contentType,
                title = "", // 实际使用时需要从内容中获取
                description = "", // 实际使用时需要从内容中获取
                addedAt = LocalDateTime.now()
            )
            favoriteDao.insertFavorite(newFavorite.toEntity())
            true
        }
    }

    override suspend fun isFavorite(contentId: String, contentType: ContentType): Boolean {
        return favoriteDao.getFavoriteByContentId(contentId, contentType.name) != null
    }
}
```

### `FavoriteDao.kt`
```kotlin
package com.mathknowledge.app.data.local

import androidx.room.*
import com.mathknowledge.app.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE content_type = :contentType ORDER BY added_at DESC")
    fun getFavoritesByType(contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE id = :id")
    fun getFavoriteById(id: String): Flow<FavoriteEntity?>

    @Query("SELECT * FROM favorites WHERE content_id = :contentId AND content_type = :contentType")
    suspend fun getFavoriteByContentId(contentId: String, contentType: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Update
    suspend fun updateFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()
}
```

### `FavoriteEntity.kt`
```kotlin
package com.mathknowledge.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "content_id")
    val contentId: String,
    
    @ColumnInfo(name = "content_type")
    val contentType: String,
    
    val title: String,
    val description: String,
    
    @ColumnInfo(name = "added_at")
    val addedAt: String,
    
    val notes: String = "",
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = true
)
```

### `FavoriteMapper.kt`
```kotlin
package com.mathknowledge.app.data.mapper

import com.mathknowledge.app.data.local.entity.FavoriteEntity
import com.mathknowledge.app.domain.model.ContentType
import com.mathknowledge.app.domain.model.Favorite
import java.time.LocalDateTime

fun FavoriteEntity.toDomain(): Favorite {
    return Favorite(
        id = id,
        contentId = contentId,
        contentType = when (contentType) {
            "QUESTION" -> ContentType.QUESTION
            "KNOWLEDGE_POINT" -> ContentType.KNOWLEDGE_POINT
            else -> ContentType.QUESTION
        },
        title = title,
        description = description,
        addedAt = LocalDateTime.parse(addedAt),
        notes = notes,
        isFavorite = isFavorite
    )
}

fun Favorite.toEntity(): FavoriteEntity {
    return FavoriteEntity(
        id = id,
        contentId = contentId,
        contentType = contentType.name,
        title = title,
        description = description,
        addedAt = addedAt.toString(),
        notes = notes,
        isFavorite = isFavorite
    )
}
```

## 3. Presentation层 - ViewModel

### `FavoriteViewModel.kt`
```kotlin
package com.mathknowledge.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathknowledge.app.domain.model.ContentType
import com.mathknowledge.app.domain.model.Favorite
import com.mathknowledge.app.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class FavoriteUiState(
    val favorites: List<Favorite> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedContentType: ContentType? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    TITLE_ASC,
    TITLE_DESC
}

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    private val _selectedFavorite = MutableStateFlow<Favorite?>(null)
    val selectedFavorite: StateFlow<Favorite?> = _selectedFavorite.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val favoritesFlow = when (_uiState.value.selectedContentType) {
                    null -> favoriteRepository.getFavorites()
                    else -> favoriteRepository.getFavoritesByType(_uiState.value.selectedContentType!!)
                }
                
                favoritesFlow.collect { favorites ->
                    val sortedFavorites = when (_uiState.value.sortOrder) {
                        SortOrder.NEWEST_FIRST -> favorites.sortedByDescending { it.addedAt }
                        SortOrder.OLDEST_FIRST -> favorites.sortedBy { it.addedAt }
                        SortOrder.TITLE_ASC -> favorites.sortedBy { it.title }
                        SortOrder.TITLE_DESC -> favorites.sortedByDescending { it.title }
                    }
                    
                    _uiState.update { 
                        it.copy(
                            favorites = sortedFavorites,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载收藏失败"
                    )
                }
            }
        }
    }

    fun filterByContentType(contentType: ContentType?) {
        _uiState.update { it.copy(selectedContentType = contentType) }
        loadFavorites()
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
        loadFavorites()
    }

    fun selectFavorite(favorite: Favorite) {
        _selectedFavorite.value = favorite
    }

    fun clearSelectedFavorite() {
        _selectedFavorite.value = null
    }

    fun updateFavoriteNotes(favoriteId: String, notes: String) {
        viewModelScope.launch {
            try {
                val currentFavorite = _uiState.value.favorites.find { it.id == favoriteId }
                currentFavorite?.let { favorite ->
                    val updatedFavorite = favorite.copy(notes = notes)
                    favoriteRepository.updateFavorite(updatedFavorite)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "更新备注失败") }
            }
        }
    }

    fun deleteFavorite(favoriteId: String) {
        viewModelScope.launch {
            try {
                favoriteRepository.deleteFavorite(favoriteId)
                clearSelectedFavorite()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "删除收藏失败") }
            }
        }
    }

    fun toggleFavorite(contentId: String, contentType: ContentType) {
        viewModelScope.launch {
            try {
                favoriteRepository.toggleFavorite(contentId, contentType)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "切换收藏状态失败") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

## 4. Presentation层 - UI组件

### `FavoriteScreen.kt`
```kotlin
package com.mathknowledge.app.presentation.favorites

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mathknowledge.app.domain.model.ContentType
import com.mathknowledge.app.domain.model.Favorite
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
                actions = {
                    // 筛选按钮
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                    
                    // 排序按钮
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "排序")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 当前筛选状态显示
            if (uiState.selectedContentType != null) {
                FilterChip(
                    selected = true,
                    onClick = { viewModel.filterByContentType(null) },
                    label = { 
                        Text(
                            when (uiState.selectedContentType) {
                                ContentType.QUESTION -> "题目"
                                ContentType.KNOWLEDGE_POINT -> "知识点"
                                null -> "全部"
                            }
                        )
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 筛选菜单
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("全部") },
                    onClick = {
                        viewModel.filterByContentType(null)
                        showFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("题目") },
                    onClick = {
                        viewModel.filterByContentType(ContentType.QUESTION)
                        showFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("知识点") },
                    onClick = {
                        viewModel.filterByContentType(ContentType.KNOWLEDGE_POINT)
                        showFilterMenu = false
                    }
                )
            }

            // 排序菜单
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("最新添加") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.NEWEST_FIRST)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("最早添加") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.OLDEST_FIRST)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("标题升序") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.TITLE_ASC)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("标题降序") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.TITLE_DESC)
                        showSortMenu = false
                    }
                )
            }

            // 加载状态
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                // 错误状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "未知错误",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.clearError() }) {
                            Text("重试")
                        }
                    }
                }
            } else if (uiState.favorites.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无收藏",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // 收藏列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.favorites,
                        key = { it.id }
                    ) { favorite ->
                        FavoriteItem(
                            favorite = favorite,
                            onClick = { 
                                viewModel.selectFavorite(favorite)
                                onNavigateToDetail(favorite.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItem(
    favorite: Favorite,
    onClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                    text = favorite.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // 内容类型标签
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (favorite.contentType) {
                        ContentType.QUESTION -> MaterialTheme.colorScheme.primaryContainer
                        ContentType.KNOWLEDGE_POINT -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = when (favorite.contentType) {
                            ContentType.QUESTION -> "题目"
                            ContentType.KNOWLEDGE_POINT -> "知识点"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = favorite.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (favorite.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "备注: ${favorite.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "添加于: ${favorite.addedAt.format(dateFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
```

### `FavoriteDetailScreen.kt`
```kotlin
package com.mathknowledge.app.presentation.favorites

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mathknowledge.app.domain.model.Favorite
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteDetailScreen(
    favoriteId: String,
    onNavigateBack: () -> Unit,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val selectedFavorite by viewModel.selectedFavorite.collectAsStateWithLifecycle()
    var notes by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    // 加载收藏详情
    LaunchedEffect(favoriteId) {
        val favorite = viewModel.uiState.value.favorites.find { it.id == favoriteId }
        favorite?.let {
            viewModel.selectFavorite(it)
            notes = it.notes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { paddingValues ->
        selectedFavorite?.let { favorite ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = favorite.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 内容类型
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (favorite.contentType) {
                            com.mathknowledge.app.domain.model.ContentType.QUESTION -> 
                                Icons.Default.Quiz
                            com.mathknowledge.app.domain.model.ContentType.KNOWLEDGE_POINT -> 
                                Icons.Default.Lightbulb
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (favorite.contentType) {
                            com.mathknowledge.app.domain.model.ContentType.QUESTION -> "题目"
                            com.mathknowledge.app.domain.model.ContentType.KNOWLEDGE_POINT -> "知识点"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 描述
                Text(
                    text = "内容描述",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8