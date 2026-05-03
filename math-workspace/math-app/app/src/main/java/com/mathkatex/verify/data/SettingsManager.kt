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
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()
    
    var provider: String
        get() = prefs.getString(KEY_PROVIDER, "XIAOMI") ?: "XIAOMI"
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()
    
    var apiUrl: String
        get() = prefs.getString(KEY_API_URL, "https://api.xiaomimimo.com/v1") ?: ""
        set(value) = prefs.edit().putString(KEY_API_URL, value).apply()
    
    fun getLLMProvider(): com.mathkatex.verify.data.service.LLMService.Provider {
        return when (provider) {
            "DEEPSEEK" -> com.mathkatex.verify.data.service.LLMService.Provider.DEEPSEEK
            else -> com.mathkatex.verify.data.service.LLMService.Provider.XIAOMI
        }
    }
}
