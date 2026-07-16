package com.mathkatex.verify.data.local

import android.content.Context
import android.util.Log
import com.mathkatex.verify.util.LogFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 知识点数据导入工具
 * 将 dim1-formula.md 解析为 KnowledgeEntity 并导入 Room 数据库
 *
 * Debug 要点：
 * 1. 检查 assets/dim1-formula.md 是否存在且可读
 * 2. 检查 parseMarkdown 是否解析出知识条目
 * 3. 检查 dao.deleteAll() 和 dao.insertAll() 是否成功执行
 * 4. 检查 db count 是否从 0 → N（导入后）
 */
object KnowledgeImporter {

    private val blockMap = listOf("数与代数", "图形与几何", "统计与概率", "综合与实践")

    private val gradeKeywords = mapOf(
        "五年级" to "五年级", "六年级" to "六年级",
        "初一" to "初一", "初二" to "初二", "初三" to "初三",
        "高一" to "高一", "高二" to "高二", "高三" to "高三",
        "五~六" to "五~六年级", "五～六" to "五~六年级",
        "七~九" to "七~九年级", "八~九" to "八~九年级",
        "初一~初三" to "七~九年级", "初三~高一" to "初三~高一"
    )

    /**
     * 从 assets 中的 dim1-formula.md 导入知识点
     * 导入后验证 count，确保数据已写入
     */
    suspend fun importFromAssets(context: Context, dao: KnowledgeDao): Int {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 验证 assets 文件存在
                val assetFile = try {
                    context.assets.open("dim1-formula.md").use { /* 只验证存在性 */ }
                    true
                } catch (e: Exception) {
                    LogFileManager.logError("KnowledgeImporter", "assets/dim1-formula.md not found!", e)
                    false
                }
                if (!assetFile) {
                    LogFileManager.log("KnowledgeImporter", "FATAL: assets file missing!")
                    return@withContext 0
                }
                LogFileManager.log("KnowledgeImporter", "assets file exists, starting import...")

                // 2. 读取并解析
                val content = context.assets.open("dim1-formula.md").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                        reader.readText()
                    }
                }
                LogFileManager.log("KnowledgeImporter", "Read ${content.length} chars from assets file")

                val entities = parseMarkdown(content)
                LogFileManager.log("KnowledgeImporter", "Parsed ${entities.size} knowledge entities")

                if (entities.isEmpty()) {
                    LogFileManager.log("KnowledgeImporter", "WARNING: parseMarkdown returned 0 entities!")
                    // 调试：打印前20行内容看看格式
                    val firstLines = content.lines().take(20).joinToString("\n")
                    LogFileManager.log("KnowledgeImporter", "First 20 lines of content:\n$firstLines")
                    return@withContext 0
                }

                // 3. 统计数据分布
                val blockCounts = entities.groupBy { it.block }.mapValues { it.value.size }
                LogFileManager.log("KnowledgeImporter", "Block distribution: $blockCounts")

                // 4. 先删除再插入（事务内完成）
                dao.deleteAll()
                LogFileManager.log("KnowledgeImporter", "deleteAll() completed")

                dao.insertAll(entities)
                LogFileManager.log("KnowledgeImporter", "insertAll(${entities.size}) completed")

                // 5. 验证
                val actualCount = dao.count()
                LogFileManager.log("KnowledgeImporter", "Verification: DB count after import = $actualCount")
                if (actualCount != entities.size) {
                    LogFileManager.log("KnowledgeImporter", "WARNING: expected $actualCount != ${entities.size}")
                }

                actualCount
            } catch (e: Exception) {
                LogFileManager.logError("KnowledgeImporter", "Import failed: ${e.message}", e)
                // 额外诊断信息
                try {
                    val existingCount = dao.count()
                    LogFileManager.log("KnowledgeImporter", "After exception, DB count = $existingCount")
                } catch (_: Exception) {}
                0
            }
        }
    }

    /**
     * 解析 Markdown 内容为 KnowledgeEntity 列表
     *
     * 支持格式：
     * - # 板块一：数与代数  → 板块标题（中文数字/阿拉伯数字）
     * - ## / ### / #### → 各级分类标题
     * - **知识点：标题（年级）**  → 知识点条目
     * - $$ ... $$   → LaTeX 公式（在知识点行中）
     *
     * Debug 提示：
     * - line.contains("知识点：") && line.startsWith("**") 用于识别知识点行
     * - 知识点行必须同时满足这两个条件
     */
    fun parseMarkdown(content: String): List<KnowledgeEntity> {
        val entities = mutableListOf<KnowledgeEntity>()
        val idCounter = java.util.concurrent.atomic.AtomicInteger(0)

        var currentBlock = ""
        var currentGrade = ""
        var catLv1 = ""
        var catLv2 = ""
        var catLv3 = ""
        var catLv4 = ""

        val lines = content.split("\n")
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            when {
                line.startsWith("# 板块") -> {
                    val blockName = line.substringAfter("# 板块").trim()
                    val newBlock = blockIndexToName(blockName) ?: ""
                    if (newBlock.isEmpty()) {
                        LogFileManager.log("KnowledgeImporter",
                            "WARNING: blockIndexToName('$blockName') returned null")
                    }
                    currentBlock = newBlock
                    catLv1 = ""; catLv2 = ""; catLv3 = ""; catLv4 = ""
                }
                line.startsWith("## ") -> {
                    val name = line.removePrefix("## ").trim()
                    catLv1 = name; catLv2 = ""; catLv3 = ""; catLv4 = ""
                    val g = extractGrade(name)
                    if (g.isNotEmpty()) currentGrade = g
                }
                line.startsWith("### ") -> {
                    val name = line.removePrefix("### ").trim()
                    catLv2 = name; catLv3 = ""; catLv4 = ""
                    val g = extractGrade(name)
                    if (g.isNotEmpty()) currentGrade = g
                }
                line.startsWith("#### ") -> {
                    val name = line.removePrefix("#### ").trim()
                    catLv3 = name; catLv4 = ""
                    val g = extractGrade(name)
                    if (g.isNotEmpty()) currentGrade = g
                }
                (line.contains("知识点：") && line.startsWith("**")) -> {
                    idCounter.incrementAndGet()
                    val title = line.removePrefix("**").removePrefix("知识点：").trimEnd('*', ' ', '）', ')', '（', '(')

                    var formula = ""
                    val formulaMatch = Regex("""\$\$(.+?)\$\$""").find(line)
                    if (formulaMatch != null) {
                        formula = formulaMatch.groupValues[1]
                    }

                    val contentLines = mutableListOf<String>()
                    i++
                    while (i < lines.size) {
                        val nextLine = lines[i]
                        // 遇到下一个标题或知识点时停止
                        if (nextLine.startsWith("#") || nextLine.trim().startsWith("#") ||
                            nextLine.contains("知识点：")) {
                            i--
                            break
                        }
                        // 连续两个空行时停止
                        if (nextLine.isBlank() && contentLines.isNotEmpty() && contentLines.last().isBlank()) {
                            break
                        }
                        // 跳过 markdown 分隔线（---）和其前后的空行
                        if (nextLine.trim().startsWith("---") || nextLine.trim().startsWith("***")) {
                            // 如果前面已经收集了内容，分隔线表示段落结束
                            if (contentLines.isNotEmpty()) {
                                break
                            }
                            // 否则跳过
                            i++
                            continue
                        }
                        contentLines.add(nextLine)
                        i++
                    }

                    val entityContent = contentLines.joinToString("\n").trim()

                    val fullPath = buildString {
                        append(currentBlock)
                        if (catLv1.isNotEmpty()) append(" > ").append(catLv1)
                        if (catLv2.isNotEmpty()) append(" > ").append(catLv2)
                        if (catLv3.isNotEmpty()) append(" > ").append(catLv3)
                    }

                    val id = generateId(currentBlock, currentGrade, idCounter.get())

                    // 验证关键字段非空
                    var valid = true
                    if (currentBlock.isEmpty()) {
                        LogFileManager.log("KnowledgeImporter",
                            "WARNING: empty block for entity #${idCounter.get()} ($title)")
                        valid = false
                    }
                    if (title.isEmpty()) {
                        LogFileManager.log("KnowledgeImporter",
                            "WARNING: empty title for entity #${idCounter.get()}")
                        valid = false
                    }

                    if (valid) {
                        entities.add(KnowledgeEntity(
                            id = id, block = currentBlock, grade = currentGrade,
                            categoryLv1 = catLv1, categoryLv2 = catLv2,
                            categoryLv3 = catLv3, categoryLv4 = catLv4,
                            title = title, formula = formula,
                            content = entityContent, fullPath = fullPath
                        ))
                    }
                    continue
                }
            }
            i++
        }
        return entities
    }

    private fun blockIndexToName(indexStr: String): String? {
        // 支持中文数字（一、二、三、四）和阿拉伯数字（1、2、3、4）
        val chineseDigits = mapOf('一' to 1, '二' to 2, '三' to 3, '四' to 4)
        val first = indexStr.firstOrNull() ?: return null
        val idx = when {
            first in chineseDigits -> chineseDigits[first]!!
            first.isDigit() -> first.digitToInt()
            else -> return null
        }
        return blockMap.getOrNull(idx - 1)
    }

    private fun extractGrade(text: String): String {
        gradeKeywords.forEach { (key, value) ->
            if (text.contains(key)) return value
        }
        return ""
    }

    private fun generateId(block: String, grade: String, id: Int): String {
        val blockCode = when {
            block.contains("数") -> "A"
            block.contains("图") -> "G"
            block.contains("概") -> "P"
            block.contains("践") -> "S"
            else -> "X"
        }
        val gradeCode = when {
            grade.contains("五") -> "G5"
            grade.contains("六") -> "G6"
            grade.contains("初") -> "G7"
            grade.contains("八") -> "G8"
            grade.contains("九") -> "G9"
            grade.contains("高") -> "G10"
            else -> "G00"
        }
        return "KB_${blockCode}_${gradeCode}_K${String.format("%03d", id)}"
    }
}
