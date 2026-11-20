# mathApp 无人值守改造计划

> 2026-11-20 02:20，用户离线至 06:30；目标在此之前完成正式版 UI 落地。

## 已完成

- 构建可复现：version 只读、签名外置、去代理、in-process 修中文路径 KSP。
- KaTeX 本地化：assets/katex（81 个文件），APK 内已确认；断网可渲染。
- UI/UX 正式版锁定：`/home/dsh/飞书机器人/mathapp-ui-demo/DESIGN_LOCK.md`。

## 构建命令

```bash
cd /home/dsh/飞书机器人/projects/math-app/math-workspace
GRADLE_USER_HOME=/home/dsh/飞书机器人/.mathapp/gradle-home \
  ./gradlew --offline --no-daemon :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/math-app-1.1.26.apk`

## 约束

- 不碰 API Key / 设置页逻辑；不改网络层。
- 不 push 远端仓库；改动前留备份。
- 每完成一个模块必须 assembleDebug 通过，失败就回滚该模块。
- 视觉以 `mathapp-ui-demo/index.html` 为准，色板见 DESIGN_LOCK。

## 待办

- [x] 主题：`ui/theme/Color.kt`、`Theme.kt`，新增 `Type.kt`/`Shape.kt`；关闭 dynamicColor。（已编译通过）
- [x] 首页 + 底部 3 Tab：重做 `HomeScreen.kt`、`MainActivity.kt` 导航；占位页收进「我的」。（已编译通过）
- [x] 解题辅导结果页：`PhotoSolveScreen.kt` 结果区改成「已识别 + 先别急着看答案」卡片；提示词改为引导式，不直接给答案。（已编译通过）
- [x] 知识列表/详情：`KnowledgeScreen.kt` 卡片化、板块短名切换、详情页配色统一。（已编译通过）
- [x] 全量编译通过；android-emulator 容器真机跑通，首页/知识/我的/深色均截图确认。
- [ ] 写 `notes/mathapp.md` 更新，push 验收报告到飞书。

## 决策记录

- 2026-11-20 02:18：KaTeX 编译通过；本地持久 Gradle home 可用，不再每次复制 2.3G。
- 2026-11-20 02:32：主题 + 首页 + 底部导航 + 我的页完成，assembleDebug 通过。
- 2026-11-20 02:25：知识列表/详情、解题结果页完成；全量 assembleDebug 通过。
- 2026-11-20 07:10：补 /dev/kvm 节点后用 android-emulator:test-avd 真机跑通，截图确认首页/知识/我的；修了状态栏灰条，知识板块改胶囊并固定顺序。
- 2026-11-20 07:20：精修：首页一屏放下今日目标、CTA 深色对比度、知识卡片加首字徽标/数量副标题；深色模式截图确认。
- 无人值守期间不依赖桥接进度：goal 轮次在 web 进程内继续，完成后再推送总结。
