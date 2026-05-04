package com.mathkatex.verify.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置管理（使用 SharedPreferences 持久化）
 */
object SettingsManager {
    
    private const val PREFS_NAME = "math_app_settings"
    
    private const val KEY_API_KEY = "api_key"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_API_URL = "api_url"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_USER_PROMPT = "user_prompt"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "sk-ccz3j9cjtk6z7e9k95rz3cnzomp54zhstyuuif93n731d9kd") ?: "sk-ccz3j9cjtk6z7e9k95rz3cnzomp54zhstyuuif93n731d9kd"
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()
    
    var provider: String
        get() = prefs.getString(KEY_PROVIDER, "XIAOMI") ?: "XIAOMI"
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()
    
    var apiUrl: String
        get() = prefs.getString(KEY_API_URL, "https://api.xiaomimimo.com/v1") ?: ""
        set(value) = prefs.edit().putString(KEY_API_URL, value).apply()
    
    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()
    
    var userPrompt: String
        get() = prefs.getString(KEY_USER_PROMPT, DEFAULT_USER_PROMPT) ?: DEFAULT_USER_PROMPT
        set(value) = prefs.edit().putString(KEY_USER_PROMPT, value).apply()
    
    fun getLLMProvider(): com.mathkatex.verify.data.service.LLMService.Provider {
        return when (provider) {
            "DEEPSEEK" -> com.mathkatex.verify.data.service.LLMService.Provider.DEEPSEEK
            else -> com.mathkatex.verify.data.service.LLMService.Provider.XIAOMI
        }
    }
    
    val DEFAULT_SYSTEM_PROMPT = """你是一个专业的数学解题助手，专门帮助学生解决数学问题。
请仔细分析图片中的数学题目，给出：
1. 题目的文字描述（如果图片中的题目不够清晰，根据图像推断）
2. 解题思路与涉及到的知识点内容，但是不要给出解题答案
3. 如果图中存在手写部分的内容，判断解题步骤是否存在问题，并指出问题所在以及问题所涉及的知识点内容，同样不要直接给出答案

请用清晰的结构回答，使用中文。""".trimIndent()
    
    val DEFAULT_USER_PROMPT = "请仔细分析这张图片中的数学题目，给出完整的解题步骤和答案。"
}