package io.github.paoxia.guman.interfaces.chat;

import io.github.paoxia.guman.application.chat.ChatCommand;
import io.github.paoxia.guman.domain.chat.ChatAttachment;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Web 端发起一次对话所需的请求参数。 */
public class ChatRequest {

    /*
     * 用户发送给 Agent 的原始文本。
     */
    @Size(max = 20000, message = "message must contain no more than 20000 characters")
    private String message;

    /*
     * 用于隔离不同访问者状态的用户标识。
     */
    private String userId;

    /*
     * 用于恢复同一段对话上下文的会话标识。
     */
    private String sessionId;

    /**
     * 创建供 JSON 反序列化使用的空请求对象，无入参。
     */
    public ChatRequest() {}

    /**
     * 创建聊天接口请求对象。
     *
     * @param message 用户发送的消息
     * @param userId 用户标识
     * @param sessionId 会话标识
     */
    public ChatRequest(String message, String userId, String sessionId) {
        this.message = message;
        this.userId = userId;
        this.sessionId = sessionId;
    }

    /**
     * 将接口请求转换为应用层命令，无入参。
     *
     * @return 经过领域规则校验的聊天命令
     */
    public ChatCommand toCommand() {
        return new ChatCommand(message, userId, sessionId);
    }

    /**
     * 将接口请求和已读取的附件转换为应用层命令。
     *
     * @param attachments 已在接口边界校验的附件
     * @return 经过领域规则校验的聊天命令
     */
    public ChatCommand toCommand(List<ChatAttachment> attachments) {
        return new ChatCommand(message, userId, sessionId, attachments);
    }

    /**
     * 获取用户消息。
     *
     * @return 用户输入的消息内容
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置用户消息，无返回值。
     *
     * @param message 用户输入的消息内容
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取用户标识。
     *
     * @return 用户标识
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户标识，无返回值。
     *
     * @param userId 用户标识
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取会话标识。
     *
     * @return 会话标识
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话标识，无返回值。
     *
     * @param sessionId 会话标识
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
