# VideoSum

## What This Is

B站视频内容再生产平台。用户粘贴B站视频链接，系统提取字幕并通过AI管线生成4种内容形态：总结、文章、学习卡片、小红书文案。面向自媒体创作者和学习者，核心定位是"内容再生产工具"，不是内容消费效率工具。

## Core Value

一键粘贴链接，并行生成多种可发布内容。用户不是来省时间看视频的，是来把视频内容变成自己的内容去分发。

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] 用户可粘贴B站链接或BV号，系统自动提取视频信息和字幕
- [ ] 系统通过4步管线（总结→文章/卡片/小红书并行）生成4种内容
- [ ] 内容通过SSE流式实时输出，首token 5秒内出现
- [ ] 用户可一键复制结果到剪贴板，可导出Markdown文件
- [ ] 免费用户3个视频/天，付费用户无限制（Phase 2）
- [ ] 基础用户体系：注册/登录/用量控制
- [ ] 历史记录：查看已处理的视频和结果
- [ ] 无字幕视频返回友好提示，不报错
- [ ] 视频封面/标题自动提取展示

### Out of Scope

- ASR语音识别（无字幕视频暂不支持） — MVP阶段不增加复杂度
- 付费订阅/支付 — Phase 2，需要企业资质
- 自定义输出模板 — Phase 2
- 多模型切换（Qwen/DeepSeek-R1） — Phase 2
- 批量处理（UP主/收藏夹） — 需要任务队列+速率控制，Phase 2
- 总结中保留时间戳 — nice-to-have，prompt调整即可，不急
- 总结质量评分 — 增加LLM调用成本和延迟
- 输出A/B对比 — 成本翻倍，MVP太早
- Chrome扩展 — Phase 3

## Context

- B站字幕API（/x/player/wbi/v2）为逆向工程成果，wbi签名机制随时可能变更，需监控和快速适配
- 三层架构：Vue 3 前端 + Java (Spring Boot) 业务服务 + Python (FastAPI) AI管线服务
- AI模型使用国产低成本模型（DeepSeek V3 / Qwen），单视频成本约0.03元
- SSE流式输出通过Redis Pub/Sub中继（Python→Redis→Java→Vue）
- 管线步骤1（总结）必须先完成，步骤2-4（文章/卡片/小红书）可并行
- 免费用户管线仍执行全部4步（并行省不了成本），但前端只展示总结+文章
- UI设计已完成高保真版（wireframe-v2.html），品牌色#00a1d6渐变

## Constraints

- **Tech Stack**: Vue 3 + Spring Boot + FastAPI — 用户擅长Java+Vue，Python用于LLM生态对接
- **Timeline**: 1-2周出MVP — 快速上线，边用边改
- **AI Cost**: 单视频<0.05元 — 使用国产低成本模型，免费用户成本可被付费用户覆盖
- **Performance**: 首token 5秒内，总结15秒内，完整4种输出30秒内 — 流式输出体验
- **Deployment**: 云服务器部署 — MVP阶段
- **B站 API**: 依赖逆向工程API，随时可能失效 — 需要监控+隔离+降级方案
- **MVP Scope**: 不含支付、模板、多模型、ASR — 严格限定Phase 1范围

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 三层架构（Vue+Java+Python） | Java做业务擅长，Python做LLM生态最好，各取所长 | — Pending |
| Redis Pub/Sub中继SSE | Java不适合做SSE代理（线程模型），Pub/Sub解耦两个服务 | — Pending |
| 免费用户管线仍执行4步 | 避免"执行/不执行"逻辑分支，并行步骤省不了成本 | — Pending |
| DeepSeek V3作为默认模型 | 成本低（0.03元/视频），OpenAI SDK兼容 | — Pending |
| 配置化额度/价格 | 运营可动态调整，不改代码 | — Pending |
| 云服务器部署（非Docker Compose） | MVP阶段直接云部署更快 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-26 after initialization*
