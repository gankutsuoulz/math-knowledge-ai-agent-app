# 版本锁定 — v1.2.0（2026-11-20）

基础框架与练习闭环验收通过，锁定为 **v1.2.0**。

## 锁定内容
- UI v1：深海蓝主题、三 Tab、首页、知识速查、例题练习、错题本、收藏、我的统计
- 数据：dim1 368 知识点；例题 60 道 / 20 个初一初二计算知识点
- Room v3：examples(type/options)、practice_records、favorites，迁移 1→2→3
- 应用图标：鲸喜形象（自适应图标 + 旧版/圆形图标）

## 图标安全区
- 全幅头像、无圆框：使用 App 头像的 768px 宽幅原图（256px 版头饰被裁），按脸部中线水平居中，头部（含头饰）完整落在可见 72dp 区域内
- 已按圆形、圆角方形、方形遮罩预览，真机 Launcher 验证脸部/头部不被系统裁切
- 重新生成：`python3 tools/gen_icons.py`（源图 `tools/avatar-768.png`）

## 版本规则
- `app/version.properties`：1.2.0 / VERSION_CODE=1795107700
- 功能变更升 MINOR，修复升 PATCH；发布前跑：
  `GRADLE_USER_HOME=/home/dsh/飞书机器人/.mathapp/gradle-home ./gradlew --offline --no-daemon :app:assembleDebug :app:assembleRelease -x lintVitalAnalyzeRelease -x lintVitalRelease`
- 锁定后改动需开新分支，验收再合入；不打远端（用户要求）。


## v1.2.1 热修（2026-11-20）

- 默认模型：mimo-v2.6-pro（文本/视觉），旧 v2.5 配置自动升级。
- LLM max_tokens：MiMo 131072、DeepSeek 8192。
- 桥接：hub 超时后静默等待并 5 分钟心跳，完成后再回结果，不提示超时。
