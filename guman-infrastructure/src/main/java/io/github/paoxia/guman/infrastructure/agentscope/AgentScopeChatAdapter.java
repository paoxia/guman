package io.github.paoxia.guman.infrastructure.agentscope;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.github.paoxia.guman.application.chat.AgentChatPort;
import io.github.paoxia.guman.domain.chat.ChatMessage;
import io.github.paoxia.guman.domain.chat.ChatSession;
import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import io.github.paoxia.guman.domain.chat.ChatStreamEventType;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

/** 使用 AgentScope HarnessAgent 实现应用层定义的聊天输出端口。 */
@Component
@RequiredArgsConstructor
public class AgentScopeChatAdapter implements AgentChatPort {

    /*
     * 全局共享的无状态 Agent，每次调用通过 RuntimeContext 隔离会话。
     */
    private final HarnessAgent agent;

    /**
     * 调用 AgentScope 并将其事件转换为领域事件。
     *
     * @param message 已通过领域校验的用户消息
     * @param session 用于隔离 AgentScope 状态的聊天会话
     * @return AgentScope 执行过程中产生的聊天领域事件流
     */
    @Override
    public Publisher<ChatStreamEvent> stream(ChatMessage message, ChatSession session) {
        RuntimeContext context =
                RuntimeContext.builder()
                        .userId(session.getUserId())
                        .sessionId(session.getSessionId())
                        .build();

        return agent.streamEvents(new UserMessage(message.getContent()), context)
                .mapNotNull(this::toDomainEvent);
    }

    /**
     * 将 AgentScope 原始事件转换为领域事件。
     *
     * @param event AgentScope 在执行过程中产生的原始事件
     * @return 对应的聊天领域事件；上层无需处理该事件时返回 null
     */
    ChatStreamEvent toDomainEvent(AgentEvent event) {
        return switch (event.getType()) {
            case THINKING_BLOCK_DELTA ->
                    new ChatStreamEvent(
                            ChatStreamEventType.THINKING_DELTA,
                            ((ThinkingBlockDeltaEvent) event).getDelta());
            case TEXT_BLOCK_DELTA ->
                    new ChatStreamEvent(
                            ChatStreamEventType.TEXT_DELTA,
                            ((TextBlockDeltaEvent) event).getDelta());
            case TOOL_CALL_START ->
                    new ChatStreamEvent(
                            ChatStreamEventType.TOOL_STARTED,
                            ((ToolCallStartEvent) event).getToolCallName());
            case TOOL_RESULT_END -> {
                ToolResultEndEvent result = (ToolResultEndEvent) event;
                yield new ChatStreamEvent(
                        ChatStreamEventType.TOOL_COMPLETED,
                        result.getToolCallName() + ":" + result.getState());
            }
            case AGENT_END -> new ChatStreamEvent(ChatStreamEventType.COMPLETED, "");
            default -> null;
        };
    }
}
