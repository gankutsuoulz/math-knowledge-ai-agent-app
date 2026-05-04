# KaTeX代码修复方案

## 修复说明

### 问题2：本地化KaTeX资源
**修复方式：**
1. 将CDN链接替换为本地资源路径
2. 使用`file:///android_asset/`协议加载本地文件
3. 确保katex.min.css和katex.min.js已放置在assets目录中

### 问题3：权限配置过度
**修复方式：**
1. 将`allowFileAccess`设置为`false`
2. 将`allowContentAccess`设置为`false`
3. 保持其他必要的WebView设置不变

### 问题4：生命周期管理
**修复方式：**
1. 使用`DisposableEffect`管理WebView生命周期
2. 在`onDispose`中调用`webView.destroy()`
3. 确保WebView在组件销毁时被正确清理

## 修复后的完整代码

```kotlin
import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KatexWebView(
    latexExpression: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    
    // 创建WebView实例并记住
    val webView = remember {
        createWebView(context)
    }
    
    // 管理WebView生命周期
    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }
    
    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { view ->
            // 构建包含本地KaTeX资源的HTML
            val htmlContent = buildHtmlWithLocalKatex(latexExpression)
            view.loadDataWithBaseURL(
                null,
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

private fun createWebView(context: Context): WebView {
    return WebView(context).apply {
        // WebView设置
        settings.apply {
            javaScriptEnabled = true  // 保持JavaScript启用（问题1不修复）
            domStorageEnabled = true
            allowFileAccess = false   // 修复问题3：禁用文件访问
            allowContentAccess = false // 修复问题3：禁用内容访问
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
        }
        
        // WebView客户端设置
        webViewClient = WebViewClient()
    }
}

private fun buildHtmlWithLocalKatex(latexExpression: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <!-- 修复问题2：使用本地KaTeX CSS资源 -->
            <link rel="stylesheet" href="file:///android_asset/katex.min.css">
            <style>
                body {
                    margin: 0;
                    padding: 16px;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background-color: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 100vh;
                    box-sizing: border-box;
                }
                .katex-display {
                    margin: 0;
                    font-size: 1.5em;
                }
                .katex {
                    font-size: 1.2em;
                }
            </style>
        </head>
        <body>
            <div id="math-container">
                $$${latexExpression}$$
            </div>
            
            <!-- 修复问题2：使用本地KaTeX JS资源 -->
            <script src="file:///android_asset/katex.min.js"></script>
            <script>
                document.addEventListener('DOMContentLoaded', function() {
                    try {
                        // 渲染数学公式
                        katex.render(
                            "${latexExpression.replace("\"", "\\\"")}",
                            document.getElementById('math-container'),
                            {
                                throwOnError: false,
                                displayMode: true
                            }
                        );
                    } catch (e) {
                        console.error('KaTeX渲染错误:', e);
                        document.getElementById('math-container').innerHTML = 
                            '<div style="color: red; padding: 10px; border: 1px solid red; border-radius: 4px;">' +
                            '公式渲染错误: ' + e.message + '</div>';
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}

// 辅助函数：用于预览和测试
@Composable
fun KatexPreview() {
    val sampleLatex = "E = mc^2"
    
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        androidx.compose.material3.Text(
            text = "KaTeX渲染示例",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        
        androidx.compose.foundation.layout.Spacer(
            modifier = androidx.compose.ui.Modifier.height(16.dp)
        )
        
        KatexWebView(
            latexExpression = sampleLatex,
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color.White,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = androidx.compose.ui.graphics.Color.LightGray,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
        )
    }
}
```

## 资源文件准备说明

在修复问题2之前，需要确保以下文件已放置在项目的`assets`目录中：

1. **katex.min.css** - KaTeX的CSS样式文件
2. **katex.min.js** - KaTeX的JavaScript库文件

### 如何获取这些文件：

1. 访问KaTeX官方CDN：https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/
2. 下载以下文件：
   - `katex.min.css`
   - `katex.min.js`
3. 将下载的文件放置在项目的`app/src/main/assets/`目录中

### 目录结构示例：
```
app/
└── src/
    └── main/
        └── assets/
            ├── katex.min.css
            └── katex.min.js
```

## 修复验证

修复后的代码具有以下特点：

1. **本地资源加载**：使用`file:///android_asset/`协议加载本地KaTeX资源
2. **安全权限**：禁用了不必要的文件访问权限
3. **生命周期管理**：使用`DisposableEffect`确保WebView被正确销毁
4. **错误处理**：添加了KaTeX渲染错误的处理逻辑
5. **响应式设计**：WebView支持缩放和自适应布局

## 注意事项

1. 确保`katex.min.css`和`katex.min.js`文件版本兼容
2. 如果遇到跨域问题，可能需要调整WebView的安全设置
3. 对于复杂的LaTeX表达式，可能需要调整KaTeX的渲染配置
4. 在生产环境中，建议对用户输入的LaTeX表达式进行适当的清理和验证