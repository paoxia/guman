package io.github.paoxia.guman.domain.chat;

import java.util.Objects;

/** 表示 Agent 在一次流式回复中产生的领域事件。 */
public class ChatStreamEvent {

    /*
     * 事件的领域语义类型。
     */
    private final ChatStreamEventType type;

    /*
     * 事件携带的文本或工具状态内容。
     */
    private final String content;

    /**
     * 创建流式聊天领域事件。
     *
     * @param type 非空的事件类型
     * @param content 事件内容，为 null 时转换为空字符串
     */
    public ChatStreamEvent(ChatStreamEventType type, String content) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.content = content == null ? "" : content;
    }

    /**
     * 获取事件类型。
     *
     * @return 非空的领域事件类型
     */
    public ChatStreamEventType getType() {
        return type;
    }

    /**
     * 获取事件内容。
     *
     * @return 非空的事件内容
     */
    public String getContent() {
        return content;
    }
}
