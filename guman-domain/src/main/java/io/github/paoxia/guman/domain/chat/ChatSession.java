package io.github.paoxia.guman.domain.chat;

/** 表示一次按用户和会话双重隔离的聊天上下文。 */
public class ChatSession {

    /*
     * 未提供用户标识时使用的稳定默认值。
     */
    private static final String DEFAULT_USER_ID = "anonymous";

    /*
     * 未提供会话标识时使用的稳定默认值。
     */
    private static final String DEFAULT_SESSION_ID = "default";

    /*
     * 用于隔离不同访问者状态的用户标识。
     */
    private final String userId;

    /*
     * 用于恢复同一段对话上下文的会话标识。
     */
    private final String sessionId;

    /**
     * 创建聊天会话，并为空白标识应用默认值。
     *
     * @param userId 用户标识，为空时使用 anonymous
     * @param sessionId 会话标识，为空时使用 default
     */
    public ChatSession(String userId, String sessionId) {
        this.userId = defaultIfBlank(userId, DEFAULT_USER_ID);
        this.sessionId = defaultIfBlank(sessionId, DEFAULT_SESSION_ID);
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
     * 获取会话标识。
     *
     * @return 已规范化的会话标识
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 将空白字符串替换为指定默认值。
     *
     * @param value 待检查的字符串
     * @param defaultValue 原字符串为空时返回的默认值
     * @return 原始非空字符串或默认值
     */
    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
