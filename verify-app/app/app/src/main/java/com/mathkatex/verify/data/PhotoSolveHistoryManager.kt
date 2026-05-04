package com.mathkatex.verify.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 拍照解题历史记录管理
 * 使用 SharedPreferences + JSON 存储历史记录
 */
object PhotoSolveHistoryManager {

    private const val PREFS_NAME = "photo_solve_history"
    private const val KEY_HISTORY = "history_list"
    private const val MAX_HISTORY_SIZE = 50  // 最多保存50条记录

    // 静态变量用于在导航时传递数据（后备方案）
    private var selectedItemJson: String? = null

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSelectedItemJson(json: String?) {
        selectedItemJson = json
    }

    fun getSelectedItemJson(): String? = selectedItemJson

    fun clearSelectedItemJson() {
        selectedItemJson = null
    }

    /**
     * 添加历史记录
     */
    fun addHistory(item: PhotoSolveHistoryItem) {
        val historyList = getHistoryList().toMutableList()
        // 添加到列表开头（最新的在最前面）
        historyList.add(0, item)
        // 保持最大数量限制
        while (historyList.size > MAX_HISTORY_SIZE) {
            historyList.removeAt(historyList.size - 1)
        }
        saveHistoryList(historyList)
    }

    /**
     * 获取所有历史记录
     */
    fun getHistoryList(): List<PhotoSolveHistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PhotoSolveHistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 删除指定历史记录
     */
    fun deleteHistory(id: String) {
        val historyList = getHistoryList().toMutableList()
        historyList.removeAll { it.id == id }
        saveHistoryList(historyList)
    }

    /**
     * 清空所有历史记录
     */
    fun clearAllHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    /**
     * 保存历史列表到 SharedPreferences
     */
    private fun saveHistoryList(list: List<PhotoSolveHistoryItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}

/**
 * 拍照解题历史记录数据模型
 */
data class PhotoSolveHistoryItem(
    val id: String,                      // 唯一标识
    val problemDescription: String,      // 题目描述（截取前100字）
    val solutionResult: String,           // 解析结果（完整 Markdown）
    val timestamp: Long,                  // 时间戳
    val imagePath: String? = null        // 图片路径（可选，暂时不用）
) {
    companion object {
        /**
         * 从完整结果生成摘要（用于列表显示）
         */
        fun generateSummary(fullResponse: String, maxLength: Int = 80): String {
            // 移除 Markdown 格式符号，提取纯文本
            val cleanText = fullResponse
                .replace(Regex("#+"), "")  // 移除标题符号
                .replace(Regex("\\*\\*"), "")  // 移除粗体
                .replace(Regex("\\*"), "")  // 移除斜体
                .replace(Regex("\\[|\\]"), "")  // 移除方括号
                .replace(Regex("\\\\"), "")  // 移除反斜杠
                .replace(Regex("\\$+"), "")  // 移除美元符号
                .trim()
            return if (cleanText.length > maxLength) {
                cleanText.take(maxLength) + "..."
            } else {
                cleanText
            }
        }
    }
}
