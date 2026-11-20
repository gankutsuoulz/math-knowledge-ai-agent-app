package com.mathkatex.verify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoSolveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PhotoSolveRecordEntity)

    @Query("UPDATE photo_solves SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT * FROM photo_solves WHERE favorite = 1 ORDER BY createdAt DESC")
    suspend fun favorites(): List<PhotoSolveRecordEntity>

    @Query("SELECT COUNT(*) FROM photo_solves WHERE favorite = 1")
    suspend fun favoriteCount(): Int

    @Query("SELECT * FROM photo_solves WHERE id = :id")
    suspend fun get(id: String): PhotoSolveRecordEntity?
}
