请生成一个完整的 Android Kotlin 项目，项目名为 **KaTeX Local Verifier**，用于在 WebView 中离线渲染数学公式（KaTeX 完全本地化），并具备清晰的状态管理和完善的错误处理。项目针对之前代码评分 2/5 的问题进行修复：补齐 WebView 初始化、规范化作用域与状态、增加 WebView 安全配置、实现 KaTeX 资源全本地化。

## 技术栈与架构要求
- 语言：Kotlin
- 最低 SDK：24
- UI 框架：传统 XML 布局 + ViewBinding（不使用 Jetpack Compose）
- 架构组件：ViewModel + LiveData，协程作用域限定在 ViewModel 的 viewModelScope
- 构建工具：Gradle Kotlin DSL，但不用生成构建文件，只专注于源码与资源

## 必须输出的文件清单（缺一不可）
1. `app/src/main/java/com/example/katexlocal/MainActivity.kt`
2. `app/src/main/java/com/example/katexlocal/MainViewModel.kt`
3. `app/src/main/res/layout/activity_main.xml`
4. `app/src/main/assets/index.html`
5. `app/src/main/assets/katex/katex.min.css`（以注释形式占位，说明应从官方发行版复制至此）
6. `app/src/main/assets/katex/katex.min.js`（以注释形式占位，说明应从官方发行版复制至此）
7. `app/src/main/assets/katex/contrib/auto-render.min.js`（以注释形式占位，说明可选）
8. 一个简短的 `README.md`（仅说明文件放置与运行注意事项）

## 各文件的详细精确要求

### 1. MainActivity.kt
- 必须是完整的 Activity 类，继承自 `AppCompatActivity`，使用 `ActivityMainBinding` 进行视图绑定。
- `onCreate` 中完成：
  - 初始化 ViewBinding。
  - 通过 `ViewModelProvider` 获取 `MainViewModel` 实例。
  - 初始化 WebView（来自布局）并进行**所有必要的安全与功能配置**（见 WebView 配置要求）。
  - 观察 ViewModel 中的 `formulaLiveData`，当公式变化时调用 `evaluateJavascript` 执行 KaTeX 渲染（调用 HTML 中定义的 `renderKatex(formula)` 函数）。
  - 观察 ViewModel 中的 `errorLiveData`，弹出 Toast 显示错误信息。
  - 设置一个“渲染”按钮（布局中提供），点击时从 EditText 读取内容，调用 `viewModel.updateFormula(input)`。
- 正确处理 `onDestroy`，无需做多余清理（ViewModel 负责协程）。
- 绝对不允许使用 `webView.loadUrl("javascript:...")` 这种有注入风险的方式，必须使用 `evaluateJavascript`。
- 不允许在 Activity 中直接启动协程，所有业务逻辑交给 ViewModel。
- 必须导入所有需要的包，文件开头声明 `package com.example.katexlocal`。

### 2. MainViewModel.kt
- 继承 `ViewModel()`。
- 提供：
  - `private val _formula = MutableLiveData<String>()` → `val formulaLiveData: LiveData<String>`
  - `private val _error = MutableLiveData<String>()` → `val errorLiveData: LiveData<String>`
- `updateFormula(rawInput: String)` 方法:
  - 在 `viewModelScope` 中执行（使用 `viewModelScope.launch` 并在 `Dispatchers.Default` 下做简单处理，但最终切回主线程 post 值）。
  - 对输入做基础校验：若为空或长度<1 则设置 error 为 "公式不能为空"。
  - 否则去除首尾空格，将公式字符串设置到 `_formula`，清空 `_error`。
- 严禁暴露 MutableLiveData 给外部修改。
- 所有 LiveData 初始化在声明时完成。

### 3. WebView 配置（在 MainActivity 中实现，作为私有扩展函数 `setupWebView`）
- 启用 JavaScript：`webView.settings.javaScriptEnabled = true`
- 禁用文件访问安全漏洞：
  - `webView.settings.allowFileAccess = true` （因为我们要加载本地 assets，必须允许）
  - 但必须设置 `webView.settings.allowFileAccessFromFileURLs = false`
  - `webView.settings.allowUniversalAccessFromFileURLs = false`
- 启用 DOM 存储：`webView.settings.domStorageEnabled = true`
- 设置混合内容策略为 `WebSettings.MIXED_CONTENT_NEVER_ALLOW`（因为全离线）
- 禁止缩放：`webView.settings.setSupportZoom(false)`
- 设置 WebViewClient，覆盖 `onReceivedError`，将错误信息 post 到 ViewModel 的 errorLiveData。
- 加载本地 HTML：`webView.loadUrl("file:///android_asset/index.html")`，并确认在 `onPageFinished` 中触发一次初始公式渲染（默认公式可为 `E=mc^2`）。
- 禁止 WebView 的远程调试 (如果不要求，可保留，但不要额外声明安全)。

### 4. 状态管理规范
- 所有与 UI 相关的状态（公式文本、错误）通过 LiveData 驱动，View 层只做观察和调用。
- 不允许使用全局变量、companion object 保存 WebView 实例或状态。
- 唯一数据源是 ViewModel，EditText 输入直接传递到 ViewModel 处理，不绕过。

### 5. KaTeX 本地化实现
- `index.html` 必须使用下列本地路径引用 KaTeX 资源（不得使用 CDN）：
  - CSS: `<link rel="stylesheet" href="katex/katex.min.css">`
  - JS: `<script src="katex/katex.min.js"></script>`
  - 若有 auto-render 可选引用，也用本地路径。
- HTML 中定义一个 JavaScript 函数 `renderKatex(formula)`，内部调用 `katex.render(formula, document.getElementById('math'), { throwOnError: false })`，并提供友好错误捕获（在渲染出错时返回错误字符串并通过 `console.error` 输出，Android 端可通过 `onConsoleMessage` 捕获）。
- 设置 `WebChromeClient` 的 `onConsoleMessage`，将 KaTeX 的错误信息传递给 ViewModel。
- `katex.min.css`、`katex.min.js`、`auto-render.min.js` 文件生成时内容仅为注释，例如 `/* 请从 KaTeX 官方发行版复制该文件到此路径 */`，并在 README 中说明。
- 确保 `index.html` 内的 DOM 元素 `<div id="math"></div>` 存在，且样式使公式清晰可见（字体大小、边距等）。

### 6. 错误处理覆盖点
- WebView 页面加载失败 → 通过 `onReceivedError` 推送错误到 LiveData → Toast。
- KaTeX 渲染异常（JS 语法错误、公式错误） → 通过 `consoleMessage` 捕获 → 设置 errorLiveData。
- 输入为空 → ViewModel 设置错误。
- 所有错误信息使用中文显示（如“WebView 加载失败”、“公式渲染出错”等）。

### 7. activity_main.xml 布局
- 根布局 LinearLayout 垂直方向，padding 16dp。
- 包含一个 EditText (id=formulaInput)，一个 Button (id=renderButton, text="渲染")，一个 WebView (id=katexWebView)，高度 0dp，layout_weight=1 占满剩余空间。
- 不使用硬编码字符串，用 `@string/...` 引用，并在 `res/values/strings.xml` 中定义（可简单说明，不作为必须文件，但为了完整性可在布局内直接写 text 但要求代码不要硬编码到 Kotlin）。

### 8. README.md
- 简短说明需要手动下载 KaTeX 资源并放入 `assets/katex/` 目录。
- 列出必须的文件：katex.min.css, katex.min.js，以及存放位置。
- 说明运行后可离线验证数学公式渲染。

## 代码质量与完整性硬性要求
- 所有 Kotlin 文件必须能编译通过（假设已有合适依赖），不得引用未定义的类或资源。
- 禁止使用 `TODO()` 占位；每个功能必须完整实现。
- 注释使用中文，清晰解释安全配置和状态管理逻辑。
- 不要截断代码，每个文件完整输出。

请严格按照以上要求生成项目所有文件，不得遗漏任何细节。现在开始输出代码。