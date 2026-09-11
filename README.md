# Guman

Guman 是一个基于 [Spring Boot](https://spring.io/projects/spring-boot/) 与 [AgentScope Java 2](https://java.agentscope.io/) 的 human-agent web gateway。

项目提供响应式聊天界面和 SSE 流式 API，并使用 `HarnessAgent` 管理会话、工作区、记忆及工具调用。

## 环境要求

- JDK 21
- Maven 3.9+
- DashScope API Key

## 快速开始

```bash
export DASHSCOPE_API_KEY="your-api-key"
mvn verify
mvn spring-boot:run
```

浏览器访问 <http://localhost:8080> 即可使用聊天界面，健康检查地址为 <http://localhost:8080/actuator/health>。

默认模型为 `dashscope:qwen-plus`。可以通过环境变量覆盖运行配置：

```bash
export DASHSCOPE_MODEL="qwen-plus"
export AGENTSCOPE_WORKSPACE=".agentscope/workspace"
export SERVER_PORT="8080"
```

Web 端会为浏览器生成稳定的用户 ID，并为每个对话创建独立 session ID。AgentScope 运行时产生的会话与记忆文件不会提交到 Git；人格配置位于 `.agentscope/workspace/AGENTS.md`。

## 流式接口

```bash
curl -N http://localhost:8080/api/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好","userId":"alice","sessionId":"demo"}'
```

接口返回 `text/event-stream`，目前会发送 `text-delta`、`tool-start`、`tool-end`、`done` 和 `error` 事件。

## 项目结构

```text
.
├── .agentscope/workspace/AGENTS.md  # Agent 人格配置
├── src/main/java/                   # Spring Boot 与流式 API
├── src/main/resources/static/       # Web 聊天界面
├── src/main/resources/application.yml
├── pom.xml                          # Maven 与 AgentScope 依赖
└── README.md
```
