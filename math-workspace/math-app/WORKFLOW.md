# 数学知识助手APP - 开发工序与工作流

> 整理时间：2026-05-01
> 更新：2026-05-01 19:10
> 模型：mimo-v2.5-pro（编码）+ deepseek-v4-pro（CodeReview）

---

## 一、编码+Review流程

每个工作项执行：**mimo编码 → deepseek review → 修正 → 交付**

**通知要求：**
- 提交mimo编码时：告知"此代码将由deepseek-v4-pro进行CodeReview"
- 提交deepseek Review时：告知"此代码由mimo-v2.5-pro编写"

---

## 二、工序列表

### 工作项 1：KaTeX技术验证
| 项目 | 内容 |
|------|------|
| 目的 | 验证KaTeX与Jetpack Compose兼容性 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | KaTeX渲染验证报告（包含测试代码） |
| 前置依赖 | 无 |
| 状态 | 🔄 进行中 |

### 工作项 2：P0-1 项目脚手架
| 项目 | 内容 |
|------|------|
| 目的 | 创建可编译的Gradle空壳项目 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 可编译的空壳项目，含全部依赖配置 |
| 前置依赖 | 工作项1 |
| 状态 | ⏳ 待处理 |

### 工作项 3：P0-2 Clean Architecture骨架
| 项目 | 内容 |
|------|------|
| 目的 | 分层目录结构与基础路由 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 骨架代码（UI/Domain/Data分层） |
| 前置依赖 | 工作项2 |
| 状态 | ⏳ 待处理 |

### 工作项 4：P1-1 Room数据库搭建
| 项目 | 内容 |
|------|------|
| 目的 | 数据库Entity/DAO/迁移策略 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | Database.kt + DAO + Entity |
| 前置依赖 | 工作项3 |
| 状态 | ⏳ 待处理 |

### 工作项 5：P1-2 AI服务客户端封装
| 项目 | 内容 |
|------|------|
| 目的 | HTTP客户端封装（mimo/deepseek API） |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | ApiService + Repository |
| 前置依赖 | 工作项3 |
| 状态 | ⏳ 待处理 |

### 工作项 6：P2-2 知识点速查模块
| 项目 | 内容 |
|------|------|
| 目的 | 搜索+分类列表+详情页 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 知识点速查完整功能 |
| 前置依赖 | 工作项4, 工作项5 |
| 状态 | ⏳ 待处理 |

### 工作项 7：P2-3 例题练习模块
| 项目 | 内容 |
|------|------|
| 目的 | 题库浏览+做题+答案解析 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 例题练习完整闭环 |
| 前置依赖 | 工作项4 |
| 状态 | ⏳ 待处理 |

### 工作项 8：P2-5 收藏夹模块
| 项目 | 内容 |
|------|------|
| 目的 | 收藏CRUD+筛选 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 收藏管理页面 |
| 前置依赖 | 工作项4 |
| 状态 | ⏳ 待处理 |

### 工作项 9：P2-6 设置模块
| 项目 | 内容 |
|------|------|
| 目的 | API配置+主题+关于页 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 设置页面 |
| 前置依赖 | 工作项3 |
| 状态 | ⏳ 待处理 |

### 工作项 10：P2-1 拍照解题模块
| 项目 | 内容 |
|------|------|
| 目的 | 拍照+框选+AI识别+结果展示 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 拍照解题完整功能 |
| 前置依赖 | 工作项4, 工作项5 |
| 状态 | ⏳ 待处理 |

### 工作项 11：P2-4 AI辅导模块
| 项目 | 内容 |
|------|------|
| 目的 | 对话式AI辅导 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | AI辅导对话页面 |
| 前置依赖 | 工作项5 |
| 状态 | ⏳ 待处理 |

### 工作项 12：P3-1 导航集成
| 项目 | 内容 |
|------|------|
| 目的 | Bottom Navigation+路由联通 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 可全流程导航的App |
| 前置依赖 | 工作项6~11全部完成 |
| 状态 | ⏳ 待处理 |

### 工作项 13：P3-3 单元测试
| 项目 | 内容 |
|------|------|
| 目的 | UT覆盖+截图测试 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 测试报告 |
| 前置依赖 | 工作项6~11全部完成 |
| 状态 | ⏳ 待处理 |

### 工作项 14：P3-4 性能优化
| 项目 | 内容 |
|------|------|
| 目的 | 启动速度+内存+帧率优化 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 优化报告 |
| 前置依赖 | 工作项12 |
| 状态 | ⏳ 待处理 |

### 工作项 15：P4-1~P4-3 发布准备
| 项目 | 内容 |
|------|------|
| 目的 | 适配+签名+上架 |
| 执行者 | mimo-v2.5-pro 编码 |
| Review | deepseek-v4-pro CodeReview |
| 交付物 | 正式上架包 |
| 前置依赖 | 工作项13, 工作项14 |
| 状态 | ⏳ 待处理 |

---

## 三、工作流程图

```
[工作项1] KaTeX验证 → deepseek Review
       ↓
[工作项2] P0-1脚手架 → deepseek Review
       ↓
[工作项3] P0-2骨架 → deepseek Review
       ↓
[工作项4] P1-1 Room → deepseek Review  \
       ↓                              \
[工作项5] P1-2 AI客户端 → deepseek Review  \
       ↓                                    \
[工作项6] P2-2知识点 → deepseek Review → [工作项12] P3-1导航集成
[工作项7] P2-3练习   → deepseek Review  ↗
[工作项8] P2-5收藏   → deepseek Review  ↗
[工作项9] P2-6设置   → deepseek Review  ↗
[工作项10] P2-1拍照 → deepseek Review  ↗
[工作项11] P2-4辅导 → deepseek Review  ↗
       ↓
[工作项13] P3-3测试 → deepseek Review
       ↓
[工作项14] P3-4优化 → deepseek Review
       ↓
[工作项15] P4-1~4-3发布
```

---

## 四、潜在风险清单

| 风险 | 级别 | 处理方式 |
|------|------|---------|
| KaTeX公式渲染兼容性差 | 🔴高 | **当前优先验证** |
| AI图像识别精度不足 | 🔴高 | 暂不处理，列入潜在风险 |
| Room数据库迁移数据丢失 | 🟡中 | Schema冻结，走add-migration |
| 网络超时用户感知差 | 🟡中 | loading动画+重试+离线缓存兜底 |
| 多分辨率适配工作量超出 | 🟡中 | 优先覆盖主流分辨率 |
| Play Store隐私政策不合规 | 🟢低 | 提前准备隐私政策文档 |
| AI API费用超预算 | 🟢低 | 设置每日调用限额告警 |

---

## 五、注意事项

1. **例题库暂留空白**，后续处理
2. **AI图像识别（P2-1）暂不实现**，列入潜在风险
3. 每个工作项完成后需要您确认，再进入下一个工作项
4. CodeReview发现的问题需修正后才能进入下一工作项