# MEMORY.md — 数学工作区长期记忆

> 更新：2026-07-16

## 工作区路径
`/projects/math-app/math-workspace/`

## 项目：数学知识助手 APP

### 项目概述
- **目标**：创建数学类安卓APP
- **源码路径**：`/projects/math-app/math-workspace/app/`（单层 app 模块）
- **GitHub 仓库**：`gankutsuoulz/math-knowledge-ai-agent-app`
- **知识来源**：`dim1-formula.md` ~ `dim4-formula.md`（四维度框架）
- **模型架构**：
  - 处理基座：xiaomi mimo v2.5-pro（文字推理）+ mimo-v2.5（图像识别）
  - 可选：deepseek-v4-pro（文字推理）
  - APP直接调用大模型，**不经过OpenClaw**
  - API Key由用户在APP设置中自行填入

### 核心文档
| 文件 | 说明 |
|------|------|
| `APP_PROTOTYPE.md` | 简单版原型（5页） |
| `PROTOTYPE_V1.md` | 完整版原型（含7章节详细设计） |
| `REQUIREMENTS_ORGANIZED.md` | 需求整理文档 |

### 知识体系
- **四大板块**：数与代数 / 图形与几何 / 统计与概率 / 综合与实践
- **知识点总数**：约395个
- **年级覆盖**：五年级 ~ 高三

### 版本号规则
- **版本格式**：`major.minor.patch`
- **自增规则**：每次构建 patch 自动 +1（从 `build/version.properties` 读取/写入）
- **versionCode**：使用当前时间戳（秒级），每次构建不同
- **当前版本**：1.1.19（2026-07-16）
- **APK 输出名**：`math-app-{versionName}.apk`
- **手动指定版本**：`./gradlew assembleRelease -PbuildVersion=1.2.0 -PbuildVersionCode=123`

---

## 题库设计

### 双层题库结构
1. **标准题库**
   - 导入的题库作为标准题库
   - 支持版本更新

2. **用户个性化题库**
   - 用户拍照/上传分析的题目可添加进来
   - 标准题库更新时，个性化题库**不被覆盖**
   - 支持**导出**功能
   - 可从知识点的例题中查看

### 收藏保护机制
- 用户收藏的题库题目，当被从标准题库移除时，**仍保留在APP中**
- 个性化题目独立存在，不受标准题库更新影响

### 例题库规格
- **存储格式**：SQLite（Room） + JSON（导入/导出）
- **每知识点例题数**：≥3道（基础/中等/困难）
- **预估总量**：~2000道
- **审核**：生成后使用 deepseek-v4-pro 检查确保无错误
- **离线可用**：完全离线

---

## 功能需求（F-01~F-08）

| 编号 | 功能 | 优先级 | 状态 |
|------|------|--------|------|
| F-01 | 拍照解题（拍照/框选/AI分析） | P0 | ✅ 已实现 |
| F-02 | 知识点速查（LaTeX公式渲染） | P0 | 🔄 待完善三级分类 |
| F-03 | 例题练习（分知识点刷题） | P0 | ✅ 已实现 |
| F-04 | 例题库管理（导入/导出/双层结构） | P0 | ✅ 已实现 |
| F-05 | 收藏夹（收藏保护机制） | P1 | ✅ 已实现 |
| F-06 | 搜索功能 | P1 | ✅ 已实现 |
| F-07 | 错误辅导（AI引导自主解决） | P0 | ✅ 已实现 |
| F-08 | 拍照转公式（LaTeX提取） | P2 | ⏳ 待实现 |

---

## 页面设计（12页）

1. 首页（Home）
2. 拍照解题页
3. AI辅导结果页
4. 知识点速查页
5. 知识点详情页
6. 例题练习页
7. 答案解析页
8. 收藏夹页
9. 设置页（API Key配置入口）
10. 搜索页
11. 例题库管理页
12. 拍照转公式页

---

## 技术架构

| 层级 | 技术选型 |
|------|---------|
| 前端框架 | Kotlin + Jetpack Compose |
| 公式渲染 | KaTeX（WebView） |
| 图片框选 | Canvas + Touch事件 |
| AI图像识别 | mimo-v2.5（直调） |
| AI文字推理 | mimo-v2.5-pro 或 deepseek-v4-pro（可切换） |
| 本地存储 | Room数据库 + KSP |
| 架构 | MVVM |
| 最小SDK | API 26（Android 8.0） |
| 目标SDK | API 34 |
| 编译SDK | API 34 |

---

## 重要注意事项

### 代码目录不要混淆
- **正确目录**：`/projects/math-app/math-workspace/`（Git v1.1.x）
- **旧的副本目录**：`/projects/verify-app/`（v1.0 副本，不要碰）
- 两个目录关联同一 Git remote，但 verify-app 是旧 commit

### 知识点速查（F-02）
- 当前状态：已实现 `KnowledgeScreen.kt` 基础知识展示
- 待办：给知识点增加二级分类（SubTopic）支持三级导航
- `KnowledgeScreen.kt` 位置：`app/src/main/java/com/mathkatex/verify/screens/KnowledgeScreen.kt`

---

## 历史记录

### 2026-04-28 初始化
- 原始知识点框架文件创建
- 四套维度框架（dim1-dim4）生成完成

### 2026-05-01 项目启动
- 数学APP需求提出，原型生成
- 需求收集完成，PROTOTYPE_V1.md 完成

### 2026-07-15 v1.1.1+ 构建
- 版本号更新，WebView 公式渲染修复
- GitHub 仓库持续推送

### 2026-07-16 检查确认
- 确认正确代码目录为 `/projects/math-app/math-workspace/`
- 版本号 1.1.19（version.properties），自动 patch 递增
- settings.gradle 单层 `app/` 结构，无需额外配置