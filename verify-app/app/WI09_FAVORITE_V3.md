我来修复收藏夹模块的toggleFavorite逻辑错误。以下是修复后的完整代码：

## 1. 首先在FavoriteRepository中添加getFavoriteByContentId方法

```kotlin
// FavoriteRepository.kt
interface FavoriteRepository {
    // 其他现有方法...
    
    /**
     * 根据contentId查询收藏记录
     * @param contentId 内容ID
     * @return 收藏记录，如果不存在则返回null
     */
    suspend fun getFavoriteByContentId(contentId: String): Favorite?
    
    // 其他现有方法...
}

// FavoriteRepositoryImpl.kt
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {
    
    // 其他现有实现...
    
    override suspend fun getFavoriteByContentId(contentId: String): Favorite? {
        return favoriteDao.getFavoriteByContentId(contentId)
    }
    
    // 其他现有实现...
}
```

## 2. 在FavoriteDao中添加对应的查询方法

```kotlin
// FavoriteDao.kt
@Dao
interface FavoriteDao {
    // 其他现有方法...
    
    /**
     * 根据contentId查询收藏记录
     * @param contentId 内容ID
     * @return 收藏记录，如果不存在则返回null
     */
    @Query("SELECT * FROM favorites WHERE contentId = :contentId LIMIT 1")
    suspend fun getFavoriteByContentId(contentId: String): Favorite?
    
    // 其他现有方法...
}
```

## 3. 修复后的FavoriteViewModel完整代码

```kotlin
// FavoriteViewModel.kt
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    
    // 其他现有代码...
    
    /**
     * 切换收藏状态
     * @param contentId 内容ID
     * @param contentType 内容类型
     * @param contentTitle 内容标题（可选）
     * @param contentImage 内容图片URL（可选）
     */
    fun toggleFavorite(
        contentId: String,
        contentType: String,
        contentTitle: String? = null,
        contentImage: String? = null
    ) {
        viewModelScope.launch {
            try {
                // 使用高效查询直接通过contentId查找
                val existingFavorite = favoriteRepository.getFavoriteByContentId(contentId)
                
                if (existingFavorite != null) {
                    // 已收藏，执行删除操作
                    favoriteRepository.removeFavorite(existingFavorite)
                    // 可以在这里发送收藏状态变更的通知
                    _favoriteStatusChanged.value = FavoriteStatusChanged(
                        contentId = contentId,
                        isFavorite = false
                    )
                } else {
                    // 未收藏，执行添加操作
                    val newFavorite = Favorite(
                        contentId = contentId,
                        contentType = contentType,
                        contentTitle = contentTitle,
                        contentImage = contentImage,
                        timestamp = System.currentTimeMillis()
                    )
                    favoriteRepository.addFavorite(newFavorite)
                    // 可以在这里发送收藏状态变更的通知
                    _favoriteStatusChanged.value = FavoriteStatusChanged(
                        contentId = contentId,
                        isFavorite = true
                    )
                }
            } catch (e: Exception) {
                // 处理异常情况
                _error.value = "收藏操作失败: ${e.message}"
            }
        }
    }
    
    // 其他现有代码...
    
    // 定义收藏状态变更的数据类
    data class FavoriteStatusChanged(
        val contentId: String,
        val isFavorite: Boolean
    )
    
    // LiveData或StateFlow用于观察收藏状态变更
    private val _favoriteStatusChanged = MutableLiveData<FavoriteStatusChanged>()
    val favoriteStatusChanged: LiveData<FavoriteStatusChanged> = _favoriteStatusChanged
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
}
```

## 4. 如果需要在Repository中实现getFavoriteByContentId方法

```kotlin
// FavoriteRepositoryImpl.kt 的完整实现示例
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {
    
    override suspend fun getFavoriteByContentId(contentId: String): Favorite? {
        return withContext(Dispatchers.IO) {
            favoriteDao.getFavoriteByContentId(contentId)
        }
    }
    
    override suspend fun addFavorite(favorite: Favorite) {
        withContext(Dispatchers.IO) {
            favoriteDao.insertFavorite(favorite)
        }
    }
    
    override suspend fun removeFavorite(favorite: Favorite) {
        withContext(Dispatchers.IO) {
            favoriteDao.deleteFavorite(favorite)
        }
    }
    
    override suspend fun getAllFavorites(): List<Favorite> {
        return withContext(Dispatchers.IO) {
            favoriteDao.getAllFavorites()
        }
    }
    
    // 其他方法实现...
}
```

## 修复说明：

1. **性能优化**：原来的代码使用`getAllFavorites().first().find { it.contentId == contentId }`会加载所有收藏记录到内存中，然后进行线性查找。对于大量收藏数据，这会导致性能问题。

2. **高效查询**：修复后使用`getFavoriteByContentId(contentId)`直接通过contentId查询数据库，利用数据库索引进行高效查找，时间复杂度从O(n)降低到O(1)或O(log n)。

3. **线程安全**：在Repository实现中使用`withContext(Dispatchers.IO)`确保数据库操作在IO线程执行，避免阻塞主线程。

4. **错误处理**：添加了异常处理，确保在收藏操作失败时能够给出适当的错误提示。

5. **状态通知**：添加了收藏状态变更的通知机制，便于UI层及时更新收藏状态。

这个修复方案既解决了性能问题，又保持了代码的清晰性和可维护性。