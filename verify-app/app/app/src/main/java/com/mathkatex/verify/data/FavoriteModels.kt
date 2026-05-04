package com.mathkatex.verify.data

import java.util.UUID

/**
 * 收藏项目类型
 */
enum class FavoriteType {
    KNOWLEDGE,  // 收藏的知识点
    MISTAKE      // 收藏的错题
}

/**
 * 收藏项目数据模型
 */
data class FavoriteItem(
    val id: String = UUID.randomUUID().toString(),
    val type: FavoriteType,
    val title: String,
    val topic: String,          // 所属话题
    val grade: String,          // 年级
    val dateAdded: Long = System.currentTimeMillis(),
    val content: String = "",   // 详细内容（用于详情页）
    val summary: String = ""    // 摘要
)

/**
 * 模拟的收藏数据仓库
 * 实际项目中应使用 Room 或 SharedPreferences 持久化
 */
object FavoritesRepository {
    
    private val knowledgeFavorites = mutableListOf(
        FavoriteItem(
            id = "k1",
            type = FavoriteType.KNOWLEDGE,
            title = "三角函数诱导公式",
            topic = "三角函数",
            grade = "高一",
            content = """
                # 三角函数诱导公式

                ## 核心公式
                - sin(-α) = -sin α
                - cos(-α) = cos α
                - tan(-α) = -tan α

                ## 和差角公式
                - sin(α + β) = sin α cos β + cos α sin β
                - cos(α + β) = cos α cos β - sin α sin β

                ## 倍角公式
                - sin 2α = 2 sin α cos α
                - cos 2α = cos²α - sin²α = 2 cos²α - 1
            """.trimIndent(),
            summary = "三角函数诱导公式、和差角公式、倍角公式"
        ),
        FavoriteItem(
            id = "k2",
            type = FavoriteType.KNOWLEDGE,
            title = "等差数列求和公式",
            topic = "数列",
            grade = "高一",
            content = """
                # 等差数列

                ## 定义
                如果一个数列从第2项起，每一项与它的前一项的差等于同一个常数，这个数列就叫做等差数列。

                ## 通项公式
                a_n = a_1 + (n-1)d

                ## 前n项和公式
                S_n = n(a_1 + a_n)/2 = n[2a_1 + (n-1)d]/2
            """.trimIndent(),
            summary = "等差数列的通项公式与前n项和公式"
        ),
        FavoriteItem(
            id = "k3",
            type = FavoriteType.KNOWLEDGE,
            title = "基本求导法则",
            topic = "导数",
            grade = "高三",
            content = """
                # 导数基本公式

                ## 基础导数
                - (x^n)' = n x^(n-1)
                - (sin x)' = cos x
                - (cos x)' = -sin x
                - (e^x)' = e^x
                - (ln x)' = 1/x

                ## 求导法则
                - (u ± v)' = u' ± v'
                - (uv)' = u'v + uv'
                - (u/v)' = (u'v - uv') / v²
            """.trimIndent(),
            summary = "基本求导公式与求导法则"
        )
    )
    
    private val mistakeFavorites = mutableListOf(
        FavoriteItem(
            id = "m1",
            type = FavoriteType.MISTAKE,
            title = "二次函数最值问题",
            topic = "函数",
            grade = "高一",
            content = """
                # 二次函数最值问题

                ## 题目
                求函数 y = x² - 4x + 3 在区间 [0, 3] 上的最大值和最小值。

                ## 错误解法
                直接代入端点：
                x=0 时，y=3
                x=3 时，y=6
                所以最大值是6，最小值是3。

                ## 正确解法
                先求顶点：
                y = (x-2)² - 1
                顶点坐标 (2, -1)

                在区间 [0, 3] 内：
                - 最小值在顶点处：y_min = -1
                - 最大值在端点 x=3 处：y_max = 6

                ## 易错点
                二次函数最值需要比较顶点和端点值，不能只比较端点！
            """.trimIndent(),
            summary = "二次函数在闭区间上的最值需考虑顶点"
        ),
        FavoriteItem(
            id = "m2",
            type = FavoriteType.MISTAKE,
            title = "向量数量积运算",
            topic = "平面向量",
            grade = "高一",
            content = """
                # 向量数量积运算

                ## 题目
                已知 |a| = 3, |b| = 4, a·b = 6，求 |a + b|。

                ## 错误解法
                |a + b| = |a| + |b| = 3 + 4 = 7  （错误）

                ## 正确解法
                |a + b|² = (a + b)² = a² + 2a·b + b²
                          = |a|² + 2(a·b) + |b|²
                          = 9 + 2×6 + 16 = 37

                所以 |a + b| = √37

                ## 易错点
                向量模长不满足三角不等式的等号！
                |a + b| ≠ |a| + |b|（除非同向）
                正确公式：|a + b|² = |a|² + |b|² + 2a·b
            """.trimIndent(),
            summary = "向量模长计算要用完全平方公式"
        )
    )
    
    fun getKnowledgeFavorites(): List<FavoriteItem> = knowledgeFavorites.toList()
    
    fun getMistakeFavorites(): List<FavoriteItem> = mistakeFavorites.toList()
    
    fun getAllFavorites(): List<FavoriteItem> = knowledgeFavorites + mistakeFavorites
    
    fun removeFavorite(id: String) {
        knowledgeFavorites.removeIf { it.id == id }
        mistakeFavorites.removeIf { it.id == id }
    }
    
    fun addKnowledgeFavorite(item: FavoriteItem) {
        if (!knowledgeFavorites.any { it.id == item.id }) {
            knowledgeFavorites.add(item.copy(type = FavoriteType.KNOWLEDGE))
        }
    }
    
    fun addMistakeFavorite(item: FavoriteItem) {
        if (!mistakeFavorites.any { it.id == item.id }) {
            mistakeFavorites.add(item.copy(type = FavoriteType.MISTAKE))
        }
    }
}