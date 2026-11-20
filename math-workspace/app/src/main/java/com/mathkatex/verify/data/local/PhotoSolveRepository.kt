package com.mathkatex.verify.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 拍照解题记录与收藏。 */
object PhotoSolveRepository {
    private var dao: PhotoSolveDao? = null

    suspend fun init(context: Context) {
        if (dao == null) dao = AppDatabase.getInstance(context).photoSolveDao()
    }

    suspend fun save(record: PhotoSolveRecordEntity) = withContext(Dispatchers.IO) {
        dao?.upsert(record)
    }

    suspend fun setFavorite(id: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        dao?.setFavorite(id, favorite)
    }

    suspend fun favorites(): List<PhotoSolveRecordEntity> = withContext(Dispatchers.IO) {
        dao?.favorites() ?: emptyList()
    }

    suspend fun favoriteCount(): Int = withContext(Dispatchers.IO) {
        dao?.favoriteCount() ?: 0
    }

    suspend fun get(id: String): PhotoSolveRecordEntity? = withContext(Dispatchers.IO) {
        dao?.get(id)
    }
}
