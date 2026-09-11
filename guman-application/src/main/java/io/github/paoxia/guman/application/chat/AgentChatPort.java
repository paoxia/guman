package io.github.paoxia.guman.application.chat;

import io.github.paoxia.guman.domain.chat.ChatMessage;
import io.github.paoxia.guman.domain.chat.ChatSession;
import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import org.reactivestreams.Publisher;

/** 定义应用层调用外部 Agent 能力所需的输出端口。 */
public interface AgentChatPort {

    /**
     * 使用外部 Agent 在指定会话中生成流式回复。
     *
     * @param message 已通过领域校验的用户消息
     * @param session 用于隔离 Agent 状态的聊天会话
     * @return 外部 Agent 产生并转换后的领域事件流
     */
    Publisher<ChatStreamEvent> stream(ChatMessage message, ChatSession session);
}
