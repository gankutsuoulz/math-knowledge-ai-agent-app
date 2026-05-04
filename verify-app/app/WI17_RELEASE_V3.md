# 修复后的 build.gradle.kts 配置

```gradle
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.mathapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 移除非法引用 buildType.name 的代码
        // buildConfigField "String", "BUILD_TYPE", ""${buildType.name}""  // 已移除
    }
    
    signingConfigs {
        create("release") {
            // 从环境变量或属性文件读取签名信息
            val storeFilePath = project.findProperty("STORE_FILE_PATH") as? String
            val storePassword = project.findProperty("STORE_PASSWORD") as? String
            val keyAlias = project.findProperty("KEY_ALIAS") as? String
            val keyPassword = project.findProperty("KEY_PASSWORD") as? String
            
            if (storeFilePath.isNullOrBlank() || 
                storePassword.isNullOrBlank() || 
                keyAlias.isNullOrBlank() || 
                keyPassword.isNullOrBlank()) {
                throw GradleException(
                    "Release signing configuration is incomplete. " +
                    "Please provide: STORE_FILE_PATH, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD"
                )
            }
            
            storeFile = file(storeFilePath)
            storePassword = storePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }
    
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    // 在 applicationVariants.configureEach 中安全地设置 BuildConfig 字段
    applicationVariants.configureEach {
        val variant = this
        
        // 为每个变体设置 BUILD_TYPE 字段
        val buildTypeField = when (variant.buildType.name) {
            "debug" -> "DEBUG"
            "release" -> "RELEASE"
            else -> variant.buildType.name.uppercase()
        }
        
        variant.buildConfigField("String", "BUILD_TYPE", "\"$buildTypeField\"")
        
        // 为每个变体设置其他自定义字段
        variant.buildConfigField(
            "String", 
            "VARIANT_NAME", 
            "\"${variant.name}\""
        )
        
        variant.buildConfigField(
            "String", 
            "BUILD_TIMESTAMP", 
            "\"${System.currentTimeMillis()}\""
        )
        
        // 根据变体设置不同的 API URL
        val apiUrl = when (variant.buildType.name) {
            "debug" -> "https://api-dev.example.com"
            "release" -> "https://api.example.com"
            else -> "https://api-staging.example.com"
        }
        
        variant.buildConfigField("String", "API_URL", "\"$apiUrl\"")
        
        // 为 release 变体设置额外的字段
        if (variant.buildType.name == "release") {
            variant.buildConfigField(
                "boolean", 
                "ENABLE_ANALYTICS", 
                "true"
            )
            variant.buildConfigField(
                "boolean", 
                "ENABLE_CRASH_REPORTING", 
                "true"
            )
        } else {
            variant.buildConfigField(
                "boolean", 
                "ENABLE_ANALYTICS", 
                "false"
            )
            variant.buildConfigField(
                "boolean", 
                "ENABLE_CRASH_REPORTING", 
                "false"
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

## 修复说明

1. **移除了非法引用**：删除了 `defaultConfig` 中对 `buildType.name` 的非法引用
2. **安全设置 BuildConfig 字段**：在 `applicationVariants.configureEach` 中为每个变体安全地设置 `BuildConfig` 字段
3. **签名信息验证**：在 `signingConfigs` 中添加了签名信息完整性检查，如果缺失关键信息会抛出 `GradleException`
4. **变体特定配置**：为不同的构建类型（debug/release）设置了不同的配置值
5. **保持原有功能**：保留了原有的构建配置，同时修复了问题

这种修复方式确保了：
- 在配置阶段不会引用尚未确定的构建类型
- 每个变体都能获得正确的 `BuildConfig` 字段值
- 签名配置在缺失时会给出明确的错误信息
- 代码更加健壮和可维护