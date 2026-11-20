package com.mathkatex.verify.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 知识点数据仓库 - 线程安全的单例
 *
 * 初始化流程（首次启动）：
 * 1. 获取 Room 数据库实例
 * 2. 检查 DB 中是否有知识点数据 (count)
 * 3. 若 count == 0，从 assets/dim1-formula.md 导入
 * 4. 导入后验证 count > 0，才设置 _isReady = true
 * 5. 若导入失败，_isReady 保持 false，等待重试
 */
object KnowledgeRepository {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    /** 记录 init 是否已经尝试过，防止重复失败日志 */
    private var initAttempted = false

    private var dao: KnowledgeDao? = null
    private var graphDao: KnowledgeGraphDao? = null
    private var exampleDao: ExampleDao? = null
    private val initMutex = Mutex()

    /**
     * 初始化知识点数据库
     * - 线程安全（Mutex）
     * - 幂等：第二次调用直接返回（如果已 ready）
     * - 自动从 assets 导入
     */
    suspend fun init(context: Context) {
        initMutex.withLock {
            if (_isReady.value) return
            try {
                val db = AppDatabase.getInstance(context)
                dao = db.knowledgeDao()
                graphDao = db.knowledgeGraphDao()
                exampleDao = db.exampleDao()

                val count = withContext(Dispatchers.IO) { dao!!.count() }
                Log.d("KnowledgeRepo", "DB count before import: $count")

                if (count == 0) {
                    Log.d("KnowledgeRepo", "DB empty, importing from assets...")
                    val imported = withContext(Dispatchers.IO) {
                        KnowledgeImporter.importFromAssets(context, dao!!)
                    }
                    Log.d("KnowledgeRepo", "Import returned: imported = $imported")

                    // 验证导入结果
                    val afterCount = withContext(Dispatchers.IO) { dao!!.count() }
                    Log.d("KnowledgeRepo", "DB count after import: $afterCount")

                    if (afterCount == 0) {
                        Log.e("KnowledgeRepo", "IMPORT FAILED! imported=$imported, dbCount=0")
                        // 不要设置 _isReady = true，让页面显示加载中
                        initAttempted = true
                        return
                    }

                    // 验证 blocks 查询能否拿到数据
                    val blocks = withContext(Dispatchers.IO) { dao!!.getAllBlocks() }
                    Log.d("KnowledgeRepo", "Blocks from DB: $blocks")
                    if (blocks.isEmpty()) {
                        Log.e("KnowledgeRepo", "Blocks query returned empty after import!")
                        initAttempted = true
                        return
                    }
                }

                // 图谱关系导入（幂等）：关系/别名/路径/题型/例题回填
                withContext(Dispatchers.IO) {
                    KnowledgeGraphImporter.importFromAssets(
                        context,
                        dao!!,
                        db.knowledgeGraphDao(),
                        db.exampleDao(),
                        db.practiceDao(),
                    )
                }

                _isReady.value = true
                Log.d("KnowledgeRepo", "Initialization complete, isReady=true")
            } catch (e: Exception) {
                Log.e("KnowledgeRepo", "Init failed", e)
                _isReady.value = false
                initAttempted = true
                // 不再 throw，让页面显示 loading 或错误
            }
        }
    }

    /**
     * 强制重新导入（用于调试或数据损坏恢复）
     */
    suspend fun forceReimport(context: Context) {
        initMutex.withLock {
            try {
                _isReady.value = false
                val db = AppDatabase.getInstance(context)
                dao = db.knowledgeDao()
                graphDao = db.knowledgeGraphDao()
                exampleDao = db.exampleDao()
                val imported = withContext(Dispatchers.IO) {
                    KnowledgeImporter.importFromAssets(context, dao!!)
                }
                val count = withContext(Dispatchers.IO) { dao!!.count() }
                Log.d("KnowledgeRepo", "Force reimport: imported=$imported, dbCount=$count")
                if (count > 0) {
                    _isReady.value = true
                } else {
                    Log.w("KnowledgeRepo", "Force reimport had 0 results")
                }
            } catch (e: Exception) {
                Log.e("KnowledgeRepo", "Force reimport failed", e)
            }
        }
    }

    private fun requireDao(): KnowledgeDao {
        return dao ?: throw IllegalStateException("KnowledgeRepository not initialized")
    }

    // ============ 查询方法 ============

    suspend fun getAll(): List<KnowledgeEntity> = try {
        withContext(Dispatchers.IO) { requireDao().getAll() }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "getAll failed", e)
        emptyList()
    }

    suspend fun getBlocks(): List<String> = try {
        withContext(Dispatchers.IO) { requireDao().getAllBlocks() }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "getBlocks failed", e)
        emptyList()
    }

    suspend fun getByBlock(block: String): List<KnowledgeEntity> = try {
        withContext(Dispatchers.IO) { requireDao().getByBlock(block) }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "getByBlock failed for '$block'", e)
        emptyList()
    }

    suspend fun getById(id: String): KnowledgeEntity? = try {
        withContext(Dispatchers.IO) { requireDao().getById(id) }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "getById failed", e)
        null
    }

    suspend fun getCategoryLv1(block: String): List<String> = try {
        withContext(Dispatchers.IO) { requireDao().getCategoryLv1(block) }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "getCategoryLv1 failed for '$block'", e)
        emptyList()
    }

    suspend fun getCategoryLv2(block: String, lv1: String): List<String> = try {
        withContext(Dispatchers.IO) { requireDao().getCategoryLv2(block, lv1) }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "getCategoryLv2 failed", e)
        emptyList()
    }

    suspend fun getByLv2(block: String, lv1: String, lv2: String): List<KnowledgeEntity> = try {
        withContext(Dispatchers.IO) { requireDao().getByLv2(block, lv1, lv2) }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "getByLv2 failed", e)
        emptyList()
    }

    suspend fun search(keyword: String): List<KnowledgeEntity> = try {
        withContext(Dispatchers.IO) { requireDao().search(keyword) }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "search failed", e)
        emptyList()
    }

    suspend fun count(): Int = try {
        withContext(Dispatchers.IO) { requireDao().count() }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "count failed", e)
        0
    }
    // ============ 图谱 / 串联查询 ============

    suspend fun examplesFor(knowledgeId: String): List<ExampleEntity> = try {
        withContext(Dispatchers.IO) { exampleDao?.byKnowledgeId(knowledgeId) ?: emptyList() }
    } catch (e: Exception) {
        Log.e("KnowledgeRepo", "examplesFor failed", e)
        emptyList()
    }

    suspend fun aliasesOf(knowledgeId: String): List<String> = try {
        withContext(Dispatchers.IO) { graphDao?.aliasesOf(knowledgeId) ?: emptyList() }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun modelsOf(knowledgeId: String): List<String> = try {
        withContext(Dispatchers.IO) { graphDao?.modelsOf(knowledgeId)?.map { it.modelName } ?: emptyList() }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun prerequisitesOf(knowledgeId: String): List<KnowledgeEntity> = try {
        withContext(Dispatchers.IO) {
            val ids = graphDao?.edgesTo(knowledgeId)
                ?.filter { it.type == "PREREQUISITE" }?.map { it.fromId } ?: emptyList()
            if (ids.isEmpty()) emptyList() else dao?.getByIds(ids) ?: emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun relatedOf(knowledgeId: String): List<KnowledgeEntity> = try {
        withContext(Dispatchers.IO) {
            val ids = graphDao?.edgesFrom(knowledgeId)
                ?.filter { it.type == "RELATED" }?.map { it.toId } ?: emptyList()
            if (ids.isEmpty()) emptyList() else dao?.getByIds(ids) ?: emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** 把模型/用户输入的知识点文本匹配到知识库 ID（先别名，再标题包含）。 */
    suspend fun resolveKnowledgeId(text: String): String? = try {
        withContext(Dispatchers.IO) {
            val raw = text.trim().removePrefix("$").removeSuffix("$")
            if (raw.isEmpty()) return@withContext null
            graphDao?.findByAlias(raw)?.knowledgeId?.let { return@withContext it }
            // 去掉年级括号再试一次
            val cleaned = raw.replace(Regex("（[^）]*）|\\([^)]*\\)"), "").trim()
            graphDao?.findByAlias(cleaned)?.knowledgeId?.let { return@withContext it }
            dao?.getAll()?.firstOrNull {
                it.title.replace(Regex("（[^）]*）"), "").trim() == cleaned
            }?.id
        }
    } catch (e: Exception) {
        null
    }

    suspend fun getPaths(): List<KnowledgePathEntity> = try {
        withContext(Dispatchers.IO) { graphDao?.allPaths() ?: emptyList() }
    } catch (e: Exception) {
        emptyList()
    }

}
