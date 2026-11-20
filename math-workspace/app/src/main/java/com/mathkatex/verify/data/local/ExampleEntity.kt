package com.mathkatex.verify.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 例题：按知识点挂载，难度 1=基础 2=中等 3=困难。
 * type=choice 时 options 是 JSON 数组字符串；type=fill 时 options 为 "[]"。
 */
@Entity(tableName = "examples", indices = [Index("knowledgeTitle")])
data class ExampleEntity(
    @PrimaryKey val id: String,
    val knowledgeTitle: String,
    val grade: String,
    val difficulty: Int,
    val question: String,
    val answer: String,
    val solution: String,
    val source: String = "local",
    val type: String = "choice",
    val options: String = "[]",
)
