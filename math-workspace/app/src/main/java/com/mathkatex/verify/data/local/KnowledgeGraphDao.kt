package com.mathkatex.verify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KnowledgeGraphDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(items: List<KnowledgeEdgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(items: List<KnowledgeAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaths(items: List<KnowledgePathEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPathItems(items: List<KnowledgePathItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(items: List<KnowledgeModelEntity>)

    @Query("DELETE FROM knowledge_edges")
    suspend fun clearEdges()

    @Query("DELETE FROM knowledge_aliases")
    suspend fun clearAliases()

    @Query("DELETE FROM knowledge_paths")
    suspend fun clearPaths()

    @Query("DELETE FROM knowledge_path_items")
    suspend fun clearPathItems()

    @Query("DELETE FROM knowledge_models")
    suspend fun clearModels()

    @Query("SELECT * FROM knowledge_edges WHERE fromId = :id")
    suspend fun edgesFrom(id: String): List<KnowledgeEdgeEntity>

    @Query("SELECT * FROM knowledge_edges WHERE toId = :id")
    suspend fun edgesTo(id: String): List<KnowledgeEdgeEntity>

    @Query("SELECT * FROM knowledge_paths ORDER BY grade, id")
    suspend fun allPaths(): List<KnowledgePathEntity>

    @Query("SELECT * FROM knowledge_path_items WHERE pathId = :pathId ORDER BY orderIndex")
    suspend fun pathItems(pathId: String): List<KnowledgePathItemEntity>

    @Query("SELECT * FROM knowledge_aliases WHERE alias = :alias LIMIT 1")
    suspend fun findByAlias(alias: String): KnowledgeAliasEntity?

    @Query("SELECT alias FROM knowledge_aliases WHERE knowledgeId = :id")
    suspend fun aliasesOf(id: String): List<String>

    @Query("SELECT * FROM knowledge_models WHERE knowledgeId = :id")
    suspend fun modelsOf(id: String): List<KnowledgeModelEntity>

    @Query("SELECT COUNT(*) FROM knowledge_edges")
    suspend fun edgeCount(): Int

    @Query("SELECT COUNT(*) FROM knowledge_aliases")
    suspend fun aliasCount(): Int
}
