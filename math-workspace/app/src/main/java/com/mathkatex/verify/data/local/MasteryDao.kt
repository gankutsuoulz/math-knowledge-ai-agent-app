package com.mathkatex.verify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MasteryDao {
    @Query("SELECT * FROM mastery WHERE knowledgeId = :id")
    suspend fun get(id: String): MasteryEntity?

    @Query("SELECT * FROM mastery ORDER BY updatedAt DESC")
    suspend fun all(): List<MasteryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MasteryEntity)

    @Query("SELECT * FROM review_items WHERE dueAt <= :now ORDER BY dueAt LIMIT :limit")
    suspend fun due(now: Long, limit: Int = 20): List<ReviewItemEntity>

    @Query("SELECT COUNT(*) FROM review_items WHERE dueAt <= :now")
    suspend fun dueCount(now: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(item: ReviewItemEntity)

    @Query("SELECT * FROM review_items WHERE knowledgeId = :id")
    suspend fun reviewOf(id: String): ReviewItemEntity?

    @Query("SELECT COUNT(*) FROM mastery WHERE score >= 0.75")
    suspend fun masteredCount(): Int

    @Query("SELECT AVG(score) FROM mastery")
    suspend fun averageScore(): Float?
}
