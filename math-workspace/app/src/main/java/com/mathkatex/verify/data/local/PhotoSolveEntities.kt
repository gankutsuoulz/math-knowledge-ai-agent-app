package com.mathkatex.verify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 拍照解题记录：收藏后进入「我的收藏」，保留题干、结果与图片路径。 */
@Entity(tableName = "photo_solves")
data class PhotoSolveRecordEntity(
    @PrimaryKey val id: String,
    val question: String,
    val rawResponse: String,
    val structuredJson: String,
    val knowledgeIds: String,
    val imagePath: String,
    val favorite: Boolean,
    val createdAt: Long,
)
