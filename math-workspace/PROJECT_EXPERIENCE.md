# 数学知识点助手 APP — 项目经验总结

> 记录时间：2026-07-16

## 一、项目结构

### 工作区路径
- **正确源码目录**：`/projects/math-app/math-workspace/app/`
- **单层模块结构**：settings.gradle 中 `include ':app'`，`app/` 下直接是 `src/` 目录
- **不要混淆**：`/projects/verify-app/` 是旧版本副本（v1.0），不要在那里修改

### Git 仓库
- 远程：`https://github.com/gankutsuoulz/math-knowledge-ai-agent-app.git`
- 推送需通过 SOCKS5 代理：`git -c http.proxy=socks5h://127.0.0.1:10808 push origin master`

### 版本号规则
- 自动递增：`build.gradle` 中 `computeVersionName()` 从 `version.properties` 读取
- 格式：`major.minor.patch`，每次构建 patch +1
- **注意**：version.properties 曾放在 `app/build/` 下，`clean` 会清掉
- **修复**：已移至 `app/version.properties`（`app/build/` 之外），clean 不再重置

---

## 二、编译注意事项

### 代理问题
- `gradle.properties` 中配置了 HTTP 代理 `127.0.0.1:10809`，这是**曾立峰的开发习惯**
- v2ray 服务可能已停止（`systemctl is-active v2ray` 检查）
- 编译前必须确保 v2ray 在运行：`v2ray run -c /usr/local/etc/v2ray/config.json &`
- 否则 Gradle 因代理端口不通反复重试 → OOM

### 编译命令
```bash
# 确保 v2ray 在运行
ps aux | grep v2ray | grep -v grep

# debug 编译（默认不含签名）
./gradlew :app:assembleDebug

# release 编译（带 keystore.jks 签名）
./gradlew :app:assembleRelease
```

### 签名
- keystore：`app/keystore.jks`
- 密码：`Reading@2026`
- 别名：`reading`
- debug 构建默认不含签名，需手动用 apksigner 签名后发布
- 签名命令：`apksigner sign --ks app/keystore.jks --ks-pass pass:'Reading@2026' --ks-key-alias reading --out output.apk input.apk`

### 发布到 FTP
- 本地 FTP 服务已运行（vsftpd）
- 根目录：`/srv/ftp/`
- 内网地址：`ftp://192.168.5.4/`
- 上传：`cp apk文件 /srv/ftp/`

---

## 三、知识点速查开发经验（F-02）

### 数据库架构
- **Room 数据库**：`KnowledgeEntity` 表，7 个分类字段
  - `block`：板块（数与代数/图形与几何/统计与概率/综合与实践）
  - `categoryLv1`：一级分类（如"数与运算基础"）
  - `categoryLv2`：二级分类（如"小数的认识与运算"）
  - `categoryLv3`：三级分类（可选，如"分数乘法"）
  - `categoryLv4`：四级分类（可选）
- **数据来源**：`assets/dim1-formula.md`，启动时自动导入
- **DAO 查询**：纯 SQL DISTINCT 去重查询

### 历史问题与修复

| 问题 | 根因 | 修复 |
|------|------|------|
| 二级分类显示"暂无子分类" | 多次 LaunchedEffect 竞态条件 + SQL 全角半角不匹配 | 全量加载 + 内存分组过滤，不再逐层查 SQL |
| 知识点顺序无规律 | SQL ORDER BY id 被忽略 | `.sortedBy { it.id }` 在内存中排序 |
| 详情页按返回键回到首页 | 物理返回键触发 Activity 导航栈 | 添加 `BackHandler` 拦截返回键 |
| 二级分类展开三级多余 | 原设计点击二级才展开知识点列表 | 改为点击二级直接进入详情页 |
| 详情页公式未渲染 | 多个 `MarkdownWebView` 实例相互干扰 | 改为单 WebView + 合并 markdown 统一渲染 |
| 版本号 clean 后重置 | version.properties 在 build/ 下 | 移至 app/ 目录 |

### 知识点详情页的最佳实践
- **公式渲染**：用 `MarkdownWebView`（内部用 KaTeX CDN）
- **多点并列**：多个知识点合并为一个 markdown，一个 WebView 渲染全部
- **数据传递**：通过 `remember` 派生状态，避免 `LaunchedEffect` 竞态

---

## 四、通过飞书发送文件

### Python 脚本模板
```python
import json, urllib.request, time

# 获取 token
url = 'https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal'
data = json.dumps({"app_id": app_id, "app_secret": app_secret}).encode()
req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req) as resp:
    token = json.loads(resp.read())['tenant_access_token']

# 上传文件
boundary = '----' + str(int(time.time()))
body = [
    f'--{boundary}\r\n'.encode(),
    b'Content-Disposition: form-data; name="file_type"\r\n\r\nstream\r\n',
    f'--{boundary}\r\n'.encode(),
    b'Content-Disposition: form-data; name="file_name"\r\n\r\n',
    file_name.encode() + b'\r\n',
    f'--{boundary}\r\n'.encode(),
    f'Content-Disposition: form-data; name="file"; filename="{file_name}"\r\n'.encode(),
    b'Content-Type: application/octet-stream\r\n\r\n',
    file_content, b'\r\n',
    f'--{boundary}--\r\n'.encode(),
]
req2 = urllib.request.Request('https://open.feishu.cn/open-apis/im/v1/files',
    data=b''.join(body),
    headers={"Authorization": f"Bearer {token}", "Content-Type": f"multipart/form-data; boundary={boundary}"})
file_key = json.loads(urllib.request.urlopen(req2).read())['data']['file_key']

# 发送消息
sender = 'ou_e7d0ae37bf57aa11621ea355f9481c5b'
msg = json.dumps({"receive_id": sender, "msg_type": "file",
    "content": json.dumps({"file_key": file_key})}).encode()
urllib.request.Request('https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=open_id',
    data=msg, headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"})
```

### 常用飞书用户
- 用户 open_id：`ou_e7d0ae37bf57aa11621ea355f9481c5b`

---

## 五、OpenClaw Gateway 问题排查

### Memory Search 失败
- **症状**：`memory_search` 工具调用返回 `No API key found for provider "openai"`
- **根因**：memory-core 默认使用 openai 作为 embedding provider，但没有配置 openai API key
- **修复**：在 `agents.defaults.memorySearch` 中配置硅基流动的 embedding API
  - provider: `openai-compatible`
  - model: `BAAI/bge-m3`
  - baseUrl: `https://api.siliconflow.cn/v1`
  - apiKey: 用户提供的硅基流动 key

### 飞书 "Agent couldn't generate a response"
- **根因**：memory_search 工具调用失败后，agent 在大量 tool call 重试中被截断
- **修复**：与 Memory Search 修复相同，embedding provider 恢复正常后自动解决

---

## 六、文档处理经验

### Word 文档生成
- 使用 `python-docx` 生成 .docx 文件
- 党政机关公文格式：上3.7cm 下3.5cm 左2.8cm 右2.6cm，三号仿宋，行距28磅
- 修订模式：红色删除线 = 删除内容，蓝色下划线 = 新增内容
- 表格：用 `Table Grid` 样式，`D9E2F3` 背景色表头

### .doc 文件读取
- LibreOffice 转换：`libreoffice --headless --convert-to txt file.doc`
- 编码问题：中文 .doc 文件用 GBK 解码

---

## 七、安全报告分析经验

### 告警数据分析
- 告警 CSV 列：来源IP / 目的IP / 告警子类型 / 事件名称 / 目的端口 / 威胁等级 / 处置结果 / 时间
- 重点关注：告警结果为"成功"的高危事件、内网横向攻击、僵木蠕外联
- 对比分析：5月→7月告警数据对比，发现Redis弱口令、明文传输等历史问题未闭环

### 报告生成
- 结构：整体概况 / 重点威胁分析 / 处置建议 / 与上月对比 / 结论
- 统计数据分布优先，大篇幅文字在后
- 表格：2列（事件名称+核心信息）用于告警类型汇总，3列（风险+状态+建议）用于处置建议
