package com.mathkatex.verify.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathkatex.verify.data.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    // 加载当前设置
    var provider by remember { mutableStateOf(SettingsManager.provider) }

    // Xiaomi 设置
    var xiaomiApiKey by remember { mutableStateOf(SettingsManager.xiaomiApiKey) }
    var xiaomiModel by remember { mutableStateOf(SettingsManager.xiaomiModel) }
    var xiaomiBaseUrl by remember { mutableStateOf(SettingsManager.xiaomiBaseUrl) }
    var showXiaomiKey by remember { mutableStateOf(false) }

    // DeepSeek 设置
    var deepseekApiKey by remember { mutableStateOf(SettingsManager.deepseekApiKey) }
    var deepseekModel by remember { mutableStateOf(SettingsManager.deepseekModel) }
    var deepseekBaseUrl by remember { mutableStateOf(SettingsManager.deepseekBaseUrl) }
    var showDeepseekKey by remember { mutableStateOf(false) }

    // 拍照提示词
    var photoPrompt by remember { mutableStateOf(SettingsManager.photoPrompt) }
    var photoPromptEnabled by remember { mutableStateOf(SettingsManager.photoPromptEnabled) }

    var savedToast by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============ 模型选择 ============
            Text("AI 模型配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "当前使用的模型服务商",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Provider 切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = provider == "XIAOMI",
                            onClick = {
                                provider = "XIAOMI"
                                SettingsManager.provider = "XIAOMI"
                            },
                            label = { Text("小米 Mimo") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Smartphone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = provider == "DEEPSEEK",
                            onClick = {
                                provider = "DEEPSEEK"
                                SettingsManager.provider = "DEEPSEEK"
                            },
                            label = { Text("DeepSeek") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ============ Xiaomi Mimo 配置 ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("小米 Mimo 配置", fontWeight = FontWeight.SemiBold)
                        if (provider == "XIAOMI") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "当前使用",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = xiaomiApiKey,
                        onValueChange = { xiaomiApiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("输入 Xiaomi Mimo API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showXiaomiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showXiaomiKey = !showXiaomiKey }) {
                                Icon(
                                    if (showXiaomiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = xiaomiModel,
                        onValueChange = { xiaomiModel = it },
                        label = { Text("模型名称") },
                        placeholder = { Text("mimo-v2.5-pro") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("例如：mimo-v2.5-pro、mimo-v2.5") }
                    )

                    OutlinedTextField(
                        value = xiaomiBaseUrl,
                        onValueChange = { xiaomiBaseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.xiaomimimo.com/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("API 请求的基础地址") }
                    )

                    Button(
                        onClick = {
                            SettingsManager.xiaomiApiKey = xiaomiApiKey
                            SettingsManager.xiaomiModel = xiaomiModel
                            SettingsManager.xiaomiBaseUrl = xiaomiBaseUrl
                            savedToast = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存小米配置")
                    }
                }
            }

            // ============ DeepSeek 配置 ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DeepSeek 配置", fontWeight = FontWeight.SemiBold)
                        if (provider == "DEEPSEEK") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "当前使用",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = deepseekApiKey,
                        onValueChange = { deepseekApiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("输入 DeepSeek API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showDeepseekKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showDeepseekKey = !showDeepseekKey }) {
                                Icon(
                                    if (showDeepseekKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = deepseekModel,
                        onValueChange = { deepseekModel = it },
                        label = { Text("模型名称") },
                        placeholder = { Text("deepseek-chat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("例如：deepseek-chat、deepseek-v4-pro") }
                    )

                    OutlinedTextField(
                        value = deepseekBaseUrl,
                        onValueChange = { deepseekBaseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.deepseek.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("API 请求的基础地址（不含 /chat/completions）") }
                    )

                    Button(
                        onClick = {
                            SettingsManager.deepseekApiKey = deepseekApiKey
                            SettingsManager.deepseekModel = deepseekModel
                            SettingsManager.deepseekBaseUrl = deepseekBaseUrl
                            savedToast = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存 DeepSeek 配置")
                    }
                }
            }

            // ============ 拍照解题提示词 ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("拍照解题提示词", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("启用自定义提示词", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = photoPromptEnabled,
                            onCheckedChange = {
                                photoPromptEnabled = it
                                SettingsManager.photoPromptEnabled = it
                            }
                        )
                    }

                    if (photoPromptEnabled) {
                        OutlinedTextField(
                            value = photoPrompt,
                            onValueChange = { photoPrompt = it },
                            label = { Text("自定义提示词") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            minLines = 4,
                            maxLines = 8,
                            supportingText = { Text("提交分析时使用此提示词替代默认提示词") }
                        )

                        Button(
                            onClick = {
                                SettingsManager.photoPrompt = photoPrompt
                                savedToast = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存提示词")
                        }
                    } else {
                        Text(
                            text = "默认提示词：\"请仔细分析这张图片中的数学题目，给出完整的解题步骤和最终答案。\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ============ 关于 ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("关于", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "数学知识助手 v1.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "拍照解题 | 知识点速查 | 例题练习",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 保存成功提示
            if (savedToast) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✅ 设置已保存",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    savedToast = false
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
