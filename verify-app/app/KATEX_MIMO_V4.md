```kotlin
package com.example.katexviewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KaTeXScreen()
                }
            }
        }
    }
}

class KaTeXBridge {
    var onRendered: ((String) -> Unit)? = null

    @JavascriptInterface
    fun onRenderSuccess(latex: String) {
        onRendered?.invoke("Rendered: $latex")
    }

    @JavascriptInterface
    fun onRenderError(latex: String, error: String) {
        onRendered?.invoke("Error rendering '$latex': $error")
    }
}

@Composable
fun KaTeXScreen() {
    val bridge = remember { KaTeXBridge() }
    var statusMessage by remember { mutableStateOf("Ready") }
    var latexInput by remember {
        mutableStateOf(
            """E = mc^2 + \frac{\hbar^2}{2m}\nabla^2\psi
\sum_{i=1}^{n} x_i = \int_0^\infty f(x)\,dx
\sqrt{a^2 + b^2} = c
A = \begin{pmatrix} a & b \\ c & d \end{pmatrix}
f(x) = \begin{cases} x^2 & \text{if } x \geq 0 \\ -x & \text{if } x < 0 \end{cases}"""
        )
    }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(bridge) {
        bridge.onRendered = { msg ->
            statusMessage = msg
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "KaTeX Renderer",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = latexInput,
            onValueChange = { latexInput = it },
            label = { Text("LaTeX Input") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                val escaped = latexInput
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\"", "\\\"")
                webViewInstance?.evaluateJavascript(
                    "renderLatex('$escaped')", null
                )
            }) {
                Text("Render")
            }

            Button(onClick = {
                webViewInstance?.evaluateJavascript("clearLatex()", null)
            }) {
                Text("Clear")
            }
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        KaTeXWebView(
            bridge = bridge,
            onWebViewCreated = { webViewInstance = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KaTeXWebView(
    bridge: KaTeXBridge,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)

                webViewClient = WebViewClient()

                addJavascriptInterface(bridge, "AndroidBridge")

                loadDataWithBaseURL(
                    "https://katex.org/",
                    katexHtml,
                    "text/html",
                    "UTF-8",
                    null
                )

                onWebViewCreated(this)
            }
        }
    )
}

val katexHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css">
    <script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            padding: 16px;
            background-color: #ffffff;
            color: #1a1a1a;
            line-height: 1.6;
        }
        #output {
            min-height: 100px;
            padding: 16px;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            background-color: #fafafa;
            overflow-x: auto;
        }
        #output .katex-display {
            margin: 12px 0;
            overflow-x: auto;
            overflow-y: hidden;
            padding: 4px 0;
        }
        #output .katex { font-size: 1.2em; }
        .error {
            color: #d32f2f;
            background: #ffebee;
            padding: 8px 12px;
            border-radius: 4px;
            font-family: monospace;
            font-size: 13px;
            margin: 8px 0;
        }
        .placeholder {
            color: #999;
            font-style: italic;
            text-align: center;
            padding: 40px 0;
        }
    </style>
</head>
<body>
    <div id="output">
        <div class="placeholder">Enter LaTeX and press Render</div>
    </div>

    <script>
        function renderLatex(latex) {
            var output = document.getElementById('output');
            output.innerHTML = '';

            try {
                var lines = latex.split('\n');
                var hasContent = false;

                for (var i = 0; i < lines.length; i++) {
                    var line = lines[i].trim();
                    if (line === '') continue;
                    hasContent = true;

                    var div = document.createElement('div');
                    div.className = 'katex-display';

                    try {
                        katex.render(line, div, {
                            displayMode: true,
                            throwOnError: true,
                            trust: true,
                            strict: false,
                            macros: {
                                "\\R": "\\mathbb{R}",
                                "\\N": "\\mathbb{N}",
                                "\\Z": "\\mathbb{Z}",
                                "\\C": "\\mathbb{C}",
                                "\\eps": "\\varepsilon"
                            }
                        });
                    } catch (e) {
                        div.className = 'error';
                        div.textContent = 'Error: ' + e.message;
                    }

                    output.appendChild(div);
                }

                if (!hasContent) {
                    output.innerHTML = '<div class="placeholder">Empty input</div>';
                }

                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.onRenderSuccess(latex);
                }
            } catch (e) {
                output.innerHTML = '<div class="error">' + e.message + '</div>';
                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.onRenderError(latex, e.message);
                }
            }
        }

        function clearLatex() {
            document.getElementById('output').innerHTML =
                '<div class="placeholder">Enter LaTeX and press Render</div>';
        }
    </script>
</body>
</html>
""".trimIndent()
```