# AI辅导模块代码实现

下面是基于Clean Architecture的AI辅导模块完整代码实现：

## 1. Domain层 - 模型和接口

### Message.kt
```kotlin
package com.mathknowledge.app.domain.model

import java.util.UUID

sealed class Message {
    abstract val id: String
    abstract val timestamp: Long
    
    data class UserMessage(
        override val id: String = UUID.randomUUID().toString(),
        val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message()
    
    data class AiMessage(
        override val id: String = UUID.randomUUID().toString(),
        val content: String,
        val isLatex: Boolean = false,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message()
    
    data class ErrorMessage(
        override val id: String = UUID.randomUUID().toString(),
        val error: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message()
}
```

### ChatState.kt
```kotlin
package com.mathknowledge.app.domain.model

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### TutoringRepository.kt
```kotlin
package com.mathknowledge.app.domain.repository

import com.mathknowledge.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface TutoringRepository {
    suspend fun sendMessage(message: String): Result<Message>
    fun getMessages(): Flow<List<Message>>
    suspend fun clearChatHistory()
}
```

## 2. Data层 - Repository实现

### TutoringRepositoryImpl.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.domain.model.Message
import com.mathknowledge.app.domain.repository.TutoringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutoringRepositoryImpl @Inject constructor() : TutoringRepository {
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val messages: Flow<List<Message>> = _messages.asStateFlow()
    
    override suspend fun sendMessage(message: String): Result<Message> {
        return try {
            // 模拟网络延迟
            kotlinx.coroutines.delay(1000)
            
            // 模拟AI回复
            val aiResponse = generateAiResponse(message)
            
            // 添加用户消息
            val userMessage = Message.UserMessage(content = message)
            _messages.update { currentMessages ->
                currentMessages + userMessage
            }
            
            // 添加AI回复
            val aiMessage = Message.AiMessage(
                content = aiResponse.content,
                isLatex = aiResponse.isLatex
            )
            _messages.update { currentMessages ->
                currentMessages + aiMessage
            }
            
            Result.success(aiMessage)
        } catch (e: Exception) {
            val errorMessage = Message.ErrorMessage(
                error = e.message ?: "未知错误"
            )
            _messages.update { currentMessages ->
                currentMessages + errorMessage
            }
            Result.failure(e)
        }
    }
    
    override fun getMessages(): Flow<List<Message>> = messages
    
    override suspend fun clearChatHistory() {
        _messages.update { emptyList() }
    }
    
    private fun generateAiResponse(userMessage: String): AiResponse {
        // 这里模拟AI回复逻辑，实际项目中应该调用真实的AI API
        val lowerMessage = userMessage.lowercase()
        
        return when {
            lowerMessage.contains("导数") || lowerMessage.contains("微分") -> {
                AiResponse(
                    content = "导数是函数在某一点的瞬时变化率。对于函数 $f(x)$，其导数定义为：\n\n$$f'(x) = \\lim_{h \\to 0} \\frac{f(x+h) - f(x)}{h}$$\n\n例如，$f(x) = x^2$ 的导数是 $f'(x) = 2x$。",
                    isLatex = true
                )
            }
            lowerMessage.contains("积分") || lowerMessage.contains("微积分") -> {
                AiResponse(
                    content = "积分是微分的逆运算。定积分表示曲线下的面积：\n\n$$\\int_{a}^{b} f(x) dx = F(b) - F(a)$$\n\n其中 $F(x)$ 是 $f(x)$ 的原函数。",
                    isLatex = true
                )
            }
            lowerMessage.contains("矩阵") || lowerMessage.contains("线性代数") -> {
                AiResponse(
                    content = "矩阵是按矩形排列的数字集合。例如，2×2矩阵：\n\n$$\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}$$\n\n矩阵乘法不满足交换律：$AB \\neq BA$。",
                    isLatex = true
                )
            }
            lowerMessage.contains("概率") || lowerMessage.contains("统计") -> {
                AiResponse(
                    content = "概率是事件发生可能性的度量。贝叶斯定理：\n\n$$P(A|B) = \\frac{P(B|A) \\cdot P(A)}{P(B)}$$\n\n这是条件概率的重要公式。",
                    isLatex = true
                )
            }
            lowerMessage.contains("极限") -> {
                AiResponse(
                    content = "极限是函数在某点附近的行为。重要极限：\n\n$$\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1$$\n\n$$\\lim_{n \\to \\infty} \\left(1 + \\frac{1}{n}\\right)^n = e$$",
                    isLatex = true
                )
            }
            lowerMessage.contains("帮助") || lowerMessage.contains("你能做什么") -> {
                AiResponse(
                    content = "我是数学AI辅导助手，可以帮助你：\n\n1. 解释数学概念\n2. 解答数学问题\n3. 提供数学公式和定理\n4. 辅助学习数学知识\n\n你可以问我关于微积分、线性代数、概率统计等数学问题。",
                    isLatex = false
                )
            }
            else -> {
                AiResponse(
                    content = "这是一个很好的问题！让我来解释一下。\n\n数学是研究数量、结构、变化和空间的学科。如果你有具体的数学问题，请告诉我，我会尽力帮助你理解。\n\n你可以尝试问我：\n- 什么是导数？\n- 解释一下积分\n- 矩阵乘法怎么计算？",
                    isLatex = false
                )
            }
        }
    }
    
    private data class AiResponse(
        val content: String,
        val isLatex: Boolean
    )
}
```

## 3. Presentation层 - ViewModel

### TutoringViewModel.kt
```kotlin
package com.mathknowledge.app.presentation.tutoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathknowledge.app.domain.model.ChatState
import com.mathknowledge.app.domain.model.Message
import com.mathknowledge.app.domain.repository.TutoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TutoringViewModel @Inject constructor(
    private val repository: TutoringRepository
) : ViewModel() {
    
    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    init {
        loadMessages()
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            repository.getMessages().collect { messages ->
                _chatState.update { it.copy(messages = messages) }
            }
        }
    }
    
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            _chatState.update { it.copy(isLoading = true, error = null) }
            
            val result = repository.sendMessage(message)
            
            result.fold(
                onSuccess = {
                    // 消息已通过Flow自动更新
                },
                onFailure = { exception ->
                    _chatState.update { 
                        it.copy(error = exception.message ?: "发送消息失败") 
                    }
                }
            )
            
            _chatState.update { it.copy(isLoading = false) }
        }
    }
    
    fun clearError() {
        _chatState.update { it.copy(error = null) }
    }
    
    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }
}
```

## 4. Presentation层 - UI组件

### TutoringScreen.kt
```kotlin
package com.mathknowledge.app.presentation.tutoring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathknowledge.app.domain.model.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutoringScreen(
    viewModel: TutoringViewModel = hiltViewModel()
) {
    val chatState by viewModel.chatState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 当消息列表变化时，滚动到底部
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "AI数学辅导",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearChatHistory() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空对话"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatState.messages) { message ->
                    MessageItem(message = message)
                }
            }
            
            // 错误消息
            AnimatedVisibility(
                visible = chatState.error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                chatState.error?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            
                            TextButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text("关闭")
                            }
                        }
                    }
                }
            }
            
            // 输入框和发送按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value =