# Requirements: VideoSum

**Defined:** 2026-04-26
**Core Value:** 一键粘贴链接，并行生成多种可发布内容

## v1 Requirements

### B站字幕提取

- [ ] **BILI-01**: User can paste B站 BV号 or full URL and system resolves to video info
- [ ] **BILI-02**: System extracts video title, duration, and cover image via /x/web-interface/view API
- [ ] **BILI-03**: System extracts subtitles via /x/player/wbi/v2 API with wbi signature
- [ ] **BILI-04**: System validates video duration against configured limit (free: 30min)
- [ ] **BILI-05**: System returns friendly error for videos without subtitles
- [ ] **BILI-06**: wbi signature module is isolated for easy updates when API changes

### AI管线

- [ ] **PIPE-01**: System generates summary from subtitle text via LLM (Step 1, must complete first)
- [ ] **PIPE-02**: System generates article from summary via LLM (Step 2, parallel with 3-4)
- [ ] **PIPE-03**: System generates learning cards from summary via LLM (Step 3, parallel with 2,4)
- [ ] **PIPE-04**: System generates Xiaohongshu post from summary via LLM (Step 4, parallel with 2,3)
- [ ] **PIPE-05**: Steps 2-4 execute in parallel after Step 1 completes
- [ ] **PIPE-06**: Long subtitles (>8K token) are compressed via LLM before pipeline, with truncation fallback
- [ ] **PIPE-07**: Each step streams tokens via Redis Pub/Sub for real-time SSE output
- [ ] **PIPE-08**: Individual step failure does not block other steps (partial completion supported)

### SSE流式输出

- [ ] **SSE-01**: Frontend receives streaming output via EventSource (SSE)
- [ ] **SSE-02**: First token appears within 5 seconds of submission
- [ ] **SSE-03**: Java relays Python pipeline chunks via Redis Pub/Sub subscription
- [ ] **SSE-04**: Vue auto-reconnects on disconnect with Last-Event-ID, Java sends completed results first
- [ ] **SSE-05**: SSE events include step_start, chunk, step_complete, pipeline_complete, error types

### 用户体系

- [ ] **AUTH-01**: User can register with username/email and password
- [ ] **AUTH-02**: User can login and receive JWT token (Sa-Token)
- [ ] **AUTH-03**: User session persists across browser refresh
- [ ] **AUTH-04**: User can view their profile and daily usage count

### 用量控制

- [ ] **QUOTA-01**: Free users limited to 3 videos per day (configurable)
- [ ] **QUOTA-02**: System checks quota before processing and returns limit error if exceeded
- [ ] **QUOTA-03**: Daily usage counter resets at midnight
- [ ] **QUOTA-04**: Free users see only summary + article in frontend (pipeline still executes all 4 steps)

### 结果交互

- [ ] **RES-01**: User can copy any result to clipboard with one click
- [ ] **RES-02**: User can export results as Markdown file
- [ ] **RES-03**: Results are displayed in tabs (总结/文章/学习卡片/小红书文案)
- [ ] **RES-04**: User can request regeneration of any output type

### 历史记录

- [ ] **HIST-01**: User can view list of previously processed videos
- [ ] **HIST-02**: History shows video title, cover, processing time, and output types
- [ ] **HIST-03**: User can click history item to view full results

### 任务管理

- [ ] **TASK-01**: System creates task record with pending status on submission
- [ ] **TASK-02**: System deduplicates tasks (same user + same BV号 returns existing taskId)
- [ ] **TASK-03**: User can cancel a running task
- [ ] **TASK-04**: Task status transitions: pending → processing → completed/partially_completed/failed/cancelled

## v2 Requirements

### 付费订阅

- **PAY-01**: User can subscribe monthly (19元/月) via WeChat/Alipay
- **PAY-02**: Paid users have unlimited daily quota
- **PAY-03**: Paid users see all 4 output types
- **PAY-04**: Paid users can use custom output templates

### 模板系统

- **TMPL-01**: User can create custom output templates
- **TMPL-02**: User can share templates publicly
- **TMPL-03**: System provides preset templates

### 多模型

- **MODL-01**: System supports switching between DeepSeek/Qwen/R1 models
- **MODL-02**: Paid users can select model per task

## Out of Scope

| Feature | Reason |
|---------|--------|
| ASR (speech recognition) | High complexity, not core to MVP, deferred to Phase 3 |
| Batch processing (UP主/收藏夹) | Requires task queue + rate control, deferred to Phase 2 |
| Chrome extension | Distribution channel, not core product, deferred to Phase 3 |
| Summary timestamps | Nice-to-have, prompt adjustment, not blocking |
| Quality scoring | Adds LLM cost and latency |
| A/B comparison | Doubles cost |
| Config admin UI | Phase 2, YAML/env config sufficient for MVP |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| BILI-01 | Phase 1 | Pending |
| BILI-02 | Phase 1 | Pending |
| BILI-03 | Phase 1 | Pending |
| BILI-04 | Phase 1 | Pending |
| BILI-05 | Phase 1 | Pending |
| BILI-06 | Phase 1 | Pending |
| PIPE-01 | Phase 2 | Pending |
| PIPE-02 | Phase 2 | Pending |
| PIPE-03 | Phase 2 | Pending |
| PIPE-04 | Phase 2 | Pending |
| PIPE-05 | Phase 2 | Pending |
| PIPE-06 | Phase 2 | Pending |
| PIPE-07 | Phase 2 | Pending |
| PIPE-08 | Phase 2 | Pending |
| SSE-01 | Phase 3 | Pending |
| SSE-02 | Phase 3 | Pending |
| SSE-03 | Phase 3 | Pending |
| SSE-04 | Phase 3 | Pending |
| SSE-05 | Phase 3 | Pending |
| AUTH-01 | Phase 4 | Pending |
| AUTH-02 | Phase 4 | Pending |
| AUTH-03 | Phase 4 | Pending |
| AUTH-04 | Phase 4 | Pending |
| QUOTA-01 | Phase 4 | Pending |
| QUOTA-02 | Phase 4 | Pending |
| QUOTA-03 | Phase 4 | Pending |
| QUOTA-04 | Phase 4 | Pending |
| RES-01 | Phase 5 | Pending |
| RES-02 | Phase 5 | Pending |
| RES-03 | Phase 5 | Pending |
| RES-04 | Phase 5 | Pending |
| HIST-01 | Phase 5 | Pending |
| HIST-02 | Phase 5 | Pending |
| HIST-03 | Phase 5 | Pending |
| TASK-01 | Phase 1 | Pending |
| TASK-02 | Phase 1 | Pending |
| TASK-03 | Phase 3 | Pending |
| TASK-04 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 29 total
- Mapped to phases: 29
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-26*
*Last updated: 2026-04-26 after initial definition*
