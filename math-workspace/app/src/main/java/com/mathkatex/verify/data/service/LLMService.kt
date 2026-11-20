package com.mathkatex.verify.data.service

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mathkatex.verify.data.SettingsManager
import com.mathkatex.verify.util.LogFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 数学解题 AI 服务 - 支持 DeepSeek 和 小米 Mimo 多模态模型
 *
 * 所有配置（API Key / 模型名 / Base URL）均从 SettingsManager 动态读取。
 * 使用配置快照机制确保线程安全，使用全局 OkHttpClient 单例避免资源浪费。
 */
class LLMService {

    enum class Provider(val displayName: String, val defaultBaseUrl: String) {
        DEEPSEEK("DeepSeek", "https://api.deepseek.com"),
        XIAOMI("小米 (Mimo)", "https://api.xiaomimimo.com/v1")
    }

    /**
     * 配置快照，一次性读取保证原子性
     */
    companion object {
        /** MiMo v2.6 系列最大输出 131072 tokens；DeepSeek 文本模型上限 8192。 */
        private const val MAX_TOKENS_XIAOMI = 131072
        private const val MAX_TOKENS_DEEPSEEK = 8192
    }

    data class ConfigSnapshot(
        val apiKey: String,
        val modelName: String,
        val visionModelName: String,  // 视觉专用模型（小米：mimo-v2.5）
        val baseUrl: String,
        val provider: Provider
    )

    // 使用 volatile + 快照确保可见性和原子读取
    @Volatile
    private var configSnapshot: ConfigSnapshot = ConfigSnapshot(
        apiKey = "",
        modelName = "mimo-v2.5-pro",
        visionModelName = "mimo-v2.5",
        baseUrl = "https://api.xiaomimimo.com/v1",
        provider = Provider.XIAOMI
    )

    /**
     * 从 SettingsManager 读取最新配置并更新快照（线程安全）
     * 照片分析使用视觉模型，文字问答使用文本模型
     */
    fun refreshConfigFromSettings() {
        val config = SettingsManager.getActiveLLMConfig()
        val visionConfig = SettingsManager.getVisionLLMConfig()
        configSnapshot = ConfigSnapshot(
            apiKey = config.apiKey,
            modelName = config.model,
            visionModelName = visionConfig.model,
            baseUrl = config.baseUrl,
            provider = SettingsManager.getLLMProvider()
        )
    }

    /**
     * 手动设置配置（兼容旧接口）
     */
    fun updateConfig(apiKey: String, provider: Provider) {
        val config = SettingsManager.getActiveLLMConfig()
        val visionConfig = SettingsManager.getVisionLLMConfig()
        configSnapshot = ConfigSnapshot(
            apiKey = apiKey,
            modelName = config.model,
            visionModelName = visionConfig.model,
            baseUrl = config.baseUrl,
            provider = provider
        )
    }

    /** 获取当前快照 */
    private fun snapshot(): ConfigSnapshot = configSnapshot

    /** 获取视觉配置快照（使用视觉模型名） */
    private fun snapVision(): ConfigSnapshot = configSnapshot

    /**
     * 获取当前有效的 API 端点 URL（确保末尾不带斜杠）
     */
    private fun maxOutputTokens(provider: Provider): Int =
        if (provider == Provider.XIAOMI) MAX_TOKENS_XIAOMI else MAX_TOKENS_DEEPSEEK

    private fun getApiUrl(): String {
        return snapshot().baseUrl.trimEnd('/')
    }

    /**
     * 使用多模态模型理解图片中的数学题目
     * 注意：小米 Mimo 的视觉模型与纯文本模型是分开的
     * - 视觉：mimo-v2.5（支持图片输入）
     * - 纯文本：mimo-v2.5-pro（不支持图片输入）
     */
    suspend fun analyzeMathImage(imageBase64: String, prompt: String = ""): Result<String> {
        refreshConfigFromSettings()
        val cfg = snapVision()

        if (cfg.apiKey.isEmpty()) {
            return Result.failure(Exception("请先在设置中配置 ${cfg.provider.displayName} API Key"))
        }

        val cleanBase64 = if (imageBase64.startsWith("data:")) {
            imageBase64.substringAfter(",")
        } else {
            imageBase64
        }

        return try {
            val systemPrompt = """你是一个专业的数学解题助手，专门帮助学生解决数学问题。
请仔细分析图片中的数学题目，给出：
1. 题目的文字描述（如果图片中的题目不够清晰，根据图像推断）
2. 解题步骤（分步说明，每步都要清晰）
3. 最终答案

请用清晰的结构回答，使用中文。"""

            val userPrompt = if (prompt.isNotEmpty()) {
                prompt
            } else {
                "请仔细分析这张图片中的数学题目，给出完整的解题步骤和答案。"
            }

            val messagesJson = buildVisionMessagesJson(systemPrompt, userPrompt, cleanBase64)

            val jsonBody = JsonObject().apply {
                addProperty("model", cfg.visionModelName)
                add("messages", com.google.gson.JsonParser.parseString(messagesJson).asJsonArray)
                addProperty("temperature", 0.5)
                addProperty("max_tokens", maxOutputTokens(cfg.provider))
            }.toString()

            LogFileManager.logRequest("LLMService", "provider=${cfg.provider} model=${cfg.visionModelName} url=${getApiUrl()} json=${jsonBody.substring(0, minOf(jsonBody.length, 500))}")

            val response = sendRequest(jsonBody)
            LogFileManager.logResponse("LLMService", response)
            return parseResponse(response)
        } catch (e: Exception) {
            LogFileManager.logError("LLMService", "Vision Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 文字问答（用于追问等场景）
     */
    suspend fun chat(text: String): Result<String> {
        refreshConfigFromSettings()
        val cfg = snapshot()

        if (cfg.apiKey.isEmpty()) {
            return Result.failure(Exception("请先在设置中配置 ${cfg.provider.displayName} API Key"))
        }

        return try {
            val messagesArray = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", "你是一个专业的数学解题助手。")
                })
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", text)
                })
            }

            val jsonBody = JsonObject().apply {
                addProperty("model", cfg.modelName)
                add("messages", messagesArray)
                addProperty("temperature", 0.5)
                addProperty("max_tokens", maxOutputTokens(cfg.provider))
            }.toString()

            val response = sendRequest(jsonBody)
            return parseResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendRequest(jsonBody: String): String = withContext(Dispatchers.IO) {
        val cfg = snapshot()
        val url = "${getApiUrl()}/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${cfg.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            HttpClientProvider.client.newCall(request).execute().use { resp ->
                val code = resp.code
                val body = resp.body?.string() ?: "(empty)"
                Log.d("LLMService", "<<< Response: code=$code")
                if (!resp.isSuccessful) {
                    return@withContext "ERROR:$code:$body"
                }
                body
            }
        } catch (e: Exception) {
            return@withContext "ERROR:NETWORK:${e.message}"
        }
    }

    private fun parseResponse(response: String): Result<String> {
        if (response.startsWith("ERROR:")) {
            return Result.failure(Exception("API 错误: ${response.removePrefix("ERROR:")}"))
        }

        val content = try {
            val jsonObj = com.google.gson.JsonParser.parseString(response).asJsonObject
            jsonObj.getAsJsonArray("choices")
                .firstOrNull()
                ?.asJsonObject
                ?.getAsJsonObject("message")
                ?.getAsJsonPrimitive("content")
                ?.asString
        } catch (e: Exception) {
            return Result.failure(Exception("JSON解析失败: ${e.message}"))
        }

        if (content.isNullOrEmpty()) {
            return Result.failure(Exception("响应为空"))
        }
        return Result.success(content)
    }

    private fun buildVisionMessagesJson(systemPrompt: String, userPrompt: String, imageBase64: String): String {
        val contentArray = JsonArray()

        val imageObject = JsonObject()
        imageObject.addProperty("type", "image_url")
        val imageUrlObject = JsonObject()
        imageUrlObject.addProperty("url", "data:image/jpeg;base64,$imageBase64")
        imageUrlObject.addProperty("detail", "auto")
        imageObject.add("image_url", imageUrlObject)
        contentArray.add(imageObject)

        val textObject = JsonObject()
        textObject.addProperty("type", "text")
        textObject.addProperty("text", userPrompt)
        contentArray.add(textObject)

        val messagesArray = JsonArray()
        val systemMsg = JsonObject()
        systemMsg.addProperty("role", "system")
        systemMsg.addProperty("content", systemPrompt)
        messagesArray.add(systemMsg)

        val userMsg = JsonObject()
        userMsg.addProperty("role", "user")
        userMsg.add("content", contentArray)
        messagesArray.add(userMsg)

        return messagesArray.toString()
    }
}

/**
 * 全局 OkHttpClient 单例，避免重复创建连接池和线程池
 */
object HttpClientProvider {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}
