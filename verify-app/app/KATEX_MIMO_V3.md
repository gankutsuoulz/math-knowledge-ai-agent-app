# 使用 WebView 加载本地 KaTeX 渲染公式

## 项目结构

```
app/
├── src/main/
│   ├── assets/
│   │   └── katex/
│   │       ├── katex.min.css
│   │       ├── katex.min.js
│   │       └── fonts/          ← KaTeX 字体文件夹
│   ├── java/.../MainActivity.kt
│   └── AndroidManifest.xml
```

> **获取 KaTeX 文件**：从 https://github.com/KaTeX/KaTeX/releases 下载发布包，将 `katex.min.js`、`katex.min.css` 和 `fonts/` 文件夹放入 `assets/katex/` 目录。

---

## MainActivity.kt（完整代码）

```kotlin
package com.example.katexdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // ============================================================
    //  要渲染的公式列表（可自行增删改）
    // ============================================================
    private val formulas = listOf(
        // 行内公式
        "E = mc^2",
        "a^2 + b^2 = c^2",
        "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
        // 求和
        "\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}",
        // 积分
        "\\int_{0}^{\\infty} e^{-x^2} \\, dx = \\frac{\\sqrt{\\pi}}{2}",
        // 矩阵
        "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",
        // 极限
        "\\lim_{n \\to \\infty} \\left(1 + \\frac{1}{n}\\right)^n = e",
        // 多行公式
        """
        \\left\\{
        \\begin{array}{l}
        x + y = 5 \\\\
        2x - y = 1
        \\end{array}
        \\right.
        """.trimIndent(),
        // 希腊字母与特殊符号
        "\\alpha + \\beta = \\gamma, \\quad \\forall x \\in \\mathbb{R}",
        // 傅里叶变换
        "\\hat{f}(\\xi) = \\int_{-\\infty}^{\\infty} f(x)\\, e^{-2\\pi i x \\xi}\\, dx"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 以纯 WebView 作为页面根视图
        webView = WebView(this)
        setContentView(webView)

        configureWebView(webView)
        webView.loadDataWithBaseURL(
            "file:///android_asset/katex/",
            buildHtml(formulas),
            "text/html",
            "UTF-8",
            null
        )
    }

    // ============================================================
    //  WebView 基本配置
    // ============================================================
    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            defaultTextEncodingName = "UTF-8"
            // 让内容自适应屏幕宽度
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        // 阻止跳转外部浏览器
        wv.webViewClient = WebViewClient()
    }

    // ============================================================
    //  构建包含 KaTeX 的完整 HTML 页面
    // ============================================================
    private fun buildHtml(formulas: List<String>): String {
        // 将每个公式包装为 KaTeX 渲染调用
        val formulaBlocks = formulas.joinToString("\n") { tex ->
            // 对 LaTeX 字符串做转义，防止 JS 注入 / 引号冲突
            val escaped = tex
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .trim()

            """
            <div class="formula-card">
                <div class="katex-render" id=""></div>
                <pre class="source">$tex</pre>
            </div>
            """.trimIndent()
        }

        // 用 JS 批量渲染（更可靠，避免 HTML 内联转义问题）
        val renderScript = formulas.mapIndexed { index, tex ->
            val escaped = tex
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .trim()
            """
            try {
                katex.render("$escaped", document.querySelectorAll('.katex-render')[$index], {
                    throwOnError: false,
                    displayMode: true
                });
            } catch(e) {
                document.querySelectorAll('.katex-render')[$index].innerHTML =
                    '<span style="color:red;">' + e.message + '</span>';
            }
            """.trimIndent()
        }.joinToString("\n")

        return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8"/>
            <meta name="viewport"
                  content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes"/>
            <!-- 加载本地 KaTeX CSS -->
            <link rel="stylesheet" href="katex.min.css"/>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }

                body {
                    font-family: -apple-system, "Helvetica Neue", "PingFang SC",
                                 "Microsoft YaHei", sans-serif;
                    background: #f5f7fa;
                    padding: 16px;
                    color: #333;
                }

                h1 {
                    text-align: center;
                    font-size: 22px;
                    margin-bottom: 20px;
                    color: #1a1a2e;
                }

                .formula-card {
                    background: #fff;
                    border-radius: 12px;
                    box-shadow: 0 2px 12px rgba(0,0,0,0.08);
                    padding: 20px 16px;
                    margin-bottom: 16px;
                    overflow-x: auto;
                }

                .katex-render {
                    text-align: center;
                    font-size: 1.3em;
                    padding: 8px 0;
                }

                .katex-display {
                    margin: 0 !important;
                }

                .source {
                    margin-top: 12px;
                    padding: 10px 12px;
                    background: #f0f2f5;
                    border-radius: 8px;
                    font-size: 12px;
                    color: #666;
                    white-space: pre-wrap;
                    word-break: break-all;
                    font-family: "SF Mono", "Fira Code", "Consolas", monospace;
                }
            </style>
        </head>
        <body>
            <h1>📐 KaTeX 公式渲染示例</h1>

            $formulaBlocks

            <!-- 加载本地 KaTeX JS -->
            <script src="katex.min.js"></script>
            <script>
                document.addEventListener('DOMContentLoaded', function() {
                    $renderScript
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // ============================================================
    //  处理返回键（WebView 页面回退）
    // ============================================================
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
```

---

## AndroidManifest.xml（关键权限）

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.katexdemo">

    <!-- 如果只加载本地资源，网络权限不是必须的 -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="KaTeX Demo"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## build.gradle（app 级别，关键依赖）

```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.webkit:webkit:1.8.0'   // 可选，增强 WebView 兼容性
}
```

---

## assets 目录准备步骤

```bash
# 1. 从 GitHub 下载 KaTeX 发布包
wget https://github.com/KaTeX/KaTeX/releases/download/v0.16.9/katex.tar.gz

# 2. 解压
tar -xzf katex.tar.gz

# 3. 复制到 Android 项目 assets 目录
mkdir -p app/src/main/assets/katex
cp katex/katex.min.js   app/src/main/assets/katex/
cp katex/katex.min.css  app/src/main/assets/katex/
cp -r katex/fonts       app/src/main/assets/katex/
```

最终 `assets/katex/` 目录结构：

```
assets/katex/
├── katex.min.css
├── katex.min.js
└── fonts/
    ├── KaTeX_Main-Bold.woff2
    ├── KaTeX_Main-Italic.woff2
    ├── KaTeX_Math-BoldItalic.woff2
    ├── KaTeX_Size1-Regular.woff2
    └── ... (其他字体文件)
```

---

## 运行效果示意

```
┌──────────────────────────────┐
│     📐 KaTeX 公式渲染示例      │
├──────────────────────────────┤
│  ┌────────────────────────┐  │
│  │      E = mc²           │  │
│  │  ────────────────────  │  │
│  │  source: E = mc^2      │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │  -b ± √(b²-4ac)       │  │
│  │  ─────────────────     │  │
│  │       2a               │  │
│  │  source: \frac{-b...   │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │   n                    │  │
│  │   Σ  i = n(n+1)/2     │  │
│  │  i=1                   │  │
│  │  source: \sum_{i=1}... │  │
│  └────────────────────────┘  │
│           ...更多公式          │
└──────────────────────────────┘
```

---

## 核心要点总结

| 要点 | 说明 |
|------|------|
| **本地加载** | 通过 `file:///android_asset/katex/` 路径引用本地 JS/CSS |
| **`loadDataWithBaseURL`** | 设置 baseURL 为 assets 路径，使相对路径的 CSS/字体正确加载 |
| **字体文件** | KaTeX 的 `fonts/` 文件夹**必须**一并放入 assets，否则公式无法正常显示 |
| **JS 渲染** | 使用 `katex.render()` 在 DOMContentLoaded 后批量渲染，比内联更安全 |
| **displayMode** | 设为 `true` 为块级公式（居中独占一行），`false` 为行内公式 |
| **错误处理** | `throwOnError: false` 遇到语法错误时显示红色提示而非白屏 |