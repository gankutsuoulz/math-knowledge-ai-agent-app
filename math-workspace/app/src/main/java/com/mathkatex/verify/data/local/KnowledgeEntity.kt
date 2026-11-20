package com.mathkatex.verify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 知识点数据模型
 * 对应 dim1-formula.md 中的一条知识点
 */
@Entity(tableName = "knowledge_points")
data class KnowledgeEntity(
    @PrimaryKey
    val id: String,              // 如：KB_A_G5_K001
    val block: String,           // 板块：数与代数/图形与几何/统计与概率/综合与实践
    val grade: String,           // 年级：五年级/六年级/初一/初二/初三/高一/高二/高三
    val categoryLv1: String,     // 一级分类
    val categoryLv2: String,     // 二级分类
    val categoryLv3: String,     // 三级分类（可选）
    val categoryLv4: String,     // 四级分类（可选）
    val title: String,           // 知识点标题
    val formula: String,         // LaTeX 公式
    val content: String,         // 详细说明
    val fullPath: String,        // 完整分类路径，如 "数与代数 > 数与运算基础 > 小数的认识与运算"
    val abilityLevel: String = "" // dim2 能力层（L1/L2/L3），导入图谱时回填
)
