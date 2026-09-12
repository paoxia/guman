package io.github.paoxia.guman.application.chat;

import io.github.paoxia.guman.domain.chat.ChatMessage;
import io.github.paoxia.guman.domain.chat.ChatSession;

/** 封装流式聊天用例所需的领域输入。 */
public class ChatCommand {

    /*
     * 已通过领域规则校验的用户消息。
     */
    private final ChatMessage message;

    /*
     * 已完成默认值处理的会话上下文。
     */
    private final ChatSession session;

    /**
     * 根据外部输入创建流式聊天命令。
     *
     * @param message 用户发送的消息内容
     * @param userId 用户标识，为空时使用领域默认值
     * @param sessionId 会话标识，为空时使用领域默认值
     */
    public ChatCommand(String message, String userId, String sessionId) {
        this.message = new ChatMessage(message);
        this.session = new ChatSession(userId, sessionId);
    }

    /**
     * 获取经过校验的用户消息。
     *
     * @return 用户消息领域对象
     */
    public ChatMessage getMessage() {
        return message;
    }

    /**
     * 获取经过规范化的聊天会话。
     *
     * @return 聊天会话领域对象
     */
    public ChatSession getSession() {
        return session;
    }
}
