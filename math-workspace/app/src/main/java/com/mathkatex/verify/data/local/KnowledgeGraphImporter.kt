package com.mathkatex.verify.data.local

import android.content.Context
import com.google.gson.Gson
import com.mathkatex.verify.util.LogFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** assets/knowledge_graph.json 的 DTO（只取 App 需要的字段）。 */
private data class GraphFile(
    val version: String? = null,
    val nodes: List<GraphNode>? = null,
    val edges: List<GraphEdge>? = null,
    val paths: List<GraphPath>? = null,
    val questionModels: List<GraphModel>? = null,
    val examples: List<GraphExample>? = null,
)

private data class GraphNode(
    val id: String? = null,
    val abilityLevel: String? = null,
    val aliases: List<String>? = null,
)

private data class GraphEdge(
    val from: String? = null,
    val to: String? = null,
    val type: String? = null,
    val confidence: Float? = null,
    val note: String? = null,
)

private data class GraphPath(
    val id: String? = null,
    val title: String? = null,
    val grade: String? = null,
    val nodeIds: List<String>? = null,
)

private data class GraphModel(
    val name: String? = null,
    val nodeIds: List<String>? = null,
    val confidence: Float? = null,
)

private data class GraphExample(
    val exampleId: String? = null,
    val knowledgeId: String? = null,
)

/**
 * 把知识图谱（关系/别名/路径/题型模型/例题回填）导入 Room。
 * 幂等：先清空图关系表再写入；知识点节点仍由 dim1 的 importer 负责。
 */
object KnowledgeGraphImporter {
    suspend fun importFromAssets(
        context: Context,
        knowledgeDao: KnowledgeDao,
        graphDao: KnowledgeGraphDao,
        exampleDao: ExampleDao,
        practiceDao: PracticeDao,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val raw = context.assets.open("knowledge_graph.json").bufferedReader().use { it.readText() }
            val graph = Gson().fromJson(raw, GraphFile::class.java) ?: return@withContext false
            val nodes = graph.nodes.orEmpty()
            if (nodes.isEmpty()) return@withContext false

            // 1) 能力层回填
            nodes.forEach { node ->
                val id = node.id ?: return@forEach
                val level = node.abilityLevel.orEmpty()
                if (level.isNotEmpty()) knowledgeDao.updateAbilityLevel(id, level)
            }

            // 2) 别名
            val aliases = nodes.flatMap { node ->
                (node.aliases.orEmpty()).mapNotNull { alias ->
                    node.id?.takeIf { it.isNotEmpty() }?.let { KnowledgeAliasEntity(alias, it) }
                }
            }
            graphDao.clearAliases()
            if (aliases.isNotEmpty()) graphDao.insertAliases(aliases.distinctBy { it.alias to it.knowledgeId })

            // 3) 关系边
            val edges = graph.edges.orEmpty().mapNotNull { e ->
                val from = e.from; val to = e.to; val type = e.type
                if (from.isNullOrEmpty() || to.isNullOrEmpty() || type.isNullOrEmpty()) null
                else KnowledgeEdgeEntity(from, to, type, e.confidence ?: 0.5f, e.note.orEmpty())
            }
            graphDao.clearEdges()
            if (edges.isNotEmpty()) graphDao.insertEdges(edges)

            // 4) 路径 + 路径项
            val paths = graph.paths.orEmpty().filter { !it.id.isNullOrEmpty() }
            graphDao.clearPathItems()
            graphDao.clearPaths()
            if (paths.isNotEmpty()) {
                graphDao.insertPaths(paths.map {
                    KnowledgePathEntity(it.id!!, it.title.orEmpty(), it.grade.orEmpty())
                })
                val items = mutableListOf<KnowledgePathItemEntity>()
                paths.forEach { p ->
                    p.nodeIds.orEmpty().forEachIndexed { index, nid ->
                        items.add(KnowledgePathItemEntity(p.id!!, nid, index))
                    }
                }
                if (items.isNotEmpty()) graphDao.insertPathItems(items.distinctBy { it.pathId to it.knowledgeId })
            }

            // 5) 题型模型
            val models = graph.questionModels.orEmpty().flatMap { m ->
                m.nodeIds.orEmpty().mapNotNull { nid ->
                    m.name?.takeIf { it.isNotEmpty() }?.let {
                        KnowledgeModelEntity(it, nid, m.confidence ?: 0.5f)
                    }
                }
            }
            graphDao.clearModels()
            if (models.isNotEmpty()) graphDao.insertModels(models.distinctBy { it.modelName to it.knowledgeId })

            // 6) 例题回填 knowledgeId
            graph.examples.orEmpty().forEach { e ->
                val eid = e.exampleId; val kid = e.knowledgeId
                if (!eid.isNullOrEmpty() && !kid.isNullOrEmpty()) {
                    exampleDao.updateKnowledgeId(eid, kid)
                }
            }
            practiceDao.backfillKnowledgeIds()

            LogFileManager.log(
                "KnowledgeGraph",
                "graph imported v${graph.version}: nodes=${nodes.size}, edges=${edges.size}, " +
                    "aliases=${aliases.size}, paths=${paths.size}, models=${models.size}, " +
                    "examples=${graph.examples.orEmpty().size}"
            )
            true
        } catch (e: Exception) {
            LogFileManager.logError("KnowledgeGraph", "import failed: ${e.message}", e)
            false
        }
    }
}
