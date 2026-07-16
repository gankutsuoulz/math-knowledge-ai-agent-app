package com.mathkatex.verify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_points ORDER BY id")
    suspend fun getAll(): List<KnowledgeEntity>

    @Query("SELECT * FROM knowledge_points WHERE block = :block ORDER BY id")
    suspend fun getByBlock(block: String): List<KnowledgeEntity>

    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    suspend fun getById(id: String): KnowledgeEntity?

    @Query("SELECT DISTINCT block FROM knowledge_points ORDER BY block")
    suspend fun getAllBlocks(): List<String>

    @Query("SELECT DISTINCT categoryLv1 FROM knowledge_points WHERE block = :block ORDER BY categoryLv1")
    suspend fun getCategoryLv1(block: String): List<String>

    @Query("SELECT DISTINCT categoryLv2 FROM knowledge_points WHERE block = :block AND categoryLv1 = :lv1 ORDER BY categoryLv2")
    suspend fun getCategoryLv2(block: String, lv1: String): List<String>

    @Query("SELECT * FROM knowledge_points WHERE block = :block AND categoryLv1 = :lv1 AND categoryLv2 = :lv2 ORDER BY id")
    suspend fun getByLv2(block: String, lv1: String, lv2: String): List<KnowledgeEntity>

    @Query("SELECT * FROM knowledge_points WHERE title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%'")
    suspend fun search(keyword: String): List<KnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KnowledgeEntity>)

    @Query("DELETE FROM knowledge_points")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM knowledge_points")
    suspend fun count(): Int
}
