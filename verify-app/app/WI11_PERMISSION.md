# Android 权限处理模块

我将为您创建一个完整的权限处理模块，遵循Clean Architecture原则，使用Kotlin和Jetpack Compose实现。

## 1. 权限类型定义

```kotlin
// com/mathknowledge/app/domain/model/permission/PermissionType.kt
package com.mathknowledge.app.domain.model.permission

import android.Manifest

sealed class PermissionType(val permission: String, val description: String) {
    // 相机权限
    object Camera : PermissionType(
        permission = Manifest.permission.CAMERA,
        description = "需要相机权限来拍摄照片和扫描文档"
    )
    
    // 存储权限
    object Storage : PermissionType(
        permission = Manifest.permission.READ_EXTERNAL_STORAGE,
        description = "需要存储权限来访问和保存文件"
    )
    
    // 写入存储权限（Android 10+）
    object WriteStorage : PermissionType(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        description = "需要写入存储权限来保存文件"
    )
    
    // 位置权限
    object Location : PermissionType(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        description = "需要位置权限来提供基于位置的服务"
    )
    
    // 麦克风权限
    object Microphone : PermissionType(
        permission = Manifest.permission.RECORD_AUDIO,
        description = "需要麦克风权限来录制音频"
    )
    
    // 通知权限（Android 13+）
    object Notification : PermissionType(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        description = "需要通知权限来接收重要更新"
    )
    
    companion object {
        // 相机权限组
        val CAMERA_GROUP = listOf(Camera)
        
        // 存储权限组
        val STORAGE_GROUP = listOf(Storage, WriteStorage)
        
        // 位置权限组
        val LOCATION_GROUP = listOf(Location)
        
        // 音频权限组
        val AUDIO_GROUP = listOf(Microphone)
        
        // 所有权限
        val ALL_PERMISSIONS = listOf(
            Camera, Storage, WriteStorage, Location, Microphone, Notification
        )
        
        // 根据权限字符串获取权限类型
        fun fromPermission(permission: String): PermissionType? {
            return when (permission) {
                Manifest.permission.CAMERA -> Camera
                Manifest.permission.READ_EXTERNAL_STORAGE -> Storage
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> WriteStorage
                Manifest.permission.ACCESS_FINE_LOCATION -> Location
                Manifest.permission.RECORD_AUDIO -> Microphone
                Manifest.permission.POST_NOTIFICATIONS -> Notification
                else -> null
            }
        }
    }
}
```

## 2. 权限状态定义

```kotlin
// com/mathknowledge/app/domain/model/permission/PermissionState.kt
package com.mathknowledge.app.domain.model.permission

sealed class PermissionState {
    object NotRequested : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
    object ShouldShowRationale : PermissionState()
    
    val isGranted: Boolean
        get() = this is Granted
    
    val isDenied: Boolean
        get() = this is Denied || this is PermanentlyDenied
    
    val isPermanentlyDenied: Boolean
        get() = this is PermanentlyDenied
}
```

## 3. 权限请求结果

```kotlin
// com/mathknowledge/app/domain/model/permission/PermissionRequestResult.kt
package com.mathknowledge.app.domain.model.permission

sealed class PermissionRequestResult {
    object Success : PermissionRequestResult()
    object Denied : PermissionRequestResult()
    object PermanentlyDenied : PermissionRequestResult()
    object Cancelled : PermissionRequestResult()
    
    val isSuccess: Boolean
        get() = this is Success
    
    val isDenied: Boolean
        get() = this is Denied || this is PermanentlyDenied
}
```

## 4. 权限管理器

```kotlin
// com/mathknowledge/app/data/permission/PermissionManager.kt
package com.mathknowledge.app.data.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mathknowledge.app.domain.model.permission.PermissionState
import com.mathknowledge.app.domain.model.permission.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * 检查单个权限是否已授予
     */
    fun isPermissionGranted(permissionType: PermissionType): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permissionType.permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查权限组是否已全部授予
     */
    fun arePermissionsGranted(permissionTypes: List<PermissionType>): Boolean {
        return permissionTypes.all { isPermissionGranted(it) }
    }
    
    /**
     * 获取权限状态
     */
    fun getPermissionState(permissionType: PermissionType): PermissionState {
        return when {
            isPermissionGranted(permissionType) -> PermissionState.Granted
            shouldShowRationale(permissionType) -> PermissionState.ShouldShowRationale
            else -> PermissionState.NotRequested
        }
    }
    
    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowRationale(permissionType: PermissionType): Boolean {
        // 在Android 11+中，shouldShowRequestPermissionRationale的行为有所改变
        // 这里我们简化处理，实际项目中可能需要更复杂的逻辑
        return false
    }
    
    /**
     * 检查权限是否被永久拒绝
     * 注意：Android系统没有直接的方法来检查权限是否被永久拒绝
     * 这里我们通过检查权限状态和是否应该显示说明来推断
     */
    fun isPermissionPermanentlyDenied(permissionType: PermissionType): Boolean {
        // 在实际项目中，你可能需要存储权限请求历史来准确判断
        // 这里我们简化处理
        return false
    }
    
    /**
     * 检查是否需要请求权限
     */
    fun shouldRequestPermission(permissionType: PermissionType): Boolean {
        return !isPermissionGranted(permissionType)
    }
    
    /**
     * 检查是否需要请求多个权限
     */
    fun shouldRequestPermissions(permissionTypes: List<PermissionType>): Boolean {
        return permissionTypes.any { shouldRequestPermission(it) }
    }
    
    /**
     * 获取所有未授予的权限
     */
    fun getUngrantedPermissions(permissionTypes: List<PermissionType>): List<PermissionType> {
        return permissionTypes.filter { shouldRequestPermission(it) }
    }
    
    /**
     * 检查是否是Android 13+（需要通知权限）
     */
    fun isAndroid13OrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    
    /**
     * 检查是否是Android 11+（需要特殊存储权限处理）
     */
    fun isAndroid11OrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }
    
    /**
     * 检查是否是Android 10+（需要分区存储）
     */
    fun isAndroid10OrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    
    /**
     * 获取适合当前Android版本的存储权限
     */
    fun getStoragePermissions(): List<PermissionType> {
        return when {
            isAndroid11OrAbove() -> listOf(PermissionType.Storage)
            isAndroid10OrAbove() -> listOf(PermissionType.Storage, PermissionType.WriteStorage)
            else -> listOf(PermissionType.Storage, PermissionType.WriteStorage)
        }
    }
}
```

## 5. 权限ViewModel

```kotlin
// com/mathknowledge/app/presentation/permission/PermissionViewModel.kt
package com.mathknowledge.app.presentation.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathknowledge.app.domain.model.permission.PermissionRequestResult
import com.mathknowledge.app.domain.model.permission.PermissionState
import com.mathknowledge.app.domain.model.permission.PermissionType
import com.mathknowledge.app.domain.usecase.permission.CheckPermissionUseCase
import com.mathknowledge.app.domain.usecase.permission.RequestPermissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionUiState(
    val permissionType: PermissionType? = null,
    val permissionState: PermissionState = PermissionState.NotRequested,
    val showRationaleDialog: Boolean = false,
    val showPermanentlyDeniedDialog: Boolean = false,
    val requestResult: PermissionRequestResult? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val checkPermissionUseCase: CheckPermissionUseCase,
    private val requestPermissionUseCase: RequestPermissionUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()
    
    /**
     * 检查权限状态
     */
    fun checkPermission(permissionType: PermissionType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val state = checkPermissionUseCase(permissionType)
            
            _uiState.update { 
                it.copy(
                    permissionType = permissionType,
                    permissionState = state,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 请求权限
     */
    fun requestPermission(permissionType: PermissionType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = requestPermissionUseCase(permissionType)
            
            _uiState.update { 
                it.copy(
                    permissionType = permissionType,
                    requestResult = result,
                    isLoading = false
                )
            }
            
            // 根据结果更新状态
            when (result) {
                is PermissionRequestResult.Success -> {
                    _uiState.update { 
                        it.copy(permissionState = PermissionState.Granted)
                    }
                }
                is PermissionRequestResult.Denied -> {
                    _uiState.update { 
                        it.copy(permissionState = PermissionState.Denied)
                    }
                }
                is PermissionRequestResult.PermanentlyDenied -> {
                    _uiState.update { 
                        it.copy(
                            permissionState = PermissionState.PermanentlyDenied,
                            showPermanentlyDeniedDialog = true
                        )
                    }
                }
                is PermissionRequestResult.Cancelled -> {
                    // 用户取消，不做特殊处理
                }
            }
        }
    }
    
    /**
     * 请求多个权限
     */
    fun requestPermissions(permissionTypes: List<PermissionType>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val results = permissionTypes.map { permissionType ->
                permissionType to requestPermissionUseCase(permissionType)
            }
            
            val allGranted = results.all { it.second is PermissionRequestResult.Success }
            
            _uiState.update { 
                it.copy(
                    requestResult = if (allGranted) {
                        PermissionRequestResult.Success
                    } else {
                        PermissionRequestResult.Denied
                    },
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 显示权限说明对话框
     */
    fun showRationaleDialog() {
        _uiState.update { it.copy(showRationaleDialog = true) }
    }
    
    /**
     * 隐藏权限说明对话框
     */
    fun hideRationaleDialog() {
        _uiState.update { it.copy(showRationaleDialog = false) }
    }
    
    /**
     * 显示永久拒绝对话框
     */
    fun showPermanentlyDeniedDialog() {
        _uiState.update { it.copy(showPermanentlyDeniedDialog = true) }
    }
    
    /**
     * 隐藏永久拒绝对话框
     */
    fun hidePermanentlyDeniedDialog() {
        _uiState.update { it.copy(showPermanentlyDeniedDialog = false) }
    }
    
    /**
     * 重置状态
     */
    fun resetState() {
        _uiState.update { PermissionUiState() }
    }
    
    /**
     * 清除请求结果
     */
    fun clearRequestResult() {
        _uiState.update { it.copy(requestResult = null) }
    }
}
```

## 6. 权限用例

```kotlin
// com/mathknowledge/app/domain/usecase/permission/CheckPermissionUseCase.kt
package com.mathknowledge.app.domain.usecase.permission

import com.mathknowledge.app.data.permission.PermissionManager
import com.mathknowledge.app.domain.model.permission.PermissionState
import com.mathknowledge.app.domain.model.permission.PermissionType
import javax.inject.Inject

class CheckPermissionUseCase @Inject constructor(
    private val permissionManager: PermissionManager
) {
    suspend operator fun invoke(permissionType: PermissionType): PermissionState {
        return permissionManager.getPermissionState(permissionType)
    }
}
```

```kotlin
// com/mathknowledge/app/domain/usecase/permission/RequestPermissionUseCase.kt
package com.mathknowledge.app.domain.usecase.permission

import com.mathknowledge.app.domain.model.permission.PermissionRequestResult
import com.mathknowledge.app.domain.model.permission.PermissionType
import com.mathknowledge.app.domain.repository.PermissionRepository
import javax.inject.Inject

class RequestPermissionUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    suspend operator fun invoke(permissionType: PermissionType): PermissionRequestResult {
        return permissionRepository.requestPermission(permissionType)
    }
}
```

## 7. 权限仓库接口

```kotlin
// com/mathknowledge/app/domain/repository/PermissionRepository.kt
package com.mathknowledge.app.domain.repository

import com.mathknowledge.app.domain.model.permission.PermissionRequestResult
import com.mathknowledge.app.domain.model.permission.PermissionType

interface PermissionRepository {
    suspend fun requestPermission(permissionType: PermissionType): PermissionRequestResult
    suspend fun requestPermissions(permissionTypes: List<PermissionType>): Map<PermissionType, PermissionRequestResult>
}
```

## 8. 权限仓库实现

```kotlin
// com/mathknowledge/app/data/repository/PermissionRepositoryImpl.kt
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.permission.PermissionManager
import com.mathknowledge.app.domain.model.permission.PermissionRequestResult
import com.mathknowledge.app.domain.model.permission.PermissionType
import com.mathknowledge.app.domain.repository.PermissionRepository
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    private val permissionManager: PermissionManager
) : PermissionRepository {
    
    override suspend fun requestPermission(permissionType: PermissionType): PermissionRequestResult {
        // 在实际实现中，这里应该调用Activity的requestPermission方法
        // 由于我们无法直接访问Activity，这里返回一个模拟结果
        // 在实际项目中，你需要通过回调或事件总线来处理权限请求结果
        
        return if (permissionManager.isPermissionGranted(permissionType)) {
            PermissionRequestResult.Success
        } else {
            // 这里应该实际请求权限，但为了示例，我们返回Denied
            // 在实际项目中，你需要实现权限请求逻辑
            PermissionRequestResult.Denied
        }
    }
    
    override suspend fun requestPermissions(
        permissionTypes: List<PermissionType>
    ): Map<PermissionType, PermissionRequestResult> {
        return permissionTypes.associateWith { permissionType ->
            requestPermission(permissionType)
        }
    }
}
```

## 9. 权限请求对话框

```kotlin
// com/mathknowledge/app/presentation/permission/PermissionRequestDialog.kt
package com.mathknowledge.app.presentation.permission

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mathknowledge.app.domain.model.permission.PermissionType

@Composable
fun PermissionRequestDialog(
    permissionType: PermissionType,
    onGrantPermission: () -> Unit,
    onDenyPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 权限图标
                Text(
                    text = getPermissionIcon(permissionType),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 权限标题
                Text(
                    text = "需要${getPermissionName(permissionType)}权限",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 权限说明
                Text(
                    text = permissionType.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 拒绝按钮
                    OutlinedButton(
                        onClick = onDenyPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("拒绝")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 授予权限按钮
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("授予权限")
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    permissionType: PermissionType,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "权限说明",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 说明内容
                Text(
                    text = "应用需要${getPermissionName(permissionType)}权限才能正常工作。\n\n" +
                            permissionType.description + "\n\n" +
                            "请在设置中授予此权限。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 继续按钮
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("继续")
                }
            }
        }
    }
}

@Composable
fun PermissionPermanentlyDeniedDialog(
    permissionType: PermissionType,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "权限被永久拒绝",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 说明内容
                Text(
                    text = "您已经永久拒绝了${getPermissionName(permissionType)}权限。\n\n" +
                            "要使用此功能，请在系统设置中手动授予此权限。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 取消按钮
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 打开设置按钮
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("打开设置")
                    }
                }
            }
        }
    }
}

// 辅助函数
private fun getPermissionName(permissionType: PermissionType): String {
    return when (permissionType) {
        is PermissionType.Camera -> "相机"
        is PermissionType.Storage -> "存储"
        is PermissionType.Write