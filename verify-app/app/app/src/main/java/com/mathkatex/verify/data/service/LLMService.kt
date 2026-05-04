package com.mathkatex.verify.data.service

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
 */
class LLMService {
    
    enum class Provider(val displayName: String, val baseUrl: String) {
        DEEPSEEK("DeepSeek", "https://api.deepseek.com/"),
        XIAOMI("小米 (Mimo)", "https://api.xiaomimimo.com")
    }
    
    companion object {
        const val VISION_MODEL = "mimo-v2-omni"
        const val TEXT_MODEL = "mimo-v2.5-pro"
    }
    
    private var apiKey: String = ""
    private var provider: Provider = Provider.XIAOMI
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    fun updateConfig(apiKey: String, provider: Provider) {
        this.apiKey = apiKey
        this.provider = provider
    }
    
    /**
     * 使用多模态模型理解图片中的数学题目
     * @param imageBase64 图片的base64编码（可带 data:image/...;base64, 前缀）
     * @param prompt 提示词
     * @return 解析结果或错误信息
     */
    suspend fun analyzeMathImage(imageBase64: String, prompt: String = ""): Result<String> {
        if (apiKey.isEmpty()) {
            return Result.failure(Exception("请先在设置中配置 API Key"))
        }

        // Strip data URI prefix if present
        val cleanBase64 = if (imageBase64.startsWith("data:")) {
            imageBase64.substringAfter(",")
        } else {
            imageBase64
        }
        
        return try {
            val systemPrompt = SettingsManager.systemPrompt
            
            val userPrompt = if (prompt.isNotEmpty()) {
                "$prompt\n\n${SettingsManager.userPrompt}"
            } else {
                SettingsManager.userPrompt
            }
            
            val messagesJson = buildVisionMessagesJson(systemPrompt, userPrompt, cleanBase64)
            val model = if (provider == Provider.XIAOMI) VISION_MODEL else "deepseek-chat"
            
            val jsonBody = buildString {
                append("{\"model\":\"$model\",")
                append("\"messages\":")
                append(messagesJson)
                append(",\"temperature\":0.5,")
                append("\"max_tokens\":4096}")
            }
            
            LogFileManager.logRequest("LLMService", "provider=$provider model=$model json=${jsonBody.substring(0, minOf(jsonBody.length, 500))}")
            
            val response = sendRequest(jsonBody)
            LogFileManager.logResponse("LLMService", response)
            return parseResponse(response)
        } catch (e: Exception) {
            LogFileManager.logError("LLMService", "Vision Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 文字问答（可选，用于追问等场景）
     */
    suspend fun chat(text: String): Result<String> {
        if (apiKey.isEmpty()) {
            return Result.failure(Exception("请先在设置中配置 API Key"))
        }
        
        return try {
            val messagesJson = buildString {
                append("[")
                append("{\"role\":\"system\",\"content\":\"你是一个专业的数学解题助手。\"},")
                append("{\"role\":\"user\",\"content\":")
                append(escapeJsonString(text))
                append("}")
                append("]")
            }
            
            val model = if (provider == Provider.XIAOMI) TEXT_MODEL else "deepseek-chat"
            val jsonBody = "{\"model\":\"$model\",\"messages\":$messagesJson,\"temperature\":0.5,\"max_tokens\":4096}"
            
            val response = sendRequest(jsonBody)
            return parseResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun sendRequest(jsonBody: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${provider.baseUrl}/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val resp = client.newCall(request).execute()
        val code = resp.code
        val body = resp.body?.string() ?: "(empty)"
        System.out.println("<<< Response: code=$code body=$body")
        if (!resp.isSuccessful) {
            return@withContext "ERROR:$code:$body"
        }
        body
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
        
        // Image part
        val imageObject = JsonObject()
        imageObject.addProperty("type", "image_url")
        val imageUrlObject = JsonObject()
        imageUrlObject.addProperty("url", imageBase64)
        imageUrlObject.addProperty("detail", "auto")
        imageObject.add("image_url", imageUrlObject)
        contentArray.add(imageObject)
        
        // Text part
        val textObject = JsonObject()
        textObject.addProperty("type", "text")
        textObject.addProperty("text", userPrompt)
        contentArray.add(textObject)
        
        // Messages array
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
    
    private fun escapeJsonString(s: String): String {
        return buildString {
            for (c in s) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c.code < 32) append("\\u${String.format("%04x", c.code)}") else append(c)
                }
            }
        }.let { "\"$it\"" }
    }
}
