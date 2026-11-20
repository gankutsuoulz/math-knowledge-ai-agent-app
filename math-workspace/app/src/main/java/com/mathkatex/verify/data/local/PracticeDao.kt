package com.mathkatex.verify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PracticeDao {
    @Insert
    suspend fun insertRecord(record: PracticeRecordEntity)

    @Query("SELECT DISTINCT exampleId FROM practice_records WHERE correct = 0 ORDER BY createdAt DESC")
    suspend fun wrongExampleIds(): List<String>

    @Query("UPDATE practice_records SET knowledgeId = (SELECT knowledgeId FROM examples WHERE examples.id = practice_records.exampleId) WHERE knowledgeId = ''")
    suspend fun backfillKnowledgeIds()

    @Query("SELECT COUNT(*) FROM practice_records")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM practice_records WHERE correct = 1")
    suspend fun correctCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE exampleId = :exampleId")
    suspend fun removeFavorite(exampleId: String)

    @Query("SELECT exampleId FROM favorites ORDER BY createdAt DESC")
    suspend fun favoriteIds(): List<String>
}
