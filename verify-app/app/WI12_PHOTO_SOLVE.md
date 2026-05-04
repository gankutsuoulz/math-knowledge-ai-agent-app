# 拍照解题模块完整代码

## 项目结构

```
com.mathknowledge.app/
├── domain/
│   ├── model/
│   │   ├── PhotoSolveState.kt
│   │   ├── CropRegion.kt
│   │   ├── RecognitionResult.kt
│   │   └── MathProblem.kt
│   ├── repository/
│   │   └── PhotoSolveRepository.kt
│   └── usecase/
│       ├── CapturePhotoUseCase.kt
│       ├── RecognizeMathUseCase.kt
│       └── SaveFavoriteUseCase.kt
├── data/
│   ├── repository/
│   │   └── PhotoSolveRepositoryImpl.kt
│   ├── remote/
│   │   └── AiRecognitionApi.kt
│   └── local/
│       └── FavoriteDao.kt
├── presentation/
│   ├── photosolve/
│   │   ├── PhotoSolveViewModel.kt
│   │   ├── PhotoSolveScreen.kt
│   │   ├── CameraPreviewScreen.kt
│   │   ├── CropScreen.kt
│   │   ├── ResultScreen.kt
│   │   └── KaTeXView.kt
│   └── navigation/
│       └── PhotoSolveNavGraph.kt
└── di/
    └── PhotoSolveModule.kt
```

---

## 1. Domain Layer — Model

```kotlin
// domain/model/PhotoSolveState.kt
package com.mathknowledge.app.domain.model

/**
 * 拍照解题整体状态
 */
data class PhotoSolveState(
    val currentStep: SolveStep = SolveStep.CAMERA,
    val photoUri: String? = null,
    val croppedBitmap: ByteArray? = null,
    val cropRegion: CropRegion? = null,
    val recognitionResult: RecognitionResult? = null,
    val isLoading: Boolean = false,
    val isFlashEnabled: Boolean = false,
    val errorMessage: String? = null,
    val isFavorite: Boolean = false
) {
    enum class SolveStep {
        CAMERA,      // 拍照阶段
        CROP,        // 框选裁剪阶段
        RECOGNIZING, // AI识别中
        RESULT       // 结果展示
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotoSolveState) return false
        return currentStep == other.currentStep &&
                photoUri == other.photoUri &&
                cropRegion == other.cropRegion &&
                recognitionResult == other.recognitionResult &&
                isLoading == other.isLoading &&
                isFlashEnabled == other.isFlashEnabled &&
                errorMessage == other.errorMessage &&
                isFavorite == other.isFavorite
    }

    override fun hashCode(): Int {
        var result = currentStep.hashCode()
        result = 31 * result + (photoUri?.hashCode() ?: 0)
        result = 31 * result + (cropRegion?.hashCode() ?: 0)
        result = 31 * result + (recognitionResult?.hashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isFlashEnabled.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        return result
    }
}
```



```kotlin
// domain/model/CropRegion.kt
package com.mathknowledge.app.domain.model

import android.graphics.RectF

/**
 * 框选区域
 */
data class CropRegion(
    val left: Float,    // 0f ~ 1f 归一化坐标
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toRectF(imageWidth: Int, imageHeight: Int): RectF = RectF(
        left * imageWidth,
        top * imageHeight,
        right * imageWidth,
        bottom * imageHeight
    )

    fun isValid(): Boolean =
        left < right && top < bottom &&
                left >= 0f && top >= 0f &&
                right <= 1f && bottom <= 1f
}
```



```kotlin
// domain/model/MathProblem.kt
package com.mathknowledge.app.domain.model

/**
 * 数学题目领域模型
 */
data class MathProblem(
    val id: String,
    val rawText: String,
    val latexFormula: String,
    val recognizedImage: ByteArray? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MathProblem) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
```

```kotlin
// domain/model/RecognitionResult.kt
package com.mathknowledge.app.domain.model

/**
 * AI识别结果
 */
data class RecognitionResult(
    val problem: MathProblem,
    val solution: Solution,
    val confidence: Float,          // 识别置信度 0~1
    val alternativeResults: List<MathProblem> = emptyList()
)

/**
 * 解题过程
 */
data class Solution(
    val steps: List<SolutionStep>,
    val finalAnswer: String,
    val finalAnswerLatex: String,
    val explanation: String,
    val relatedKnowledge: List<String> = emptyList()
)

/**
 * 单步解题
 */
data class SolutionStep(
    val stepNumber: Int,
    val description: String,
    val formulaLatex: String,
    val explanation: String
)
```

---

## 2. Domain Layer — Repository

```kotlin
// domain/repository/PhotoSolveRepository.kt
package com.mathknowledge.app.domain.repository

import android.net.Uri
import com.mathknowledge.app.domain.model.CropRegion
import com.mathknowledge.app.domain.model.RecognitionResult
import kotlinx.coroutines.flow.Flow

/**
 * 拍照解题 Repository 接口
 */
interface PhotoSolveRepository {

    /**
     * 保存拍照图片到本地
     * @return 本地文件Uri
     */
    suspend fun savePhoto(uri: Uri): Result<String>

    /**
     * 裁剪图片
     * @param photoUri 原始图片Uri
     * @param cropRegion 裁剪区域（归一化坐标）
     * @return 裁剪后的图片字节数组
     */
    suspend fun cropImage(photoUri: String, cropRegion: CropRegion): Result<ByteArray>

    /**
     * 调用AI识别数学题目
     * @param imageBytes 图片字节数组
     * @return 识别结果
     */
    suspend fun recognizeMathProblem(imageBytes: ByteArray): Result<RecognitionResult>

    /**
     * 收藏题目
     */
    suspend fun saveFavorite(result: RecognitionResult): Result<Unit>

    /**
     * 取消收藏
     */
    suspend fun removeFavorite(problemId: String): Result<Unit>

    /**
     * 检查是否已收藏
     */
    suspend fun isFavorite(problemId: String): Result<Boolean>

    /**
     * 获取历史识别记录
     */
    fun getHistory(): Flow<List<RecognitionResult>>
}
```

---

## 3. Domain Layer — Use Cases

```kotlin
// domain/usecase/CapturePhotoUseCase.kt
package com.mathknowledge.app.domain.usecase

import android.net.Uri
import com.mathknowledge.app.domain.model.CropRegion
import com.mathknowledge.app.domain.repository.PhotoSolveRepository
import javax.inject.Inject

class CapturePhotoUseCase @Inject constructor(
    private val repository: PhotoSolveRepository
) {
    /**
     * 保存拍照结果
     */
    suspend operator fun invoke(uri: Uri): Result<String> {
        return repository.savePhoto(uri)
    }

    /**
     * 裁剪图片
     */
    suspend fun cropImage(photoUri: String, cropRegion: CropRegion): Result<ByteArray> {
        return repository.cropImage(photoUri, cropRegion)
    }
}
```

```kotlin
// domain/usecase/RecognizeMathUseCase.kt
package com.mathknowledge.app.domain.usecase

import com.mathknowledge.app.domain.model.RecognitionResult
import com.mathknowledge.app.domain.repository.PhotoSolveRepository
import javax.inject.Inject

class RecognizeMathUseCase @Inject constructor(
    private val repository: PhotoSolveRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): Result<RecognitionResult> {
        return repository.recognizeMathProblem(imageBytes)
    }
}
```

```kotlin
// domain/usecase/SaveFavoriteUseCase.kt
package com.mathknowledge.app.domain.usecase

import com.mathknowledge.app.domain.model.RecognitionResult
import com.mathknowledge.app.domain.repository.PhotoSolveRepository
import javax.inject.Inject

class SaveFavoriteUseCase @Inject constructor(
    private val repository: PhotoSolveRepository
) {
    suspend fun addFavorite(result: RecognitionResult): Result<Unit> {
        return repository.saveFavorite(result)
    }

    suspend fun removeFavorite(problemId: String): Result<Unit> {
        return repository.removeFavorite(problemId)
    }

    suspend fun isFavorite(problemId: String): Result<Boolean> {
        return repository.isFavorite(problemId)
    }
}
```

---

## 4. Data Layer

```kotlin
// data/remote/AiRecognitionApi.kt
package com.mathknowledge.app.data.remote

import com.mathknowledge.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI识别API — 对接后端或第三方OCR+数学识别服务
 */
@Singleton
class AiRecognitionApi @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // TODO: 替换为实际API地址
    private val baseUrl = "https://api.mathknowledge.com/v1"

    /**
     * 上传图片进行数学题目识别
     */
    suspend fun recognize(imageBytes: ByteArray): RecognitionResult =
        withContext(Dispatchers.IO) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "math_problem.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/recognize")
                .post(requestBody)
                .build()

            // 模拟API响应（实际项目中替换为真实网络请求）
            // val response = client.newCall(request).execute()
            // return parseResponse(response.body?.string() ?: "")

            // ===== 模拟数据（开发阶段） =====
            delay(2000) // 模拟网络延迟
            return@withContext mockRecognitionResult()
        }

    private fun mockRecognitionResult(): RecognitionResult {
        val problem = MathProblem(
            id = UUID.randomUUID().toString(),
            rawText = "求解方程: x² + 5x + 6 = 0",
            latexFormula = "x^2 + 5x + 6 = 0"
        )

        val solution = Solution(
            steps = listOf(
                SolutionStep(
                    stepNumber = 1,
                    description = "使用因式分解法",
                    formulaLatex = "x^2 + 5x + 6 = (x + 2)(x + 3)",
                    explanation = "将二次三项式分解为两个一次因式的乘积"
                ),
                SolutionStep(
                    stepNumber = 2,
                    description = "令每个因式等于零",
                    formulaLatex = "x + 2 = 0 \\quad \\text{或} \\quad x + 3 = 0",
                    explanation = "根据零乘积性质，若乘积为零则至少有一个因式为零"
                ),
                SolutionStep(
                    stepNumber = 3,
                    description = "求解",
                    formulaLatex = "x_1 = -2, \\quad x_2 = -3",
                    explanation = "分别解两个一次方程得到两个根"
                )
            ),
            finalAnswer = "x = -2 或 x = -3",
            finalAnswerLatex = "x_1 = -2, \\quad x_2 = -3",
            explanation = "这是一元二次方程，通过因式分解法求解。方程 x² + 5x + 6 = 0 可以分解为 (x+2)(x+3) = 0，因此 x = -2 或 x = -3。",
            relatedKnowledge = listOf("一元二次方程", "因式分解", "零乘积性质")
        )

        return RecognitionResult(
            problem = problem,
            solution = solution,
            confidence = 0.95f
        )
    }

    private fun parseResponse(json: String): RecognitionResult {
        val obj = JSONObject(json)
        val problemObj = obj.getJSONObject("problem")
        val solutionObj = obj.getJSONObject("solution")

        val stepsArray = solutionObj.getJSONArray("steps")
        val steps = (0 until stepsArray.length()).map { i ->
            val step = stepsArray.getJSONObject(i)
            SolutionStep(
                stepNumber = step.getInt("stepNumber"),
                description = step.getString("description"),
                formulaLatex = step.getString("formulaLatex"),
                explanation = step.getString("explanation")
            )
        }

        val relatedArray = solutionObj.optJSONArray("relatedKnowledge") ?: JSONArray()
        val related = (0 until relatedArray.length()).map { relatedArray.getString(it) }

        return RecognitionResult(
            problem = MathProblem(
                id = problemObj.getString("id"),
                rawText = problemObj.getString("rawText"),
                latexFormula = problemObj.getString("latexFormula")
            ),
            solution = Solution(
                steps = steps,
                finalAnswer = solutionObj.getString("finalAnswer"),
                finalAnswerLatex = solutionObj.getString("finalAnswerLatex"),
                explanation = solutionObj.getString("explanation"),
                relatedKnowledge = related
            ),
            confidence = obj.getDouble("confidence").toFloat()
        )
    }
}
```



```kotlin
// data/local/FavoriteDao.kt
package com.mathknowledge.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 收藏题目数据库实体
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val rawText: String,
    val latexFormula: String,
    val solutionJson: String,
    val timestamp: Long
)

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :problemId")
    suspend fun deleteById(problemId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :problemId)")
    suspend fun isFavorite(problemId: String): Boolean

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<FavoriteEntity>>
}
```

```kotlin
// data/local/AppDatabase.kt
package com.mathknowledge.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
```

---

## 5. Data Layer — Repository 实现

```kotlin
// data/repository/PhotoSolveRepositoryImpl.kt
package com.mathknowledge.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.mathknowledge.app.data.local.FavoriteDao
import com.mathknowledge.app.data.local.FavoriteEntity
import com.mathknowledge.app.data.remote.AiRecognitionApi
import com.mathknowledge.app.domain.model.CropRegion
import com.mathknowledge.app.domain.model.RecognitionResult
import com.mathknowledge.app.domain.repository.PhotoSolveRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoSolveRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiApi: AiRecognitionApi,
    private val favoriteDao: FavoriteDao
) : PhotoSolveRepository {

    override suspend fun savePhoto(uri: Uri): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法读取图片")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val file = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            bitmap.recycle()
            file.absolutePath
        }
    }

    override suspend fun cropImage(
        photoUri: String,
        cropRegion: CropRegion
    ): Result<ByteArray> = runCatching {
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(photoUri)
                ?: throw IllegalStateException("无法解码图片")

            val rect = cropRegion.toRectF(bitmap.width, bitmap.height)
            val left = rect.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = rect.top.toInt().coerceIn(0, bitmap.height - 1)
            val width = (rect.width()).toInt().coerceIn(1, bitmap.width - left)
            val height = (rect.height()).toInt().coerceIn(1, bitmap.height - top)

            val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
            bitmap.recycle()

            val outputStream = ByteArrayOutputStream()
            cropped.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            cropped.recycle()
            outputStream.toByteArray()
        }
    }

    override suspend fun recognizeMathProblem(
        imageBytes: ByteArray
    ): Result<RecognitionResult> = runCatching {
        aiApi.recognize(imageBytes)
    }

    override suspend fun saveFavorite(result: RecognitionResult): Result<Unit> = runCatching {
        val stepsJson = JSONArray().apply {
            result.solution.steps.forEach { step ->
                put(JSONObject().apply {
                    put("stepNumber", step.stepNumber)
                    put("description", step.description)
                    put("formulaLatex", step.formulaLatex)
                    put("explanation", step.explanation)
                })
            }
        }
        val solutionJson = JSONObject().apply {
            put("steps", stepsJson)
            put("finalAnswer", result.solution.finalAnswer)
            put("finalAnswerLatex", result.solution.finalAnswerLatex)
            put("explanation", result.solution.explanation)
            put("relatedKnowledge", JSONArray(result.solution.relatedKnowledge))
        }.toString()

        favoriteDao.insert(
            FavoriteEntity(
                id = result.problem.id,
                rawText = result.problem.rawText,
                latexFormula = result.problem.latexFormula,
                solutionJson = solutionJson,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeFavorite(problemId: String): Result<Unit> = runCatching {
        favoriteDao.deleteById(problemId)
    }

    override suspend fun isFavorite(problemId: String): Result<Boolean> = runCatching {
        favoriteDao.isFavorite(problemId)
    }

    override fun getHistory(): Flow<List<RecognitionResult>> {
        return favoriteDao.getRecentHistory().map { entities ->
            entities.map { entity ->
                val solutionObj = JSONObject(entity.solutionJson)
                val stepsArray = solutionObj.getJSONArray("steps")
                val steps = (0 until stepsArray.length()).map { i ->
                    val s = stepsArray.getJSONObject(i)
                    com.mathknowledge.app.domain.model.SolutionStep(
                        stepNumber = s.getInt("stepNumber"),
                        description = s.getString("description"),
                        formulaLatex = s.getString("formulaLatex"),
                        explanation = s.getString("explanation")
                    )
                }
                val relatedArray = solutionObj.optJSONArray("relatedKnowledge") ?: JSONArray()
                val related = (0 until relatedArray.length()).map { relatedArray.getString(it) }

                RecognitionResult(
                    problem = com.mathknowledge.app.domain.model.MathProblem(
                        id = entity.id,
                        rawText = entity.rawText,
                        latexFormula = entity.latexFormula
                    ),
                    solution = com.mathknowledge.app.domain.model.Solution(
                        steps = steps,
                        finalAnswer = solutionObj.getString("finalAnswer"),
                        finalAnswerLatex = solutionObj.getString("finalAnswerLatex"),
                        explanation = solutionObj.getString("explanation"),
                        relatedKnowledge = related
                    ),
                    confidence = 1.0f
                )
            }
        }
    }
}
```

---

## 6. Presentation Layer — ViewModel

```kotlin
// presentation/photosolve/PhotoSolveViewModel.kt
package com.mathknowledge.app.presentation.photosolve

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathknowledge.app.domain.model.CropRegion
import com.mathknowledge.app.domain.model.PhotoSolveState
import com.mathknowledge.app.domain.model.RecognitionResult
import com.mathknowledge.app.domain.usecase.CapturePhotoUseCase
import com.mathknowledge.app.domain.usecase.RecognizeMathUseCase
import com.mathknowledge.app.domain.usecase.SaveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoSolveViewModel @Inject constructor(
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val recognizeMathUseCase: RecognizeMathUseCase,
    private val saveFavoriteUseCase: SaveFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoSolveState())
    val uiState: StateFlow<PhotoSolveState> = _uiState.asStateFlow()

    // ===== 拍照相关 =====

    fun onPhotoCaptured(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            capturePhotoUseCase(uri)
                .onSuccess { localPath ->
                    _uiState.update {
                        it.copy(
                            photoUri = localPath,
                            currentStep = PhotoSolveState.SolveStep.CROP,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "拍照保存失败: ${e.message}"
                        )
                    }
                }
        }
    }

    fun toggleFlash() {
        _uiState.update { it.copy(isFlashEnabled = !it.isFlashEnabled) }
    }

    // ===== 框选相关 =====

    fun onCropRegionConfirmed(cropRegion: CropRegion) {
        val photoUri = _uiState.value.photoUri ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    cropRegion = cropRegion,
                    currentStep = PhotoSolveState.SolveStep.RECOGNIZING,
                    isLoading = true
                )
            }

            // 裁剪图片
            capturePhotoUseCase.cropImage(photoUri, cropRegion)
                .onSuccess { imageBytes ->
                    _uiState.update { it.copy(croppedBitmap = imageBytes) }
                    // 调用AI识别
                    recognizeMathUseCase(imageBytes)
                        .onSuccess { result ->
                            _uiState.update {
                                it.copy(
                                    recognitionResult = result,
                                    currentStep = PhotoSolveState.SolveStep.RESULT,
                                    isLoading = false
                                )
                            }
                            // 检查收藏状态
                            checkFavoriteStatus(result.problem.id)
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentStep = PhotoSolveState.SolveStep.CROP,
                                    errorMessage = "识别失败: ${e.message}"
                                )
                            }
                        }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStep = PhotoSolveState.SolveStep.CROP,
                            errorMessage = "裁剪失败: ${e.message}"
                        )
                    }
                }
        }
    }

    // ===== 收藏相关 =====

    fun toggleFavorite() {
        val result = _uiState.value.recognitionResult ?: return
        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                saveFavoriteUseCase.removeFavorite(result.problem.id)
                    .onSuccess {
                        _uiState.update { it.copy(isFavorite = false) }
                    }
            } else {
                saveFavoriteUseCase.addFavorite(result)
                    .onSuccess {
                        _uiState.update { it.copy(isFavorite = true) }
                    }
            }
        }
    }

    private suspend fun checkFavoriteStatus(problemId: String) {
        saveFavoriteUseCase.isFavorite(problemId)
            .onSuccess { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
    }

    // ===== 导航相关 =====

    fun goBack() {
        _uiState.update { state ->
            when (state.currentStep) {
                PhotoSolveState.SolveStep.CROP -> state.copy(
                    currentStep = PhotoSolveState.SolveStep.CAMERA,
                    photoUri = null,
                    cropRegion = null
                )
                PhotoSolveState.SolveStep.RESULT -> state.copy(
                    currentStep = PhotoSolveState.SolveStep.CROP,
                    recognitionResult = null
                )
                else -> state
            }
        }
    }

    fun resetToCamera() {
        _uiState.update {
            PhotoSolveState()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
```

---

## 7. Presentation Layer — CameraX 拍照页面

```kotlin
// presentation/photosolve/CameraPreviewScreen.kt
package com.mathknowledge.app.presentation.photosolve

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import