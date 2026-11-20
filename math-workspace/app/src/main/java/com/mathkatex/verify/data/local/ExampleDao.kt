package com.mathkatex.verify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExampleDao {
    @Query("SELECT * FROM examples ORDER BY difficulty, id")
    suspend fun getAll(): List<ExampleEntity>

    @Query("SELECT * FROM examples WHERE knowledgeTitle = :title ORDER BY difficulty, id")
    suspend fun byKnowledge(title: String): List<ExampleEntity>

    @Query("SELECT COUNT(*) FROM examples")
    suspend fun count(): Int

    @Query("SELECT * FROM examples WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ExampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExampleEntity>)
}
