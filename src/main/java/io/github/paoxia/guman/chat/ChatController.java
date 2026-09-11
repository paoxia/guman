package io.github.paoxia.guman.chat;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** 将 AgentScope 的事件流转换为前端可直接消费的 SSE 事件。 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    /*
     * 记录 Agent 调用失败的完整服务端诊断信息。
     */
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /*
     * 全局共享的无状态 Agent，每次调用通过 RuntimeContext 隔离会话。
     */
    @Autowired
    private HarnessAgent agent;

    /**
     * 在指定用户会话中执行 Agent，并持续返回文本及工具事件。
     *
     * @param request 用户消息和隔离会话所需的标识
     * @return Agent 执行过程中产生的 SSE 事件流
     */
    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEvent>> stream(@RequestBody ChatRequest request) {
        RuntimeContext context =
                RuntimeContext.builder()
                        .userId(request.getUserId())
                        .sessionId(request.getSessionId())
                        .build();

        return agent.streamEvents(new UserMessage(request.getMessage()), context)
                .mapNotNull(this::toServerSentEvent)
                .onErrorResume(
                        error -> {
                            log.error(
                                    "Agent 流式调用失败，userId={}, sessionId={}",
                                    request.getUserId(),
                                    request.getSessionId(),
                                    error);
                            return Flux.just(
                                    serverSentEvent(
                                            new ChatEvent("error", "生成失败，请稍后重试。")));
                        });
    }

    /**
     * 将 AgentScope 原始事件转换为前端支持的 SSE 事件。
     *
     * @param event AgentScope 在执行过程中产生的原始事件
     * @return 对应的 SSE 事件；前端无需处理该事件时返回 null
     */
    private ServerSentEvent<ChatEvent> toServerSentEvent(AgentEvent event) {
        ChatEvent chatEvent =
                switch (event.getType()) {
                    case TEXT_BLOCK_DELTA ->
                            new ChatEvent(
                                    "text-delta", ((TextBlockDeltaEvent) event).getDelta());
                    case TOOL_CALL_START ->
                            new ChatEvent(
                                    "tool-start",
                                    ((ToolCallStartEvent) event).getToolCallName());
                    case TOOL_RESULT_END -> {
                        ToolResultEndEvent result = (ToolResultEndEvent) event;
                        yield new ChatEvent(
                                "tool-end",
                                result.getToolCallName() + ":" + result.getState());
                    }
                    case AGENT_END -> new ChatEvent("done", "");
                    default -> null;
                };
        return chatEvent == null ? null : serverSentEvent(chatEvent);
    }

    /**
     * 将聊天事件包装成带事件名的 Spring SSE 对象。
     *
     * @param event 待发送的聊天事件
     * @return 可由 WebFlux 序列化的 SSE 对象
     */
    private static ServerSentEvent<ChatEvent> serverSentEvent(ChatEvent event) {
        return ServerSentEvent.<ChatEvent>builder()
                .event(event.getType())
                .data(event)
                .build();
    }
}
