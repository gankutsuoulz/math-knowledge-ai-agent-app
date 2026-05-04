```kotlin
package com.example.katexwebview

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KaTeXScreen()
        }
    }
}

class KaTeXViewModel : ViewModel() {
    sealed class RenderState {
        object Idle : RenderState()
        object Rendering : RenderState()
        data class Rendered(val html: String?) : RenderState()
        data class Error(val message: String?) : RenderState()
    }

    private val _renderState = MutableStateFlow<RenderState>(RenderState.Idle)
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()

    private val _isWebViewReady = MutableStateFlow(false)
    val isWebViewReady: StateFlow<Boolean> = _isWebViewReady.asStateFlow()

    fun onPageFinished() {
        _isWebViewReady.value = true
    }

    fun onRenderComplete(html: String?) {
        _renderState.value = if (html != null) {
            RenderState.Rendered(html)
        } else {
            RenderState.Error("Rendering failed")
        }
    }

    fun render(webView: WebView?, formula: String) {
        if (formula.isBlank()) return
        _renderState.value = RenderState.Rendering
        val escaped = JSONObject.quote(formula)
        webView?.evaluateJavascript("render($escaped);", null)
    }
}

class KaTeXInterface(private val onComplete: (String?) -> Unit) {
    @JavascriptInterface
    fun onRenderComplete(html: String?) {
        onComplete(html)
    }
}

@Composable
fun KaTeXScreen(viewModel: KaTeXViewModel = viewModel()) {
    var formula by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val renderState by viewModel.renderState.collectAsState()
    val isReady by viewModel.isWebViewReady.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = formula,
            onValueChange = { formula = it },
            label = { Text("Enter LaTeX") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.render(webView, formula) },
            enabled = isReady && renderState !is KaTeXViewModel.RenderState.Rendering
        ) {
            Text("Render")
        }
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    addJavascriptInterface(
                        KaTeXInterface { html -> viewModel.onRenderComplete(html) },
                        "Android"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            viewModel.onPageFinished()
                        }
                    }
                    loadUrl("file:///android_asset/katex.html")
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
```