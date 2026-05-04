# Task: KaTeX WebView Full Rendering (方案B)

## 问题描述
f01（拍照解题）解析结果界面中，公式显示为 `[公式]` 占位符，未正确加载显示实际公式。

## 根本原因
`MarkdownRenderer.kt` 中的 `renderKaTeX` 函数检测到 `$...$` 或 `$$...$$` 公式后，只是显示占位符文本 `【公式】` / `「公式」`，并没有真正渲染 KaTeX。

## 解决方案（方案B：全 WebView 渲染）

### 目标
将 Markdown 文本渲染和 KaTeX 公式渲染合并到一个 WebView 中，全部使用 HTML + JavaScript 渲染。

### 技术要点
1. 将 Markdown 文本转换为 HTML（保留粗体、斜体、列表、代码块等格式）
2. 将 `$...$` 转换为行内 KaTeX 公式，`$$...$$` 转换为行间 KaTeX 公式
3. 使用 KaTeX CDN 渲染公式：https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.16.9/
4. 构建完整的 HTML 页面，包含 CSS 样式和 KaTeX 库
5. 在 Compose 中使用 AndroidView 嵌入 WebView

### 参考代码
- 现有 `KaTeXWebView` 组件（`KaTeXVerifyScreen.kt`）提供了 WebView + KaTeX 的基础实现
- 需要在其基础上扩展，支持 Markdown 转 HTML

### 预期效果
解析结果中的数学公式（如 $\frac{a}{b}$、$\sqrt{x^2+y^2}$、$\sum_{i=1}^{n} x_i$ 等）都能正常渲染显示。

## 输入
需要处理的示例文本：
```
请仔细分析这张图片中的数学题目，给出完整的解题步骤和最终答案。

已知: $\frac{1}{x} + \frac{1}{y} = 5$, $\frac{1}{xy} = 6$

求: $x + y$ 的值

解: 
第一步：根据倒数关系
$$\frac{1}{x} + \frac{1}{y} = \frac{x+y}{xy} = 5$$

由 $\frac{1}{xy} = 6$ 可得 $xy = \frac{1}{6}$

代入得: $x + y = 5 \times \frac{1}{6} = \frac{5}{6}$

**答案**: $\frac{5}{6}$
```

## 验收标准
1. 行内公式 `$...$` 正确渲染为数学符号
2. 行间公式 `$$...$$` 正确渲染为居中数学公式块
3. Markdown 格式（粗体、斜体、列表等）保持正确
4. 在 Android WebView 中正常显示，无安全警告
