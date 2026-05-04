# 集成测试模块代码

## 1. 测试框架配置

### build.gradle.kts (app模块)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
}

android {
    // ... 其他配置
    
    defaultConfig {
        // ... 其他配置
        
        testInstrumentationRunner = "com.mathknowledge.app.HiltTestRunner"
        
        // 测试配置
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }
    
    buildTypes {
        // ... 其他配置
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptTest("com.google.dagger:hilt-android-compiler:2.48")
    
    // Android测试依赖
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.3")
    
    // Room测试
    testImplementation("androidx.room:room-testing:2.6.0")
    
    // Retrofit测试
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    testImplementation("com.squareup.retrofit2:retrofit-mock:2.9.0")
    
    // Turbine测试
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    // 其他依赖
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // 其他
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
}
```

### HiltTestRunner.kt
```kotlin
package com.mathknowledge.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
```

### TestModule.kt
```kotlin
package com.mathknowledge.app.di

import android.content.Context
import androidx.room.Room
import com.mathknowledge.app.data.local.AppDatabase
import com.mathknowledge.app.data.local.dao.KnowledgeDao
import com.mathknowledge.app.data.local.dao.PracticeDao
import com.mathknowledge.app.data.local.dao.FavoriteDao
import com.mathknowledge.app.data.remote.ApiService
import com.mathknowledge.app.data.repository.KnowledgeRepository
import com.mathknowledge.app.data.repository.PracticeRepository
import com.mathknowledge.app.data.repository.FavoriteRepository
import com.mathknowledge.app.data.repository.PhotoSolveRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.mathknowledge.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "test_math_knowledge.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideKnowledgeDao(database: AppDatabase): KnowledgeDao {
        return database.knowledgeDao()
    }
    
    @Provides
    @Singleton
    fun providePracticeDao(database: AppDatabase): PracticeDao {
        return database.practiceDao()
    }
    
    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }
    
    @Provides
    @Singleton
    fun provideKnowledgeRepository(
        apiService: ApiService,
        knowledgeDao: KnowledgeDao
    ): KnowledgeRepository {
        return KnowledgeRepository(apiService, knowledgeDao)
    }
    
    @Provides
    @Singleton
    fun providePracticeRepository(
        apiService: ApiService,
        practiceDao: PracticeDao
    ): PracticeRepository {
        return PracticeRepository(apiService, practiceDao)
    }
    
    @Provides
    @Singleton
    fun provideFavoriteRepository(
        favoriteDao: FavoriteDao
    ): FavoriteRepository {
        return FavoriteRepository(favoriteDao)
    }
    
    @Provides
    @Singleton
    fun providePhotoSolveRepository(
        apiService: ApiService
    ): PhotoSolveRepository {
        return PhotoSolveRepository(apiService)
    }
}
```

## 2. Repository测试

### KnowledgeRepositoryTest.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.dao.KnowledgeDao
import com.mathknowledge.app.data.remote.ApiService
import com.mathknowledge.app.model.Knowledge
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import retrofit2.Response

class KnowledgeRepositoryTest {
    
    @Mock
    private lateinit var apiService: ApiService
    
    @Mock
    private lateinit var knowledgeDao: KnowledgeDao
    
    private lateinit var repository: KnowledgeRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = KnowledgeRepository(apiService, knowledgeDao)
    }
    
    @Test
    fun `getKnowledgeList returns success from API`() = runTest {
        // Given
        val knowledgeList = listOf(
            Knowledge(id = "1", title = "代数", description = "代数基础"),
            Knowledge(id = "2", title = "几何", description = "几何基础")
        )
        whenever(apiService.getKnowledgeList()).thenReturn(Response.success(knowledgeList))
        
        // When
        val result = repository.getKnowledgeList()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(knowledgeList, result.getOrNull())
        verify(knowledgeDao).insertAll(knowledgeList)
    }
    
    @Test
    fun `getKnowledgeList returns failure from API`() = runTest {
        // Given
        whenever(apiService.getKnowledgeList()).thenReturn(
            Response.error(500, "Server Error")
        )
        
        // When
        val result = repository.getKnowledgeList()
        
        // Then
        assertTrue(result.isFailure)
        verify(knowledgeDao, never()).insertAll(any())
    }
    
    @Test
    fun `getKnowledgeById returns knowledge from database`() = runTest {
        // Given
        val knowledge = Knowledge(id = "1", title = "代数", description = "代数基础")
        whenever(knowledgeDao.getKnowledgeById("1")).thenReturn(knowledge)
        
        // When
        val result = repository.getKnowledgeById("1")
        
        // Then
        assertEquals(knowledge, result)
        verify(apiService, never()).getKnowledgeById(any())
    }
    
    @Test
    fun `getKnowledgeById fetches from API when not in database`() = runTest {
        // Given
        val knowledge = Knowledge(id = "1", title = "代数", description = "代数基础")
        whenever(knowledgeDao.getKnowledgeById("1")).thenReturn(null)
        whenever(apiService.getKnowledgeById("1")).thenReturn(Response.success(knowledge))
        
        // When
        val result = repository.getKnowledgeById("1")
        
        // Then
        assertEquals(knowledge, result)
        verify(knowledgeDao).insert(knowledge)
    }
    
    @Test
    fun `searchKnowledge returns filtered results`() = runTest {
        // Given
        val query = "代数"
        val knowledgeList = listOf(
            Knowledge(id = "1", title = "代数基础", description = "代数入门"),
            Knowledge(id = "2", title = "几何基础", description = "几何入门")
        )
        whenever(knowledgeDao.searchKnowledge("%$query%")).thenReturn(
            listOf(knowledgeList[0])
        )
        
        // When
        val result = repository.searchKnowledge(query)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("代数基础", result[0].title)
    }
    
    @Test
    fun `getKnowledgeByCategory returns filtered results`() = runTest {
        // Given
        val category = "代数"
        val knowledgeList = listOf(
            Knowledge(id = "1", title = "代数基础", category = "代数"),
            Knowledge(id = "2", title = "几何基础", category = "几何")
        )
        whenever(knowledgeDao.getKnowledgeByCategory(category)).thenReturn(
            listOf(knowledgeList[0])
        )
        
        // When
        val result = repository.getKnowledgeByCategory(category)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("代数", result[0].category)
    }
}
```

### PracticeRepositoryTest.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.dao.PracticeDao
import com.mathknowledge.app.data.remote.ApiService
import com.mathknowledge.app.model.Practice
import com.mathknowledge.app.model.PracticeResult
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import retrofit2.Response

class PracticeRepositoryTest {
    
    @Mock
    private lateinit var apiService: ApiService
    
    @Mock
    private lateinit var practiceDao: PracticeDao
    
    private lateinit var repository: PracticeRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = PracticeRepository(apiService, practiceDao)
    }
    
    @Test
    fun `getPracticeList returns success from API`() = runTest {
        // Given
        val practiceList = listOf(
            Practice(id = "1", question = "1+1=?", answer = "2"),
            Practice(id = "2", question = "2+2=?", answer = "4")
        )
        whenever(apiService.getPracticeList()).thenReturn(Response.success(practiceList))
        
        // When
        val result = repository.getPracticeList()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(practiceList, result.getOrNull())
        verify(practiceDao).insertAll(practiceList)
    }
    
    @Test
    fun `submitPracticeAnswer returns success`() = runTest {
        // Given
        val practiceId = "1"
        val answer = "2"
        val expectedResult = PracticeResult(
            practiceId = practiceId,
            isCorrect = true,
            explanation = "1+1=2"
        )
        whenever(apiService.submitPracticeAnswer(practiceId, answer)).thenReturn(
            Response.success(expectedResult)
        )
        
        // When
        val result = repository.submitPracticeAnswer(practiceId, answer)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
        verify(practiceDao).updatePracticeResult(expectedResult)
    }
    
    @Test
    fun `getPracticeHistory returns history from database`() = runTest {
        // Given
        val history = listOf(
            PracticeResult(practiceId = "1", isCorrect = true, explanation = "1+1=2"),
            PracticeResult(practiceId = "2", isCorrect = false, explanation = "2+2=5")
        )
        whenever(practiceDao.getPracticeHistory()).thenReturn(history)
        
        // When
        val result = repository.getPracticeHistory()
        
        // Then
        assertEquals(history, result)
    }
    
    @Test
    fun `getPracticeStatistics returns statistics`() = runTest {
        // Given
        val statistics = mapOf(
            "total" to 10,
            "correct" to 8,
            "incorrect" to 2
        )
        whenever(practiceDao.getPracticeStatistics()).thenReturn(statistics)
        
        // When
        val result = repository.getPracticeStatistics()
        
        // Then
        assertEquals(statistics, result)
    }
    
    @Test
    fun `getPracticeByDifficulty returns filtered results`() = runTest {
        // Given
        val difficulty = "easy"
        val practiceList = listOf(
            Practice(id = "1", question = "1+1=?", difficulty = "easy"),
            Practice(id = "2", question = "2+2=?", difficulty = "medium")
        )
        whenever(practiceDao.getPracticeByDifficulty(difficulty)).thenReturn(
            listOf(practiceList[0])
        )
        
        // When
        val result = repository.getPracticeByDifficulty(difficulty)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("easy", result[0].difficulty)
    }
}
```

### FavoriteRepositoryTest.kt
```kotlin
package com.mathknowledge.app.data.repository

import com.mathknowledge.app.data.local.dao.FavoriteDao
import com.mathknowledge.app.model.Favorite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class FavoriteRepositoryTest {
    
    @Mock
    private lateinit var favoriteDao: FavoriteDao
    
    private lateinit var repository: FavoriteRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = FavoriteRepository(favoriteDao)
    }
    
    @Test
    fun `addFavorite adds favorite to database`() = runTest {
        // Given
        val favorite = Favorite(
            id = "1",
            itemId = "knowledge_1",
            itemType = "knowledge",
            title = "代数基础"
        )
        
        // When
        repository.addFavorite(favorite)
        
        // Then
        verify(favoriteDao).insert(favorite)
    }
    
    @Test
    fun `removeFavorite removes favorite from database`() = runTest {
        // Given
        val favoriteId = "1"
        
        // When
        repository.removeFavorite(favoriteId)
        
        // Then
        verify(favoriteDao).deleteById(favoriteId)
    }
    
    @Test
    fun `isFavorite returns true when item is favorite`() = runTest {
        // Given
        val itemId = "knowledge_1"
        whenever(favoriteDao.isFavorite(itemId)).thenReturn(true)
        
        // When
        val result = repository.isFavorite(itemId)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isFavorite returns false when item is not favorite`() = runTest {
        // Given
        val itemId = "knowledge_1"
        whenever(favoriteDao.isFavorite(itemId)).thenReturn(false)
        
        // When
        val result = repository.isFavorite(itemId)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `getFavorites returns list of favorites`() = runTest {
        // Given
        val favorites = listOf(
            Favorite(id = "1", itemId = "knowledge_1", itemType = "knowledge", title = "代数基础"),
            Favorite(id = "2", itemId = "practice_1", itemType = "practice", title = "1+1=?")
        )
        whenever(favoriteDao.getFavorites()).thenReturn(favorites)
        
        // When
        val result = repository.getFavorites()
        
        // Then
        assertEquals(favorites, result)
    }
    
    @Test
    fun `getFavoritesByType returns filtered favorites`() = runTest {
        // Given
        val type = "knowledge"
        val favorites = listOf(
            Favorite(id = "1", itemId = "knowledge_1", itemType = "knowledge", title = "代数基础"),
            Favorite(id = "2", itemId = "practice_1", itemType = "practice", title = "1+1=?")
        )
        whenever(favoriteDao.getFavoritesByType(type)).thenReturn(
            listOf(favorites[0])
        )
        
        // When
        val result = repository.getFavoritesByType(type)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("knowledge", result[0].itemType)
    }
    
    @Test
    fun `toggleFavorite adds favorite when not exists`() = runTest {
        // Given
        val itemId = "knowledge_1"
        whenever(favoriteDao.isFavorite(itemId)).thenReturn(false)
        
        // When
        repository.toggleFavorite(itemId, "knowledge", "代数基础")
        
        // Then
        verify(favoriteDao).insert(any())
    }
    
    @Test
    fun `toggleFavorite removes favorite when exists`() = runTest {
        // Given
        val itemId = "knowledge_1"
        val favoriteId = "1"
        whenever(favoriteDao.isFavorite(itemId)).thenReturn(true)
        whenever(favoriteDao.getFavoriteByItemId(itemId)).thenReturn(
            Favorite(id = favoriteId, itemId = itemId, itemType = "knowledge", title = "代数基础")
        )
        
        // When
        repository.toggleFavorite(itemId, "knowledge", "代数基础")
        
        // Then
        verify(favoriteDao).deleteById(favoriteId)
    }
}
```

## 3. ViewModel测试

### KnowledgeViewModelTest.kt
```kotlin
package com.mathknowledge.app.ui.viewmodel

import com.mathknowledge.app.data.repository.KnowledgeRepository
import com.mathknowledge.app.model.Knowledge
import com.mathknowledge.app.ui.state.KnowledgeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeViewModelTest {
    
    @Mock
    private lateinit var knowledgeRepository: KnowledgeRepository
    
    private lateinit var viewModel: KnowledgeViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = KnowledgeViewModel(knowledgeRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadKnowledgeList success updates state to Success`() = runTest {
        // Given
        val knowledgeList = listOf(
            Knowledge(id = "1", title = "代数", description = "代数基础"),
            Knowledge(id = "2", title = "几何", description = "几何基础")
        )
        whenever(knowledgeRepository.getKnowledgeList()).thenReturn(
            Result.success(knowledgeList)
        )
        
        // When
        viewModel.loadKnowledgeList()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is KnowledgeUiState.Success)
        assertEquals(knowledgeList, (state as KnowledgeUiState.Success).knowledgeList)
    }
    
    @Test
    fun `loadKnowledgeList failure updates state to Error`() = runTest {
        // Given
        whenever(knowledgeRepository.getKnowledgeList()).thenReturn(
            Result.failure(Exception("Network error"))
        )
        
        // When
        viewModel.loadKnowledgeList()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is KnowledgeUiState.Error)
        assertEquals("Network error", (state as KnowledgeUiState.Error).message)
    }
    
    @Test
    fun `searchKnowledge updates state with filtered results`() = runTest {
        // Given
        val query = "代数"
        val filteredList = listOf(
            Knowledge(id = "1", title = "代数基础", description = "代数入门")
        )
        whenever(knowledgeRepository.searchKnowledge(query)).thenReturn(filteredList)
        
        // When
        viewModel.searchKnowledge(query)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is KnowledgeUiState.Success)
        assertEquals(filteredList, (state as KnowledgeUiState.Success).knowledgeList)
    }
    
    @Test
    fun `getKnowledgeById updates state with knowledge details`() = runTest {
        // Given
        val knowledgeId = "1"
        val knowledge = Knowledge(id = knowledgeId, title = "代数", description = "代数基础")
        whenever(knowledgeRepository.getKnowledgeById(knowledgeId)).thenReturn(knowledge)
        
        // When
        viewModel.getKnowledgeById(knowledgeId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is KnowledgeUiState.Success)
        assertEquals(knowledge, (state as KnowledgeUiState.Success).selectedKnowledge)
    }
    
    @Test
    fun `filterByCategory updates state with category filtered results`() = runTest {
        // Given
        val category = "代数"
        val filteredList = listOf(
            Knowledge(id = "1", title = "代数基础", category = "代数")
        )
        whenever(knowledgeRepository.getKnowledgeByCategory(category)).thenReturn(filteredList)
        
        // When
        viewModel.filterByCategory(category)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is KnowledgeUiState.Success)
        assertEquals(filteredList, (state as KnowledgeUiState.Success).knowledgeList)
    }
    
    @Test
    fun `clearSearch clears search query and reloads data`() = runTest {
        // Given
        val knowledgeList = listOf(
            Knowledge(id = "1", title = "代数", description = "代数基础")
        )
        whenever(knowledgeRepository.getKnowledgeList()).thenReturn(
            Result.success(knowledgeList)
        )
        
        // When
        viewModel.clearSearch()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is KnowledgeUiState.Success)
        assertEquals(knowledgeList, (state as KnowledgeUiState.Success).knowledgeList)
    }
}
```

### PracticeViewModelTest.kt
```kotlin
package com.mathknowledge.app.ui.viewmodel

import com.mathknowledge.app.data.repository.PracticeRepository
import com.mathknowledge.app.model.Practice
import com.math