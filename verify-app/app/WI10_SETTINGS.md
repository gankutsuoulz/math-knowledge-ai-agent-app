# 设置模块代码实现

下面是完整的设置模块代码实现，遵循Clean Architecture原则，包含ViewModel、设置页面、API Key管理和Repository。

## 1. 数据模型和接口定义

### ThemeMode.kt
```kotlin
// com.mathknowledge.app.domain.model.ThemeMode
package com.mathknowledge.app.domain.model

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
```

### SettingsState.kt
```kotlin
// com.mathknowledge.app.domain.model.SettingsState
package com.mathknowledge.app.domain.model

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val apiKey: String = "",
    val isApiKeyValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### SettingsRepository.kt
```kotlin
// com.mathknowledge.app.domain.repository.SettingsRepository
package com.mathknowledge.app.domain.repository

import com.mathknowledge.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(themeMode: ThemeMode)
    fun getApiKey(): Flow<String>
    suspend fun setApiKey(apiKey: String)
    suspend fun clearApiKey()
    suspend fun validateApiKey(apiKey: String): Boolean
    suspend fun clearCache()
}
```

## 2. 数据层实现

### EncryptedPreferencesManager.kt
```kotlin
// com.mathknowledge.app.data.local.EncryptedPreferencesManager
package com.mathknowledge.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.mathknowledge.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedSharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "math_knowledge_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_API_KEY = "api_key"
    }

    fun getThemeMode(): ThemeMode {
        val themeModeString = encryptedSharedPreferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(themeModeString ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        encryptedSharedPreferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .apply()
    }

    fun getApiKey(): String {
        return encryptedSharedPreferences.getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(apiKey: String) {
        encryptedSharedPreferences.edit()
            .putString(KEY_API_KEY, apiKey)
            .apply()
    }

    fun clearApiKey() {
        encryptedSharedPreferences.edit()
            .remove(KEY_API_KEY)
            .apply()
    }
}
```

### SettingsRepositoryImpl.kt
```kotlin
// com.mathknowledge.app.data.repository.SettingsRepositoryImpl
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.EncryptedPreferencesManager
import com.mathknowledge.app.data.remote.api.MathKnowledgeApi
import com.mathknowledge.app.domain.model.ThemeMode
import com.mathknowledge.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val api: MathKnowledgeApi
) : SettingsRepository {

    private val themeModeFlow = MutableStateFlow(encryptedPreferencesManager.getThemeMode())
    private val apiKeyFlow = MutableStateFlow(encryptedPreferencesManager.getApiKey())

    override fun getThemeMode(): Flow<ThemeMode> = themeModeFlow.asStateFlow()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        encryptedPreferencesManager.setThemeMode(themeMode)
        themeModeFlow.value = themeMode
    }

    override fun getApiKey(): Flow<String> = apiKeyFlow.asStateFlow()

    override suspend fun setApiKey(apiKey: String) {
        encryptedPreferencesManager.setApiKey(apiKey)
        apiKeyFlow.value = apiKey
    }

    override suspend fun clearApiKey() {
        encryptedPreferencesManager.clearApiKey()
        apiKeyFlow.value = ""
    }

    override suspend fun validateApiKey(apiKey: String): Boolean {
        return try {
            // 调用API验证密钥有效性
            val response = api.validateApiKey(apiKey)
            response.isValid
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun clearCache() {
        // 清除应用缓存
        // 这里可以添加具体的缓存清除逻辑
    }
}
```

## 3. ViewModel实现

### SettingsViewModel.kt
```kotlin
// com.mathknowledge.app.presentation.settings.SettingsViewModel
package com.mathknowledge.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathknowledge.app.domain.model.SettingsState
import com.mathknowledge.app.domain.model.ThemeMode
import com.mathknowledge.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getThemeMode().collect { themeMode ->
                _uiState.update { it.copy(themeMode = themeMode) }
            }
        }

        viewModelScope.launch {
            settingsRepository.getApiKey().collect { apiKey ->
                _uiState.update { it.copy(apiKey = apiKey) }
                if (apiKey.isNotEmpty()) {
                    validateApiKey(apiKey)
                }
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
            _uiState.update { it.copy(themeMode = themeMode) }
        }
    }

    fun setApiKey(apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                settingsRepository.setApiKey(apiKey)
                _uiState.update { it.copy(apiKey = apiKey, isLoading = false) }
                validateApiKey(apiKey)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Failed to save API key: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            settingsRepository.clearApiKey()
            _uiState.update { 
                it.copy(
                    apiKey = "", 
                    isApiKeyValid = false,
                    error = null
                ) 
            }
        }
    }

    private fun validateApiKey(apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val isValid = settingsRepository.validateApiKey(apiKey)
                _uiState.update { 
                    it.copy(
                        isApiKeyValid = isValid,
                        isLoading = false,
                        error = if (!isValid) "Invalid API key" else null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isApiKeyValid = false,
                        isLoading = false,
                        error = "Failed to validate API key: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                settingsRepository.clearCache()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to clear cache: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

## 4. UI组件实现

### SettingsScreen.kt
```kotlin
// com.mathknowledge.app.presentation.settings.SettingsScreen
package com.mathknowledge.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font