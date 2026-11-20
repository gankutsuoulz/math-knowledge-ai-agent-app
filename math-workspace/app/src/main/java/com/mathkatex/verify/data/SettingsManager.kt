package com.mathkatex.verify.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置管理（使用 SharedPreferences 持久化）
 * 支持多模型配置，每个 Provider 独立存储 API Key / 模型名 / BaseURL
 */
object SettingsManager {

    private const val PREFS_NAME = "math_app_settings"

    // 通用
    private const val KEY_PROVIDER = "provider"

    // Xiaomi Mimo 配置
    private const val KEY_XIAOMI_API_KEY = "xiaomi_api_key"
    private const val KEY_XIAOMI_MODEL = "xiaomi_model"
    private const val KEY_XIAOMI_VISION_MODEL = "xiaomi_vision_model"
    private const val KEY_XIAOMI_BASE_URL = "xiaomi_base_url"

    // DeepSeek 配置
    private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
    private const val KEY_DEEPSEEK_BASE_URL = "deepseek_base_url"

    // 拍照解题自定义提示词
    private const val KEY_PHOTO_PROMPT = "photo_prompt"
    private const val KEY_PHOTO_PROMPT_ENABLED = "photo_prompt_enabled"

    // 默认值
    private const val DEFAULT_XIAOMI_MODEL = "mimo-v2.6-pro-ultraspeed"
    private const val DEFAULT_XIAOMI_VISION_MODEL = "mimo-v2.6-pro-ultraspeed"
    private const val DEFAULT_XIAOMI_BASE_URL = "https://api.xiaomimimo.com/v1"
    private const val DEFAULT_DEEPSEEK_MODEL = "deepseek-chat"
    private const val DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com"
    private const val DEFAULT_PHOTO_PROMPT = "请仔细分析这张图片中的数学题目，给出完整的解题步骤和最终答案。"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ========== Provider 切换 ==========

    var provider: String
        get() = prefs.getString(KEY_PROVIDER, "XIAOMI") ?: "XIAOMI"
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    fun getLLMProvider(): com.mathkatex.verify.data.service.LLMService.Provider {
        return when (provider) {
            "DEEPSEEK" -> com.mathkatex.verify.data.service.LLMService.Provider.DEEPSEEK
            else -> com.mathkatex.verify.data.service.LLMService.Provider.XIAOMI
        }
    }

    // ========== Xiaomi Mimo ==========

    var xiaomiApiKey: String
        get() = prefs.getString(KEY_XIAOMI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_XIAOMI_API_KEY, value).apply()

    var xiaomiModel: String
        get() = upgradeDeprecatedModel(
            prefs.getString(KEY_XIAOMI_MODEL, DEFAULT_XIAOMI_MODEL) ?: DEFAULT_XIAOMI_MODEL
        )
        set(value) = prefs.edit().putString(KEY_XIAOMI_MODEL, value).apply()

    var xiaomiVisionModel: String
        get() = upgradeDeprecatedModel(
            prefs.getString(KEY_XIAOMI_VISION_MODEL, DEFAULT_XIAOMI_VISION_MODEL) ?: DEFAULT_XIAOMI_VISION_MODEL
        )
        set(value) = prefs.edit().putString(KEY_XIAOMI_VISION_MODEL, value).apply()

    /** mimo-v2.5 系列已于 2026-10-21 下线，读到旧配置自动升级到 v2.6-pro。 */
    private fun upgradeDeprecatedModel(name: String): String =
        if (name.startsWith("mimo-v2.5")) DEFAULT_XIAOMI_VISION_MODEL else name

    var xiaomiBaseUrl: String
        get() = prefs.getString(KEY_XIAOMI_BASE_URL, DEFAULT_XIAOMI_BASE_URL) ?: DEFAULT_XIAOMI_BASE_URL
        set(value) = prefs.edit().putString(KEY_XIAOMI_BASE_URL, value).apply()

    // ========== DeepSeek ==========

    var deepseekApiKey: String
        get() = prefs.getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_API_KEY, value).apply()

    var deepseekModel: String
        get() = prefs.getString(KEY_DEEPSEEK_MODEL, DEFAULT_DEEPSEEK_MODEL) ?: DEFAULT_DEEPSEEK_MODEL
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_MODEL, value).apply()

    var deepseekBaseUrl: String
        get() = prefs.getString(KEY_DEEPSEEK_BASE_URL, DEFAULT_DEEPSEEK_BASE_URL) ?: DEFAULT_DEEPSEEK_BASE_URL
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_BASE_URL, value).apply()

    // ========== 拍照解题 ==========

    var photoPrompt: String
        get() = prefs.getString(KEY_PHOTO_PROMPT, DEFAULT_PHOTO_PROMPT) ?: DEFAULT_PHOTO_PROMPT
        set(value) = prefs.edit().putString(KEY_PHOTO_PROMPT, value).apply()

    var photoPromptEnabled: Boolean
        get() = prefs.getBoolean(KEY_PHOTO_PROMPT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PHOTO_PROMPT_ENABLED, value).apply()

    // ========== LLMService 配置获取 ==========

    /**
     * 根据当前选中的 Provider 获取完整的配置信息
     */
    /**
     * 根据当前选中的 Provider 获取完整的配置信息（纯文本模型）
     */
    fun getActiveLLMConfig(): LLMConfig {
        return when (provider) {
            "DEEPSEEK" -> LLMConfig(
                apiKey = deepseekApiKey,
                model = deepseekModel,
                baseUrl = deepseekBaseUrl
            )
            else -> LLMConfig(
                apiKey = xiaomiApiKey,
                model = xiaomiModel,
                baseUrl = xiaomiBaseUrl
            )
        }
    }

    /**
     * 获取视觉模型配置（用于图片分析）
     * 小米 Mimo 的视觉模型与纯文本模型不同
     */
    fun getVisionLLMConfig(): LLMConfig {
        return when (provider) {
            "DEEPSEEK" -> LLMConfig(
                apiKey = deepseekApiKey,
                model = deepseekModel,
                baseUrl = deepseekBaseUrl
            )
            else -> LLMConfig(
                apiKey = xiaomiApiKey,
                model = xiaomiVisionModel,
                baseUrl = xiaomiBaseUrl
            )
        }
    }

    data class LLMConfig(
        val apiKey: String,
        val model: String,
        val baseUrl: String
    )
}
