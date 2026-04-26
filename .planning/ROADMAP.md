# Roadmap: VideoSum

**Created:** 2026-04-26
**Granularity:** Coarse (3-5 phases, fast MVP)
**Total v1 Requirements:** 29

## Phase 1: Project Skeleton + B站字幕提取

**Goal:** 搭建三个服务项目骨架，实现B站字幕提取核心能力。用户可以提交BV号，系统返回视频信息和字幕文本。

**Requirements:** BILI-01, BILI-02, BILI-03, BILI-04, BILI-05, BILI-06, TASK-01, TASK-02, TASK-04

**Plans:** 3 plans

Plans:
- [ ] 01-01-PLAN.md — Three-service project skeleton + database schema + health checks
- [ ] 01-02-PLAN.md — Bilibili wbi signature + video/subtitle extraction module
- [ ] 01-03-PLAN.md — Task management API + BV号 parsing + Vue frontend

**Success Criteria:**
1. Vue 3 前端项目可运行，有基础页面布局
2. Spring Boot 项目可运行，有健康检查端点
3. FastAPI 项目可运行，有健康检查端点
4. 提交有效BV号可返回视频标题、封面、时长
5. 提交有字幕的BV号可返回字幕文本
6. 提交无字幕的BV号返回友好错误提示
7. 任务记录可创建、状态可查询

**Depends on:** None

---

## Phase 2: AI管线

**Goal:** 实现4步AI管线，从字幕生成4种内容形态。步骤1串行，步骤2-4并行。通过Redis Pub/Sub推送流式chunk。

**Requirements:** PIPE-01, PIPE-02, PIPE-03, PIPE-04, PIPE-05, PIPE-06, PIPE-07, PIPE-08

**Success Criteria:**
1. 提交字幕文本到Python管线可生成总结
2. 总结完成后自动并行生成文章/卡片/小红书文案
3. 每步流式输出chunk通过Redis Pub/Sub推送
4. 长字幕(>8K token)先压缩再处理
5. 单步失败不阻塞其他步骤
6. 30分钟视频18秒内完成

**Depends on:** Phase 1

---

## Phase 3: SSE流式输出 + 任务取消

**Goal:** 前端通过SSE实时接收管线输出，支持断线重连和任务取消。完整实现Redis→Java→Vue的数据通道。

**Requirements:** SSE-01, SSE-02, SSE-03, SSE-04, SSE-05, TASK-03

**Success Criteria:**
1. Vue EventSource连接可接收实时流式输出
2. 首token 5秒内出现
3. SSE断线后自动重连，不丢失已完成步骤
4. 用户可取消正在执行的任务
5. SSE事件格式正确(step_start/chunk/step_complete/pipeline_complete/error)

**Depends on:** Phase 2

---

## Phase 4: 用户体系 + 用量控制

**Goal:** 实现注册/登录/用量控制。免费用户3个视频/天，可配置。前端展示用量。

**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, QUOTA-01, QUOTA-02, QUOTA-03, QUOTA-04

**Success Criteria:**
1. 用户可注册并登录获取JWT token
2. 登录状态跨浏览器刷新保持
3. 免费用户每日3次额度，超限返回错误
4. 额度每日0点重置
5. 前端显示当日剩余额度
6. 免费用户前端仅展示总结+文章两个tab

**Depends on:** Phase 3

---

## Phase 5: 结果交互 + 历史记录

**Goal:** 实现一键复制、Markdown导出、结果tab切换、历史记录查看。完成MVP全部功能。

**Requirements:** RES-01, RES-02, RES-03, RES-04, HIST-01, HIST-02, HIST-03

**Success Criteria:**
1. 点击复制按钮可复制结果到剪贴板
2. 点击导出按钮可下载Markdown文件
3. 结果以4个tab展示(总结/文章/卡片/小红书)
4. 重新生成按钮可重跑单个输出类型
5. 历史记录列表展示已处理视频
6. 点击历史项可查看完整结果

**Depends on:** Phase 4

**UI hint:** yes

---
