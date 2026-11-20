# 多模态模型横评 · 试卷识别（2026-11-20）

样本：用户提供的《试卷识别测试.jpg》旋转校正后裁两题
- Q4：数轴 + 绝对值化简 `|a-c|-|c-b|+|a+b|`，含手写解答（正确答案 -2a，手写正确）
- Q1：数形结合求和式选择题（正确 A 1/3）

统一 prompt，temperature=0.1，JSON 输出，测试通道：SiliconFlow / 小米 MiMo。

## 结果

| 模型 | Q4 解答 | Q1 识别/解答 | 手写判断 | 耗时 | 备注 |
|---|---|---|---|---|---|
| Qwen3-VL-32B-Instruct | ✅ -2a，步骤完整 | ⚠️ 公式漏项，答 C（错） | ⚠️ 部分正确 | 17–60s | 综合最快最稳，OCR 需裁剪辅助 |
| Qwen3-VL-32B-Thinking | ✅ -2a | ✅ 公式/选项正确，答 A | ✅ 正确 | 71–117s | 质量最好，但太慢、思考 token 多 |
| Qwen3-VL-30B-A3B-Instruct | ✅ -2a（有小错） | 未测 | ❌ 判错 | 31s | 会把题干写成 \includegraphics |
| Qwen3-VL-8B-Instruct | ✅ -2a | ❌ 公式错、答 B | ❌ 判错 | 16–22s | 快，但 OCR/推理明显弱 |
| Qwen3-VL-8B-Thinking | ✅ -2a | 未测 | ✅ 正确 | 157s | 超时重试过，不适合在线 |
| PaddleOCR-VL-1.5 | 仅转写 | ❌ 不按 JSON/不解答 | — | 103–120s | 只适合当 OCR 前置 |
| DeepSeek-OCR | ❌ 输出乱码 `} } }` | ❌ | — | 9s | 只能按其 OCR 专用格式用 |
| mimo-v2.6-pro | ✅ -2a | ✅ 答 A，转写基本正确 | ⚠️ 部分正确 | 25–54s | App 已配置的通道，质量接近 32B |
| mimo-v2.6-flash | ✅ -2a | ❌ 思考烧光 token，返回空 | ⚠️ | 22–85s | 推理 token 计入 max_tokens，需调大/关思考 |

## 关键发现

1. **MiMo 的 reasoning 会计入 max_tokens**：1500 时思考还没结束就耗尽，content 为空；pro 在 4000 下正常（Q1 推理 649 token），flash 在 Q1 直接烧满 3000 token 无输出。
2. **OCR 仍要前置**：带图形/手写的整页直接丢给 VL 模型，公式容易漏项；先圈选/裁题能显著提升准确率。
3. **Thinking 模型适合异步批改**，不适合拍照即时反馈。
4. SiliconFlow 测试后段出现 TLS/超时（网络代理抖动），部分重跑未完成，结论以成功样本为准。

## 推荐

- **主链路**：Qwen3-VL-32B-Instruct 或 mimo-v2.6-pro（质量/速度平衡，25s 内）。
- **高精度异步**：Qwen3-VL-32B-Thinking（批改/难题，接受 1–2 分钟）。
- **轻量兜底**：Qwen3-VL-8B-Instruct（弱网/低端机）。
- PaddleOCR-VL 作为 OCR 前置可继续评估，但不要当解题模型。
- App 设置页增加模型切换 + 失败自动降级；MiMo 通道 max_tokens 调至 ≥4096 并考虑关闭思考。
