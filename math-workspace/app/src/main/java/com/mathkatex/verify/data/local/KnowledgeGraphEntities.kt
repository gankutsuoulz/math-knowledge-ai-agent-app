package com.mathkatex.verify.data.local

import androidx.room.Entity
import androidx.room.Index

/** 知识点关系：前置/相关/包含等。 */
@Entity(
    tableName = "knowledge_edges",
    primaryKeys = ["fromId", "toId", "type"],
    indices = [Index("toId")],
)
data class KnowledgeEdgeEntity(
    val fromId: String,
    val toId: String,
    val type: String,
    val confidence: Float = 0.5f,
    val note: String = "",
)

/** 别名：俗称、教材名、模型输出可能用的写法。 */
@Entity(
    tableName = "knowledge_aliases",
    primaryKeys = ["alias", "knowledgeId"],
    indices = [Index("knowledgeId")],
)
data class KnowledgeAliasEntity(
    val alias: String,
    val knowledgeId: String,
)

/** 专题学习路径（初一/初二计算专项等）。 */
@Entity(tableName = "knowledge_paths")
data class KnowledgePathEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val grade: String,
)

/** 路径中的知识点顺序。 */
@Entity(
    tableName = "knowledge_path_items",
    primaryKeys = ["pathId", "knowledgeId"],
    indices = [Index("knowledgeId")],
)
data class KnowledgePathItemEntity(
    val pathId: String,
    val knowledgeId: String,
    val orderIndex: Int,
)

/** 题型/方法模型与知识点的关联（dim4）。 */
@Entity(
    tableName = "knowledge_models",
    primaryKeys = ["modelName", "knowledgeId"],
    indices = [Index("knowledgeId")],
)
data class KnowledgeModelEntity(
    val modelName: String,
    val knowledgeId: String,
    val confidence: Float = 0.5f,
)
