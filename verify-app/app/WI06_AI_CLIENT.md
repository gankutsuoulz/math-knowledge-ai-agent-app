# AI服务客户端封装 - 完整代码

## 项目结构

```
com.mathknowledge.app/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── AiApiService.kt
│   │   │   └── ImageApiService.kt
│   │   ├── interceptor/
│   │   │   ├── AuthInterceptor.kt
│   │   │   ├── LoggingInterceptor.kt
│   │   │   └── UsageInterceptor.kt
│   │   ├── model/
│   │   │   ├── ChatCompletionRequest.kt
│   │   │   ├── ChatCompletionResponse.kt
│   │   │   ├── ImageRecognitionRequest.kt
│   │   │   └── ImageRecognitionResponse.kt
│   │   └── client/
│   │       ├── AiApiClient.kt
│   │       └── AiApiConfig.kt
│   ├── repository/
│   │   ├── AiChatRepositoryImpl.kt
│   │   └── ImageRecognitionRepositoryImpl.kt
│   └── monitor/
│       └── UsageMonitor.kt
├── domain/
│   ├── model/
│   │   ├── ChatMessage.kt
│   │   ├── ChatSession.kt
│   │   └── UsageStats.kt
│   ├── repository/
│   │   ├── AiChatRepository.kt
│   │   └── ImageRecognitionRepository.kt
│   └── usecase/
│       ├── SendChatMessageUseCase.kt
│       ├── StreamChatMessageUseCase.kt
│       └── RecognizeImageUseCase.kt
└── di/
    └── AiModule.kt
```

---

## 1. API配置

### AiApiConfig.kt

```kotlin
package com.mathknowledge.app.data.remote.client

import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI API 配置管理
 * 支持 MiMo (Xiaomi) 和 DeepSeek 两个 API 提供商
 */
@Singleton
class AiApiConfig @Inject constructor() {

    // ==================== MiMo (Xiaomi) API 配置 ====================
    object MiMo {
        const val BASE_URL = "https://api.xiaomi.com/v1/"
        const val API_KEY = "" // 从 BuildConfig 或安全存储中获取
        const val MODEL = "MiMo-7B-RL"
        const val MAX_TOKENS = 4096
        const val TEMPERATURE = 0.7f
        const val TOP_P = 0.9f
    }

    // ==================== DeepSeek API 配置 ====================
    object DeepSeek {
        const val BASE_URL = "https://api.deepseek.com/v1/"
        const val API_KEY = "" // 从 BuildConfig 或安全存储中获取
        const val MODEL_CHAT = "deepseek-chat"
        const val MODEL_CODER = "deepseek-coder"
        const val MODEL_REASONER = "deepseek-reasoner"
        const val MAX_TOKENS = 8192
        const val TEMPERATURE = 0.7f
        const val TOP_P = 0.9f
    }

    // ==================== 默认配置 ====================
    object Defaults {
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 120L
        const val WRITE_TIMEOUT_SECONDS = 60L
        const val LOG_LEVEL = "BODY" // NONE, BASIC, HEADERS, BODY
        const val MAX_RETRY_COUNT = 3
        const val RETRY_DELAY_MS = 1000L
    }

    // ==================== 费用配置 (每百万Token) ====================
    object Pricing {
        // MiMo 定价 (估算)
        const val MIMO_INPUT_PRICE_PER_MILLION = 0.50  // USD
        const val MIMO_OUTPUT_PRICE_PER_MILLION = 1.50 // USD

        // DeepSeek 定价
        const val DEEPSEEK_CHAT_INPUT_PRICE_PER_MILLION = 0.14  // USD
        const val DEEPSEEK_CHAT_OUTPUT_PRICE_PER_MILLION = 0.28 // USD
        const val DEEPSEEK_CODER_INPUT_PRICE_PER_MILLION = 0.14
        const val DEEPSEEK_CODER_OUTPUT_PRICE_PER_MILLION = 0.28
        const val DEEPSEEK_REASONER_INPUT_PRICE_PER_MILLION = 0.55
        const val DEEPSEEK_REASONER_OUTPUT_PRICE_PER_MILLION = 2.19
    }

    /**
     * 获取当前活跃的 API 提供商配置
     */
    fun getActiveProvider(provider: AiProvider): ProviderConfig {
        return when (provider) {
            AiProvider.MIMO -> ProviderConfig(
                name = "MiMo",
                baseUrl = MiMo.BASE_URL,
                apiKey = MiMo.API_KEY,
                model = MiMo.MODEL,
                maxTokens = MiMo.MAX_TOKENS,
                temperature = MiMo.TEMPERATURE,
                topP = MiMo.TOP_P,
                inputPricePerMillion = Pricing.MIMO_INPUT_PRICE_PER_MILLION,
                outputPricePerMillion = Pricing.MIMO_OUTPUT_PRICE_PER_MILLION
            )
            AiProvider.DEEPSEEK -> ProviderConfig(
                name = "DeepSeek",
                baseUrl = DeepSeek.BASE_URL,
                apiKey = DeepSeek.API_KEY,
                model = DeepSeek.MODEL_CHAT,
                maxTokens = DeepSeek.MAX_TOKENS,
                temperature = DeepSeek.TEMPERATURE,
                topP = DeepSeek.TOP_P,
                inputPricePerMillion = Pricing.DEEPSEEK_CHAT_INPUT_PRICE_PER_MILLION,
                outputPricePerMillion = Pricing.DEEPSEEK_CHAT_OUTPUT_PRICE_PER_MILLION
            )
        }
    }

    data class ProviderConfig(
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val maxTokens: Int,
        val temperature: Float,
        val topP: Float,
        val inputPricePerMillion: Double,
        val outputPricePerMillion: Double
    )

    enum class AiProvider {
        MIMO, DEEPSEEK
    }
}
```

---

## 2. 数据模型

### ChatCompletionRequest.kt

```kotlin
package com.mathknowledge.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Chat Completion 请求体
 * 兼容 OpenAI 格式 (MiMo 和 DeepSeek 均支持)
 */
data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<RequestMessage>,

    @SerializedName("temperature")
    val temperature: Float? = null,

    @SerializedName("top_p")
    val topP: Float? = null,

    @SerializedName("max_tokens")
    val maxTokens: Int? = null,

    @SerializedName("stream")
    val stream: Boolean = false,

    @SerializedName("frequency_penalty")
    val frequencyPenalty: Float? = null,

    @SerializedName("presence_penalty")
    val presencePenalty: Float? = null,

    @SerializedName("stop")
    val stop: List<String>? = null,

    @SerializedName("n")
    val n: Int = 1,

    @SerializedName("user")
    val user: String? = null
) {
    /**
     * 请求消息体
     */
    data class RequestMessage(
        @SerializedName("role")
        val role: MessageRole,

        @SerializedName("content")
        val content: String,

        @SerializedName("name")
        val name: String? = null
    )

    enum class MessageRole(val value: String) {
        @SerializedName("system") SYSTEM("system"),
        @SerializedName("user") USER("user"),
        @SerializedName("assistant") ASSISTANT("assistant"),
        @SerializedName("function") FUNCTION("function");

        companion object {
            fun fromString(value: String): MessageRole {
                return entries.find { it.value == value } ?: USER
            }
        }
    }

    companion object {
        /**
         * 构建数学问题的请求
         */
        fun forMathProblem(
            model: String,
            problem: String,
            systemPrompt: String = MATH_SYSTEM_PROMPT,
            temperature: Float = 0.3f,
            maxTokens: Int = 4096
        ): ChatCompletionRequest {
            return ChatCompletionRequest(
                model = model,
                messages = listOf(
                    RequestMessage(
                        role = MessageRole.SYSTEM,
                        content = systemPrompt
                    ),
                    RequestMessage(
                        role = MessageRole.USER,
                        content = problem
                    )
                ),
                temperature = temperature,
                maxTokens = maxTokens
            )
        }

        /**
         * 构建多轮对话请求
         */
        fun forConversation(
            model: String,
            history: List<Pair<String, String>>, // (role, content)
            newMessage: String,
            systemPrompt: String? = null,
            temperature: Float = 0.7f,
            maxTokens: Int = 4096
        ): ChatCompletionRequest {
            val messages = mutableListOf<RequestMessage>()

            systemPrompt?.let {
                messages.add(RequestMessage(role = MessageRole.SYSTEM, content = it))
            }

            history.forEach { (role, content) ->
                messages.add(
                    RequestMessage(
                        role = MessageRole.fromString(role),
                        content = content
                    )
                )
            }

            messages.add(RequestMessage(role = MessageRole.USER, content = newMessage))

            return ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens
            )
        }

        const val MATH_SYSTEM_PROMPT = """
你是一位专业的数学知识助手，擅长解答各类数学问题。请遵循以下原则：
1. 用清晰的步骤解释解题过程
2. 使用 LaTeX 格式表示数学公式
3. 如果可能，提供多种解题方法
4. 给出最终答案并进行验证
5. 用中文回答
"""
    }
}
```

### ChatCompletionResponse.kt

```kotlin
package com.mathknowledge.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Chat Completion 响应体
 */
data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("object")
    val objectType: String = "chat.completion",

    @SerializedName("created")
    val created: Long,

    @SerializedName("model")
    val model: String,

    @SerializedName("choices")
    val choices: List<Choice>,

    @SerializedName("usage")
    val usage: Usage?,

    @SerializedName("system_fingerprint")
    val systemFingerprint: String? = null
) {
    data class Choice(
        @SerializedName("index")
        val index: Int,

        @SerializedName("message")
        val message: ResponseMessage,

        @SerializedName("finish_reason")
        val finishReason: String? // "stop", "length", "content_filter"
    )

    data class ResponseMessage(
        @SerializedName("role")
        val role: String,

        @SerializedName("content")
        val content: String
    )

    data class Usage(
        @SerializedName("prompt_tokens")
        val promptTokens: Int,

        @SerializedName("completion_tokens")
        val completionTokens: Int,

        @SerializedName("total_tokens")
        val totalTokens: Int
    ) {
        /**
         * 计算输入费用 (USD)
         */
        fun calculateInputCost(pricePerMillion: Double): Double {
            return (promptTokens.toDouble() / 1_000_000) * pricePerMillion
        }

        /**
         * 计算输出费用 (USD)
         */
        fun calculateOutputCost(pricePerMillion: Double): Double {
            return (completionTokens.toDouble() / 1_000_000) * pricePerMillion
        }

        /**
         * 计算总费用 (USD)
         */
        fun calculateTotalCost(
            inputPricePerMillion: Double,
            outputPricePerMillion: Double
        ): Double {
            return calculateInputCost(inputPricePerMillion) +
                    calculateOutputCost(outputPricePerMillion)
        }
    }

    /**
     * 获取第一个选择的文本内容
     */
    fun firstChoiceContent(): String {
        return choices.firstOrNull()?.message?.content ?: ""
    }
}

/**
 * SSE 流式响应的单个 chunk
 */
data class ChatCompletionChunk(
    @SerializedName("id")
    val id: String,

    @SerializedName("object")
    val objectType: String = "chat.completion.chunk",

    @SerializedName("created")
    val created: Long,

    @SerializedName("model")
    val model: String,

    @SerializedName("choices")
    val choices: List<ChunkChoice>,

    @SerializedName("usage")
    val usage: ChatCompletionResponse.Usage? = null
) {
    data class ChunkChoice(
        @SerializedName("index")
        val index: Int,

        @SerializedName("delta")
        val delta: Delta,

        @SerializedName("finish_reason")
        val finishReason: String?
    )

    data class Delta(
        @SerializedName("role")
        val role: String? = null,

        @SerializedName("content")
        val content: String? = null
    )
}
```

### ImageRecognitionRequest.kt

```kotlin
package com.mathknowledge.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * 图像识别请求 (兼容多种视觉模型)
 */
data class ImageRecognitionRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<ImageMessage>,

    @SerializedName("max_tokens")
    val maxTokens: Int = 2048,

    @SerializedName("temperature")
    val temperature: Float = 0.3f,

    @SerializedName("stream")
    val stream: Boolean = false
) {
    data class ImageMessage(
        @SerializedName("role")
        val role: String = "user",

        @SerializedName("content")
        val content: List<ContentPart>
    )

    sealed class ContentPart {
        data class TextPart(
            @SerializedName("type")
            val type: String = "text",

            @SerializedName("text")
            val text: String
        ) : ContentPart()

        data class ImageUrlPart(
            @SerializedName("type")
            val type: String = "image_url",

            @SerializedName("image_url")
            val imageUrl: ImageUrl
        ) : ContentPart()

        data class ImageBase64Part(
            @SerializedName("type")
            val type: String = "image_url",

            @SerializedName("image_url")
            val imageUrl: ImageUrl
        ) : ContentPart()
    }

    data class ImageUrl(
        @SerializedName("url")
        val url: String,

        @SerializedName("detail")
        val detail: String = "auto" // "auto", "low", "high"
    )

    companion object {
        /**
         * 从 Base64 图像构建请求
         */
        fun fromBase64(
            model: String,
            base64Image: String,
            prompt: String = "请识别这张图片中的数学题目并给出解答",
            mimeType: String = "image/png"
        ): ImageRecognitionRequest {
            return ImageRecognitionRequest(
                model = model,
                messages = listOf(
                    ImageMessage(
                        role = "user",
                        content = listOf(
                            ContentPart.ImageUrlPart(
                                imageUrl = ImageUrl(
                                    url = "data:$mimeType;base64,$base64Image"
                                )
                            ),
                            ContentPart.TextPart(text = prompt)
                        )
                    )
                )
            )
        }

        /**
         * 从 URL 图像构建请求
         */
        fun fromUrl(
            model: String,
            imageUrl: String,
            prompt: String = "请识别这张图片中的数学题目并给出解答"
        ): ImageRecognitionRequest {
            return ImageRecognitionRequest(
                model = model,
                messages = listOf(
                    ImageMessage(
                        role = "user",
                        content = listOf(
                            ContentPart.ImageUrlPart(
                                imageUrl = ImageUrl(url = imageUrl)
                            ),
                            ContentPart.TextPart(text = prompt)
                        )
                    )
                )
            )
        }
    }
}

data class ImageRecognitionResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("model")
    val model: String,

    @SerializedName("choices")
    val choices: List<ChatCompletionResponse.Choice>,

    @SerializedName("usage")
    val usage: ChatCompletionResponse.Usage?
) {
    fun firstChoiceContent(): String {
        return choices.firstOrNull()?.message?.content ?: ""
    }
}
```

---

## 3. 拦截器

### AuthInterceptor.kt

```kotlin
package com.mathknowledge.app.data.remote.interceptor

import com.mathknowledge.app.data.remote.client.AiApiConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证拦截器 - 自动添加 API Key
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val config: AiApiConfig
) : Interceptor {

    @Volatile
    private var currentProvider: AiApiConfig.AiProvider = AiApiConfig.AiProvider.DEEPSEEK

    fun setProvider(provider: AiApiConfig.AiProvider) {
        currentProvider = provider
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val providerConfig = config.getActiveProvider(currentProvider)

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer ${providerConfig.apiKey}")
            .header("Content-Type", "application/json")
            .header("X-Request-Provider", providerConfig.name)
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
```

### LoggingInterceptor.kt

```kotlin
package com.mathknowledge.app.data.remote.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日志拦截器 - 记录请求/响应详情
 */
@Singleton
class LoggingInterceptor @Inject constructor() : Interceptor {

    companion object {
        private const val TAG = "AiApiLog"
        private const val MAX_LOG_LENGTH = 4000
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // 记录请求
        logRequest(request)

        return try {
            val response = chain.proceed(request)
            val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

            // 记录响应
            logResponse(response, duration)

            response
        } catch (e: IOException) {
            val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            Log.e(TAG, "❌ Request failed after ${duration}ms: ${e.message}")

            // 返回模拟错误响应，避免应用崩溃
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Network Error: ${e.message}")
                .body(
                    """
                    {
                        "error": {
                            "message": "${e.message}",
                            "type": "network_error",
                            "code": "network_error"
                        }
                    }
                    """.trimIndent().toResponseBody("application/json".toMediaTypeOrNull())
                )
                .build()
        }
    }

    private fun logRequest(request: okhttp3.Request) {
        val url = request.url
        val method = request.method
        val body = request.body

        Log.d(TAG, """
            ┌──────────────────────────────────────────
            │ 📤 REQUEST: $method $url
            │ Headers: ${request.headers}
            │ Body: ${body?.toString()?.take(500) ?: "null"}
            └──────────────────────────────────────────
        """.trimIndent())
    }

    private fun logResponse(response: Response, durationMs: Long) {
        val code = response.code
        val url = response.request.url
        val emoji = if (code in 200..299) "✅" else "⚠️"

        Log.d(TAG, """
            ┌──────────────────────────────────────────
            │ $emoji RESPONSE: $code ${url.encodedPath}
            │ Duration: ${durationMs}ms
            │ Content-Length: ${response.body?.contentLength() ?: 0}
            └──────────────────────────────────────────
        """.trimIndent())

        // 记录响应体（截断过长内容）
        response.peekBody(MAX_LOG_LENGTH.toLong()).string().let { body ->
            if (body.isNotEmpty()) {
                Log.d(TAG, "Response Body: ${body.take(MAX_LOG_LENGTH)}")
            }
        }
    }
}
```

### UsageInterceptor.kt

```kotlin
package com.mathknowledge.app.data.remote.interceptor

import android.util.Log
import com.google.gson.Gson
import com.mathknowledge.app.data.remote.model.ChatCompletionResponse
import com.mathknowledge.app.data.monitor.UsageMonitor
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用量监控拦截器 - 统计 API 使用量
 */
@Singleton
class UsageInterceptor @Inject constructor(
    private val usageMonitor: UsageMonitor,
    private val gson: Gson
) : Interceptor {

    companion object {
        private const val TAG = "UsageInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val provider = request.header("X-Request-Provider") ?: "Unknown"

        val response = chain.proceed(request)

        // 仅统计成功的响应
        if (response.isSuccessful) {
            try {
                val bodyString = response.body?.string() ?: return response

                // 尝试解析 usage 信息
                val completionResponse = try {
                    gson.fromJson(bodyString, ChatCompletionResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                completionResponse?.usage?.let { usage ->
                    usageMonitor.recordUsage(
                        provider = provider,
                        model = completionResponse.model,
                        promptTokens = usage.promptTokens,
                        completionTokens = usage.completionTokens,
                        totalTokens = usage.totalTokens
                    )

                    Log.d(TAG, """
                        📊 Usage recorded for $provider:
                        - Model: ${completionResponse.model}
                        - Prompt tokens: ${usage.promptTokens}
                        - Completion tokens: ${usage.completionTokens}
                        - Total tokens: ${usage.totalTokens}
                    """.trimIndent())
                }

                // 重新构建 Response（因为 body 已被消费）
                return response.newBuilder()
                    .body(bodyString.toResponseBody(response.body?.contentType()))
                    .build()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse usage data: ${e.message}")
            }
        }

        return response
    }
}
```

---

## 4. Retrofit 服务定义

### AiApiService.kt

```kotlin
package com.mathknowledge.app.data.remote.api

import com.mathknowledge.app.data.remote.model.ChatCompletionRequest
import com.mathknowledge.app.data.remote.model.ChatCompletionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * AI Chat API 服务接口
 * 兼容 OpenAI 格式 (MiMo / DeepSeek)
 */
interface AiApiService {

    /**
     * Chat Completion (非流式)
     */
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    /**
     * Chat Completion (流式 SSE)
     */
    @Streaming
    @POST("chat/completions")
    suspend fun chatCompletionStream(
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>

    /**
     * 获取模型列表
     */
    @POST("models")
    suspend fun listModels(): Response<Any>
}
```

### ImageApiService.kt

```kotlin
package com.mathknowledge.app.data.remote.api

import com.mathknowledge.app.data.remote.model.ImageRecognitionRequest
import com.mathknowledge.app.data.remote.model.ImageRecognitionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 图像识别 API 服务接口
 */
interface ImageApiService {

    /**
     * 图像识别 (视觉模型)
     */
    @POST("chat/completions")
    suspend fun recognizeImage(
        @Body request: ImageRecognitionRequest
    ): Response<ImageRecognitionResponse>
}
```

---

## 5. OkHttp 客户端构建

### AiApiClient.kt

```kotlin
package com.mathknowledge.app.data.remote.client

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mathknowledge.app.data.remote.api.AiApiService
import com.mathknowledge.app.data.remote.api.ImageApiService
import com.mathknowledge.app.data.remote.interceptor.AuthInterceptor
import com.mathknowledge.app.data.remote.interceptor.LoggingInterceptor
import com.mathknowledge.app.data.remote.interceptor.UsageInterceptor
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * AI API 客户端管理器
 * 管理 Retrofit 实例、OkHttp 客户端和 API 服务
 */
@Singleton
class AiApiClient @Inject constructor(
    private val config: AiApiConfig,
    private val authInterceptor: AuthInterceptor,
    private val loggingInterceptor: LoggingInterceptor,
    private val usageInterceptor: UsageInterceptor
) {

    val gson: Gson = GsonBuilder()
        .setLenient()
        .setPrettyPrinting()
        .create()

    // ==================== OkHttp 客户端 ====================

    private val baseOkHttpClient: OkHttpClient by lazy {
        createBaseOkHttpClient()
    }

    private fun createBaseOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.Defaults.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(config.Defaults.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(config.Defaults.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(config.Defaults.CONNECT_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
            .connectionPool(
                ConnectionPool(
                    maxIdleConnections = 5,
                    keepAliveDuration = 5,
                    timeUnit = TimeUnit.MINUTES
                )
            )
            // 拦截器顺序很重要：先添加的后执行
            .addInterceptor(authInterceptor)       // 1. 添加认证
            .addInterceptor(usageInterceptor)       // 2. 统计用量
            .addInterceptor(loggingInterceptor)     // 3. 日志记录

        // Debug 模式