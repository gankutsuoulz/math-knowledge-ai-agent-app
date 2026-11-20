package com.mathkatex.verify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 知识点掌握度（v1：答题结果滑动平均 + 复习阶段）。 */
@Entity(tableName = "mastery")
data class MasteryEntity(
    @PrimaryKey val knowledgeId: String,
    val score: Float = 0f,
    val attempts: Int = 0,
    val correct: Int = 0,
    val stage: Int = 0,
    val updatedAt: Long = 0L,
    val nextReviewAt: Long = 0L,
)

/** 复习队列：按知识点排期，stage 对应 1/3/7/14/30 天。 */
@Entity(tableName = "review_items")
data class ReviewItemEntity(
    @PrimaryKey val knowledgeId: String,
    val dueAt: Long,
    val stage: Int = 0,
    val updatedAt: Long = 0L,
)
