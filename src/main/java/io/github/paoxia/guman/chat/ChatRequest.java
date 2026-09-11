package io.github.paoxia.guman.chat;

/** Web 端发起一次对话所需的消息与会话标识。 */
public class ChatRequest {

    /*
     * 用户发送给 Agent 的原始文本。
     */
    private String message;

    /*
     * 用于隔离不同访问者状态的用户标识。
     */
    private String userId = "anonymous";

    /*
     * 用于恢复同一段对话上下文的会话标识。
     */
    private String sessionId = "default";

    /**
     * 创建供 JSON 反序列化使用的空请求对象，无入参。
     */
    public ChatRequest() {}

    /**
     * 创建并规范化一个聊天请求。
     *
     * @param message 用户发送的非空消息
     * @param userId 用户标识，为空时使用默认值
     * @param sessionId 会话标识，为空时使用默认值
     */
    public ChatRequest(String message, String userId, String sessionId) {
        setMessage(message);
        setUserId(userId);
        setSessionId(sessionId);
    }

    /**
     * 获取用户消息。
     *
     * @return 非空的用户消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置用户消息，无返回值。
     *
     * @param message 用户发送的非空消息
     */
    public void setMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        this.message = message;
    }

    /**
     * 获取用户标识。
     *
     * @return 已规范化的用户标识
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户标识，无返回值。
     *
     * @param userId 用户标识，为空时使用默认值
     */
    public void setUserId(String userId) {
        this.userId = defaultIfBlank(userId, "anonymous");
    }

    /**
     * 获取会话标识。
     *
     * @return 已规范化的会话标识
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话标识，无返回值。
     *
     * @param sessionId 会话标识，为空时使用默认值
     */
    public void setSessionId(String sessionId) {
        this.sessionId = defaultIfBlank(sessionId, "default");
    }

    /**
     * 将空字符串替换为指定默认值。
     *
     * @param value 待检查的字符串
     * @param defaultValue 原字符串为空时返回的默认值
     * @return 原始非空字符串或默认值
     */
    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
