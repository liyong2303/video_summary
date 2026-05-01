# VideoSum - B站视频内容再生产平台

一键粘贴B站视频链接，并行生成**总结、文章、学习卡片、小红书文案**四种内容形态。

## v1.0 功能

- **视频处理**：粘贴B站链接，自动提取字幕，AI 生成四种内容形态
- **流式输出**：SSE 实时推送生成进度，逐字呈现
- **用户体系**：注册/登录，Sa-Token 会话鉴权，BCrypt 密码加密
- **用量控制**：免费用户每日 3 次配额，定时 00:01 自动重置
- **历史记录**：按用户分页查看历史任务，支持重新生成、导出 Markdown
- **配置分离**：dev/prod profile 分离，敏感项通过环境变量注入

## 项目架构

```
┌──────────────┐     HTTP/SSE      ┌──────────────────┐    HTTP/Internal    ┌──────────────┐
│   Vue 3 前端  │ ──────────────▶  │  Spring Boot 后端  │ ─────────────────▶ │  FastAPI AI  │
│   :5173       │ ◀────────────── │    :8080           │ ◀───────────────── │  :8000       │
└──────────────┘                   └────────┬─────────┘                     └──────┬───────┘
                                            │                                       │
                                     ┌──────▼──────┐                        ┌──────▼──────┐
                                     │    MySQL     │                        │  DeepSeek   │
                                     │   :3306      │                        │    API      │
                                     └─────────────┘                        └─────────────┘
                                            │
                                     ┌──────▼──────┐
                                     │    Redis     │
                                     │   :6379      │
                                     └─────────────┘
```

**三个服务：**

| 服务 | 端口 | 职责 |
|------|------|------|
| `video-summary-frontend` | 5173 | Vue 3 前端，用户交互、SSE 流式展示 |
| `video-summary-service` | 8080 | Spring Boot 业务服务，用户鉴权、任务管理、B站API、SSE中继 |
| `video-summary-ai` | 8000 | FastAPI AI 管线，调用 DeepSeek 生成内容 |

**数据流：**
1. 用户注册/登录 → Sa-Token 签发会话令牌
2. 用户粘贴BV号 → 前端提交到 Java 后端（鉴权 + 配额检查）
3. Java 后端调 B站API 提取字幕 → 调 Python AI 管线生成内容
4. AI 管线：先串行生成总结，再并行生成文章/卡片/小红书文案
5. 结果通过 SSE 实时推送回前端，同时持久化到 MySQL

## 技术栈

### 前端
- **Vue 3** + TypeScript + Vite
- **Element Plus** UI 组件库
- **Vue Router** 路由管理 + 导航守卫
- **Axios** HTTP 请求（Token 自动注入 + 401 拦截）
- **EventSource** SSE 流式接收

### Java 后端
- **Spring Boot 3.2.5** (Java 17)
- **MyBatis-Plus 3.5.6** ORM
- **Spring Data Redis** 缓存/Pub-Sub
- **Sa-Token 1.38.0** 会话鉴权
- **BCrypt** 密码加密（spring-security-crypto）
- **Bean Validation** 参数校验
- **Hutool 5.8.27** 工具库
- **SseEmitter** 服务端推送
- **@Scheduled** 定时任务（配额重置）
- **@Transactional** 事务管理（配额原子操作）

### Python AI 服务
- **FastAPI** + Uvicorn
- **OpenAI SDK** 调用 DeepSeek API
- **Pydantic Settings** 配置管理
- **asyncio + wait_for** 管线并行执行 + 超时控制

### 基础设施
- **MySQL 8** 数据存储
- **Redis** 缓存与消息中继
- **DeepSeek API** 大模型推理

## 项目结构

```
video-summary/
├── video-summary-frontend/        # Vue 3 前端
│   ├── src/
│   │   ├── api/
│   │   │   └── auth.ts            # 认证 API + Axios 拦截器
│   │   ├── views/
│   │   │   ├── SubmitView.vue     # 提交页面 + 实时流式展示
│   │   │   ├── TaskResultView.vue # 结果详情页（复制/导出/重新生成）
│   │   │   ├── HistoryView.vue    # 历史记录页
│   │   │   ├── LoginView.vue      # 登录页
│   │   │   └── RegisterView.vue   # 注册页
│   │   ├── App.vue                # 根组件 + 导航栏 + 用量显示
│   │   ├── main.ts                # 入口（加载 auth 拦截器）
│   │   └── router/index.ts        # 路由配置 + 导航守卫
│   └── vite.config.ts             # Vite 配置（含 API 代理）
│
├── video-summary-service/         # Spring Boot 后端
│   └── src/main/java/com/videosummary/
│       ├── bilibili/              # B站API集成
│       │   ├── BilibiliApiClient.java   # 底层HTTP调用
│       │   ├── BilibiliVideoService.java # 视频信息+字幕提取
│       │   ├── WbiSignService.java      # WBI签名算法
│       │   └── WbiKeyService.java       # WBI密钥管理
│       ├── client/
│       │   └── PipelineClient.java      # AI管线调用客户端
│       ├── config/
│       │   ├── SaTokenConfig.java       # Sa-Token 拦截器 + BCrypt Bean
│       │   ├── MyBatisPlusConfig.java   # MyBatis-Plus 配置
│       │   └── RestTemplateConfig.java  # RestTemplate 配置
│       ├── controller/
│       │   ├── AuthController.java      # 注册/登录/当前用户 API
│       │   ├── VideoController.java     # 视频/任务/历史 API
│       │   ├── HealthController.java    # 健康检查
│       │   └── GlobalExceptionHandler.java # 全局异常处理
│       ├── service/
│       │   ├── AuthService.java         # 认证业务逻辑
│       │   ├── QuotaService.java        # 配额检查+递增+定时重置
│       │   ├── TaskService.java         # 任务管理业务逻辑
│       │   └── StreamService.java       # SSE流式推送
│       ├── entity/               # 数据实体（User/Task/TaskResult/DailyUsage）
│       ├── mapper/               # MyBatis-Plus Mapper
│       └── dto/                  # 请求/响应 DTO
│
├── video-summary-ai/             # FastAPI AI 服务
│   ├── app/
│   │   ├── main.py               # FastAPI 入口 + 路由
│   │   ├── pipeline.py           # AI 管线引擎（串行+并行+单步执行）
│   │   ├── llm.py                # DeepSeek API 调用封装
│   │   ├── prompts.py            # Prompt 模板
│   │   └── config.py             # 配置管理
│   └── requirements.txt
│
└── docs/                         # 项目文档
```

## 快速启动

### 环境要求

- Java 17+
- Python 3.10+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 1. 初始化数据库

```sql
-- 在 MySQL 中执行
source video-summary-service/src/main/resources/schema.sql;
```

### 2. 启动 Redis

```bash
redis-server
```

### 3. 启动 AI 服务

```bash
cd video-summary-ai

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env，填入 DeepSeek API Key

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. 启动 Java 后端

```bash
cd video-summary-service

# 开发环境（默认 profile=dev，使用本地 MySQL/Redis）
mvn spring-boot:run

# 生产环境
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:mysql://host:3306/video_summary \
DB_USERNAME=user \
DB_PASSWORD=pass \
REDIS_HOST=redis-host \
INTERNAL_SECRET=your-secret \
BILIBILI_COOKIE=your-cookie \
mvn spring-boot:run
```

### 5. 启动前端

```bash
cd video-summary-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:5173 ，注册账号后即可使用。

## API 接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册，返回 token |
| POST | `/api/auth/login` | 用户登录，返回 token |
| GET | `/api/auth/me` | 获取当前用户信息 + 用量 |

### 视频接口（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/video/submit` | 提交视频链接，创建任务 |
| GET | `/api/video/{taskId}` | 查询任务状态 |
| GET | `/api/video/{taskId}/results` | 获取任务结果 |
| GET | `/api/video/{taskId}/stream` | SSE 流式推送 |
| POST | `/api/video/{taskId}/cancel` | 取消任务 |
| GET | `/api/video/history` | 历史记录（分页） |
| POST | `/api/video/{taskId}/regenerate/{outputType}` | 重新生成指定类型 |
| GET | `/api/video/{taskId}/export` | 导出 Markdown |

## 配置说明

### Java 后端

`application.yml` — 公共配置：
```yaml
app:
  internal-secret: ${INTERNAL_SECRET:change-me-in-production}  # 服务间调用密钥
  ai-service:
    url: ${AI_SERVICE_URL:http://localhost:8000}                # AI 服务地址
  bilibili:
    cookie: ${BILIBILI_COOKIE:}                                  # B站 Cookie
  video:
    duration-limit-minutes: 30                                   # 视频时长限制

sa-token:
  token-name: token
  timeout: 604800    # 7 天过期
```

`application-prod.yml` — 生产环境所有值通过环境变量注入。

### AI 服务 (`.env`)

```env
DEEPSEEK_API_KEY=your-deepseek-api-key-here   # DeepSeek API Key
DEEPSEEK_BASE_URL=https://api.deepseek.com    # API 地址
DEEPSEEK_MODEL=deepseek-chat                  # 模型名称
INTERNAL_SECRET=change-me-in-production        # 服务间调用密钥
```

## 版本路线

| 版本 | 主题 | 状态 |
|------|------|------|
| v1.0 | MVP 上线：视频处理 + 用户体系 + 用量控制 | 已完成 |
| v1.1 | 内容在线编辑 + UX 打磨 | 计划中 |
| v1.2 | Prompt 自定义 + 输出模板 | 计划中 |
| v1.3 | 一键分发到其他平台 | 计划中 |
| v2.0 | 架构升级：Source Adapter + Pipeline DAG + Distribution Bus | 计划中 |
