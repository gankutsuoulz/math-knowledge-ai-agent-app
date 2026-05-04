# MathApp UI 组件库报告

## 1. 组件概览

### 主题结构
本组件库基于 **Material Design 3 (Material You)** 构建，通过 `MathAppTheme` Composable 函数提供统一的主题上下文。该主题会根据系统设置自动切换亮色（Light）和暗色（Dark）模式。

### 颜色系统
定义了完整的 Material 3 调色板，包含亮色和暗色两套方案。主要特点：
- **主色调**：基于紫色系（Primary: `#6750A4`），营造现代、专业的视觉感受。
- **容器颜色**：使用浅色调（如 `primaryContainer: #D0BCFF`）确保内容在容器上的可读性。
- **完整属性**：包含了 `inverseSurface`、`inverseOnSurface`、`inversePrimary` 等属性，确保在深色表面或特殊场景下的颜色对比度符合无障碍标准。

### 字体系统
代码中引用了 `Typography` 对象，但未在提供的代码片段中定义具体字体。通常，此对象应包含 `MaterialTheme.typography` 中定义的所有文本样式（如 `displayLarge`, `bodyMedium` 等），以确保应用内文本风格的一致性。

### 组件列表
本库提供了以下自定义 UI 组件：
1.  **LoadingIndicator** - 加载指示器
2.  **AppIconButton** - 图标按钮
3.  **AppOutlinedButton** - 描边按钮
4.  **WarningIcon** - 警告图标
5.  **ErrorIcon** - 错误图标

*注：根据修复总结和常见模式，报告中将补充说明 `ErrorMessage`、`EmptyState` 和 `PrimaryButton` 的设计意图。*

---

## 2. 组件说明

### LoadingIndicator - 加载指示器
- **样式**：一个圆形的进度指示器，颜色默认使用主题的 `primary` 色，线宽为 4dp。它是一个简洁、标准的 Material 3 加载动画。
- **用途**：用于表示页面、列表或某个操作正在进行数据加载或处理，提示用户需要等待。

### ErrorMessage - 错误消息
- **样式**：通常由一个 `ErrorIcon` 或 `WarningIcon` 与一段错误描述文本组合而成。图标颜色默认使用主题的 `error` 色，文本使用 `onSurface` 色以确保可读性。
- **用途**：向用户明确展示操作失败、网络错误或数据验证失败等异常情况，并提供可能的错误信息。

### EmptyState - 空状态
- **样式**：通常包含一个居中的插图（或图标）、一个标题和一段描述性文字，有时会包含一个操作按钮（如 `PrimaryButton`）。整体布局居中，使用较浅的文本颜色（如 `onSurfaceVariant`）以降低视觉权重。
- **用途**：当列表无数据、搜索无结果或功能模块首次进入时，引导用户了解当前状态并提示下一步操作（如“点击刷新”或“添加新项目”）。

### PrimaryButton - 主按钮
- **样式**：一个填充了主题 `primary` 色的按钮，文字颜色为 `onPrimary`。具有标准的圆角（`cornerRadiusMedium`）和内边距。是页面中最主要的操作触发器。
- **用途**：用于执行页面或对话框中的主要、肯定性操作，例如“确认”、“提交”、“保存”、“登录”等。

### AppIconButton - 图标按钮
- **样式**：一个仅包含图标的按钮，没有背景填充。点击时会有涟漪效果。图标颜色默认为 `onSurface`，可通过 `tint` 参数自定义。
- **用途**：用于工具栏、列表项或卡片中的次要操作，例如“收藏”、“分享”、“删除”、“更多选项”等。相比文字按钮，它更节省空间，适合图标含义明确的场景。

---

## 3. 设计规范

### 间距规范
所有间距和尺寸均定义在 `Dimens` 对象中，确保全局一致性。
- **间距 (Spacing)**：
    - `spacingSmall`: 4dp
    - `spacingMedium`: 8dp
    - `spacingLarge`: 16dp
    - `spacingExtraLarge`: 24dp
- **内边距 (Padding)**：
    - `paddingSmall`: 4dp
    - `paddingMedium`: 8dp
    - `paddingLarge`: 16dp
    - `paddingExtraLarge`: 24dp

### 颜色规范
遵循 Material 3 语义化颜色系统，主要颜色角色如下（以亮色模式为例）：
- **Primary (`#6750A4`)**：品牌主色，用于关键操作和强调元素。
- **OnPrimary (`#FFFFFF`)**：主色上的前景色（如按钮文字）。
- **PrimaryContainer (`#D0BCFF`)**：主色的浅色容器，用于承载次要信息或作为背景。
- **Secondary (`#625B71`)**：辅助色，用于次要操作和元素。
- **Surface (`#FFFBFE`)**：主要背景色。
- **OnSurface (`#1C1B1F`)**：主要背景上的前景色（如正文文字）。
- **Error (`#B3261E`)**：错误状态色。
- **Outline (`#79747E`)**：用于边框、分隔线等。

### 圆角规范
定义在 `Dimens` 对象中：
- `cornerRadiusSmall`: 4dp
- `cornerRadiusMedium`: 8dp
- `cornerRadiusLarge`: 16dp

---

## 4. 使用示例

### LoadingIndicator
```kotlin
// 在屏幕中央显示加载指示器
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    LoadingIndicator()
}

// 在按钮内显示加载状态
Button(onClick = { /* ... */ }) {
    LoadingIndicator(
        modifier = Modifier.size(20.dp),
        color = MaterialTheme.colorScheme.onPrimary
    )
    Spacer(modifier = Modifier.width(Dimens.spacingMedium))
    Text("提交中...")
}
```

### ErrorMessage
```kotlin
// 典型的错误消息布局
Column(
    modifier = Modifier.padding(Dimens.paddingLarge),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    ErrorIcon(modifier = Modifier.size(48.dp))
    Spacer(modifier = Modifier.height(Dimens.spacingMedium))
    Text(
        text = "加载失败",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(Dimens.spacingSmall))
    Text(
        text = "请检查网络连接后重试。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

### EmptyState
```kotlin
// 典型的空状态布局
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(Dimens.paddingExtraLarge),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    Icon(
        imageVector = Icons.Outlined.Inbox,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(Dimens.spacingLarge))
    Text(
        text = "暂无数据",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(Dimens.spacingSmall))
    Text(
        text = "这里空空如也，去添加一些内容吧。",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))
    PrimaryButton(onClick = { /* 刷新或添加操作 */ }) {
        Text("刷新")
    }
}
```

### PrimaryButton
```kotlin
// 主要操作按钮
PrimaryButton(
    onClick = { viewModel.submitForm() },
    modifier = Modifier.fillMaxWidth()
) {
    Text("提交订单")
}

// 带图标的按钮
PrimaryButton(onClick = { /* ... */ }) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize)
    )
    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
    Text("新建项目")
}
```

### AppIconButton
```kotlin
// 在TopAppBar中使用
TopAppBar(
    title = { Text("详情") },
    navigationIcon = {
        AppIconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
        }
    },
    actions = {
        AppIconButton(onClick = { shareContent() }) {
            Icon(Icons.Default.Share, contentDescription = "分享")
        }
        AppIconButton(onClick = { toggleFavorite() }) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "收藏")
        }
    }
)

// 在列表项中使用
ListItem(
    headlineContent = { Text("项目名称") },
    trailingContent = {
        AppIconButton(onClick = { deleteItem(item) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
)
```