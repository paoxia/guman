# Guman

Guman 是一个基于 [Spring Boot](https://spring.io/projects/spring-boot/) 与 [AgentScope Java 2](https://java.agentscope.io/) 的 human-agent web gateway。

项目提供响应式聊天界面和 SSE 流式 API，并使用 `HarnessAgent` 管理会话、工作区、记忆及工具调用。

## 环境要求

- JDK 21
- Maven 3.9+
- Ollama，或提供 Chat Completions 接口的 OpenAI-compatible 服务

## 快速开始

本地使用 Ollama 时，可以用一条命令安装并启动 Ollama、拉取 `qwen3.5:9b`，然后启动 Guman：

```bash
make start
```

如果 Ollama 已准备好，也可以直接构建并启动应用：

```bash
make run
```

只运行模型并进入 Ollama 交互会话，不启动 Guman：

```bash
make ollama-run
```

浏览器访问 <http://localhost:8080> 即可使用聊天界面，健康检查地址为 <http://localhost:8080/actuator/health>。

模型配置位于 `guman.model.configurations`，`guman.model.active` 决定其中哪一套配置生效。仓库同时提供 `ollama-qwen` 和 `openai-compatible` 两套配置；当前选择可以通过环境变量覆盖：

```bash
export GUMAN_ACTIVE_MODEL="ollama-qwen"
export OLLAMA_MODEL="qwen3.5:9b"
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_THINKING_ENABLED="true"
export AGENTSCOPE_WORKSPACE=".agentscope/workspace"
export SERVER_PORT="8080"
```

切换到 OpenAI-compatible API 时，无需修改 Java 代码：

```bash
export GUMAN_ACTIVE_MODEL="openai-compatible"
export OPENAI_MODEL="your-model-name"
export OPENAI_BASE_URL="https://llm.example.com"
export OPENAI_ENDPOINT_PATH="/v1/chat/completions"
export OPENAI_API_KEY="your-api-key"
make run
```

`OPENAI_API_KEY` 仅通过环境变量提供；不需要鉴权的本地兼容服务可以留空。不同兼容服务对 structured output 和工具调用的实现存在差异，可以通过 `OPENAI_NATIVE_STRUCTURED_OUTPUT` 与 `OPENAI_NATIVE_STRUCTURED_OUTPUT_WITH_TOOLS` 调整，二者默认关闭。

也可以在 `application.yml` 的 `guman.model.configurations` 下继续增加命名配置。同一 Provider 可以配置多套实例，例如不同的 Ollama 模型或不同的 OpenAI-compatible 地址；将 `guman.model.active` 指向相应名称后，重启应用即可切换。未被选择的配置不会创建模型连接。

`qwen3.5:9b` 支持 thinking。默认的 `ollama-qwen` 配置通过 `thinking-enabled: true` 显式开启该能力，也可以设置 `OLLAMA_THINKING_ENABLED=false` 关闭。思考内容使用独立事件传输并显示在回答上方的“思考过程”面板中，不会混入最终回答。

Web 端会为浏览器生成稳定的用户 ID，并为每个对话创建独立 session ID。AgentScope 运行时产生的会话与记忆文件不会提交到 Git；人格配置位于 `.agentscope/workspace/AGENTS.md`。

## 流式接口

```bash
curl -N http://localhost:8080/api/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好","userId":"alice","sessionId":"demo"}'
```

接口返回 `text/event-stream`，目前会发送 `thinking-delta`、`text-delta`、`tool-start`、`tool-end`、`done` 和 `error` 事件。`thinking-delta` 只在当前模型启用并返回 thinking 内容时出现。

## 项目结构

```text
.
├── .agentscope/workspace/AGENTS.md       # Agent 人格配置
├── guman-domain/                         # 领域对象、值对象与领域事件
├── guman-application/                    # 应用用例与输入/输出端口
├── guman-infrastructure/                 # AgentScope 配置与输出端口实现
├── guman-interfaces/                     # WebFlux Controller、SSE DTO 与静态页面
├── guman-bootstrap/                      # Spring Boot 入口与运行配置
├── AGENTS.md                             # 项目开发与 DDD 边界规约
├── pom.xml                               # Maven 聚合父工程
└── README.md
```

模块依赖方向如下：

```text
guman-domain
      ↑
guman-application
      ↑            ↑
guman-infrastructure  guman-interfaces
          ↑            ↑
          └─ guman-bootstrap ─┘
```

领域层不依赖 Spring 或 AgentScope；应用层通过 `AgentChatPort` 描述外部 Agent 能力，基础设施层负责 AgentScope 适配，接口层只调用 `StreamChatUseCase`。最终由启动模块完成所有模块装配。
