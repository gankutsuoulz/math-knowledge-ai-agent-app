package com.mathkatex.verify.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次作答记录；错题本按 exampleId 去重后取 correct=0 的题。 */
@Entity(tableName = "practice_records", indices = [Index("exampleId")])
data class PracticeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exampleId: String,
    val correct: Boolean,
    val userAnswer: String,
    val createdAt: Long,
    val knowledgeId: String = "",
)
