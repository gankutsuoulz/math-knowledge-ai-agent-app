package com.mathkatex.verify.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.data.SettingsManager
import com.mathkatex.verify.data.service.LLMService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var apiKey by remember { mutableStateOf(SettingsManager.apiKey) }
    var provider by remember { mutableStateOf(SettingsManager.provider) }
    var systemPrompt by remember { mutableStateOf(SettingsManager.systemPrompt) }
    var userPrompt by remember { mutableStateOf(SettingsManager.userPrompt) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showApiKeyText by remember { mutableStateOf(false) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var showUserPromptDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("设置")
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF2196F3).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "配置",
                                fontSize = 11.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF333333)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // AI模型配置
            SettingsSection(title = "AI模型配置") {
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = "AI 供应商",
                    subtitle = if (provider == "XIAOMI") "小米 (Mimo)" else "DeepSeek",
                    onClick = { showProviderDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Key,
                    title = "API Key",
                    subtitle = if (apiKey.isNotEmpty()) "已设置 •••${apiKey.takeLast(4)}" else "未设置",
                    onClick = { showApiKeyDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Functions,
                    title = "图像识别模型",
                    subtitle = "mimo-v2-omni（多模态）",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "文字分析模型",
                    subtitle = if (provider == "XIAOMI") "mimo-v2.5-pro" else "deepseek-chat",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.EditNote,
                    title = "系统提示词",
                    subtitle = "自定义 AI 解题逻辑",
                    onClick = { showSystemPromptDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "用户提示词",
                    subtitle = "自定义用户输入提示词",
                    onClick = { showUserPromptDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 应用设置
            SettingsSection(title = "应用设置") {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "推送通知",
                    subtitle = "接收练习提醒",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = "跟随系统",
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 关于
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本信息",
                    subtitle = "1.0.0 (Build 1)",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "使用协议",
                    subtitle = "查看用户协议和隐私政策",
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 应用信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "数学知识助手",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "五年级～高三学生的数学学习伴侣",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "版本 1.0.0",
                        fontSize = 11.sp,
                        color = Color(0xFFBDBDBD)
                    )
                }
            }
        }
    }
    
    // Provider Selection Dialog
    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = { Text("选择 AI 供应商") },
            text = {
                Column {
                    ProviderOption(
                        name = "小米 (Mimo)",
                        desc = "推荐：支持图像理解，解题更准",
                        selected = provider == "XIAOMI",
                        onClick = {
                            provider = "XIAOMI"
                            SettingsManager.provider = "XIAOMI"
                            showProviderDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProviderOption(
                        name = "DeepSeek",
                        desc = "通用大模型",
                        selected = provider == "DEEPSEEK",
                        onClick = {
                            provider = "DEEPSEEK"
                            SettingsManager.provider = "DEEPSEEK"
                            showProviderDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // API Key Dialog
    if (showApiKeyDialog) {
        var tempApiKey by remember { mutableStateOf(apiKey) }
        
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("设置 API Key") },
            text = {
                Column {
                    val providerLabel = if (provider == "XIAOMI") "小米 Mimo" else "DeepSeek"
                    Text(
                        text = "请输入您的 $providerLabel API Key",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showApiKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKeyText = !showApiKeyText }) {
                                Text(if (showApiKeyText) "隐藏" else "显示", fontSize = 12.sp)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apiKey = tempApiKey
                        SettingsManager.apiKey = tempApiKey
                        showApiKeyDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // System Prompt Dialog
    if (showSystemPromptDialog) {
        var tempPrompt by remember { mutableStateOf(systemPrompt) }
        
        AlertDialog(
            onDismissRequest = { showSystemPromptDialog = false },
            title = { Text("解题提示词设置") },
            text = {
                Column {
                    Text(
                        text = "设置 AI 解题助手使用的系统提示词，控制解题逻辑和输出格式",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempPrompt,
                        onValueChange = { tempPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        label = { Text("System Prompt") },
                        maxLines = 15
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        systemPrompt = tempPrompt
                        SettingsManager.systemPrompt = tempPrompt
                        showSystemPromptDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemPromptDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // User Prompt Dialog
    if (showUserPromptDialog) {
        var tempPrompt by remember { mutableStateOf(userPrompt) }
        
        AlertDialog(
            onDismissRequest = { showUserPromptDialog = false },
            title = { Text("用户提示词设置") },
            text = {
                Column {
                    Text(
                        text = "设置发送给 AI 的用户提示词内容",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempPrompt,
                        onValueChange = { tempPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        label = { Text("User Prompt") },
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userPrompt = tempPrompt
                        SettingsManager.userPrompt = tempPrompt
                        showUserPromptDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserPromptDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ProviderOption(
    name: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(text = desc, fontSize = 12.sp, color = Color(0xFF888888))
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF888888),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(20.dp)
        )
    }
}
