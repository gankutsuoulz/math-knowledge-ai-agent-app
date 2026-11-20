package com.mathkatex.verify.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * 学习闭环 v1：答题 → 掌握度 → 复习排期。
 * 掌握度是粗排（0-1），先跑通闭环，后续再用 BKT/IRT 校准。
 */
object LearningRepository {
    private var dao: MasteryDao? = null
    private val DAY = 24L * 60 * 60 * 1000
    private val INTERVALS = longArrayOf(1, 3, 7, 14, 30)

    suspend fun init(context: Context) {
        if (dao == null) dao = AppDatabase.getInstance(context).masteryDao()
    }

    suspend fun onAnswer(knowledgeId: String, correct: Boolean) = withContext(Dispatchers.IO) {
        if (knowledgeId.isBlank()) return@withContext
        val d = dao ?: return@withContext
        val now = System.currentTimeMillis()
        val old = d.get(knowledgeId)
        val score = if (old == null) {
            if (correct) 0.6f else 0.2f
        } else {
            (old.score * 0.7f + if (correct) 0.3f else -0.2f).coerceIn(0f, 1f)
        }
        val stage = if (correct) min((old?.stage ?: 0) + 1, INTERVALS.size - 1) else 0
        // 答错当天就进复习队列；答对按 1/3/7/14/30 天排期
        val due = if (correct) now + INTERVALS[stage] * DAY else now
        d.upsert(
            MasteryEntity(
                knowledgeId = knowledgeId,
                score = score,
                attempts = (old?.attempts ?: 0) + 1,
                correct = (old?.correct ?: 0) + if (correct) 1 else 0,
                stage = stage,
                updatedAt = now,
                nextReviewAt = due,
            )
        )
        d.upsertReview(ReviewItemEntity(knowledgeId, due, stage, now))
    }

    suspend fun mastery(knowledgeId: String): MasteryEntity? = withContext(Dispatchers.IO) {
        dao?.get(knowledgeId)
    }

    suspend fun dueCount(): Int = withContext(Dispatchers.IO) {
        dao?.dueCount(System.currentTimeMillis()) ?: 0
    }

    suspend fun dueReviews(limit: Int = 20): List<Pair<KnowledgeEntity, ReviewItemEntity>> =
        withContext(Dispatchers.IO) {
            val d = dao ?: return@withContext emptyList()
            val items = d.due(System.currentTimeMillis(), limit)
            items.mapNotNull { item ->
                KnowledgeRepository.getById(item.knowledgeId)?.let { it to item }
            }
        }

    suspend fun stats(): Pair<Int, Float> = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext 0 to 0f
        (d.masteredCount()) to (d.averageScore() ?: 0f)
    }

    suspend fun isReady(): Boolean = dao != null
}
