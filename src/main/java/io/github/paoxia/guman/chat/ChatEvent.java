package io.github.paoxia.guman.chat;

/** 向 Web 端发送的稳定 SSE 事件结构。 */
public class ChatEvent {

    /*
     * 前端用于分发事件的稳定类型名称。
     */
    private final String type;

    /*
     * 本次事件携带的文本或状态内容。
     */
    private final String content;

    /**
     * 创建一个发送给 Web 端的聊天事件。
     *
     * @param type SSE 事件类型
     * @param content 事件携带的内容
     */
    public ChatEvent(String type, String content) {
        this.type = type;
        this.content = content;
    }

    /**
     * 获取事件类型。
     *
     * @return SSE 事件类型
     */
    public String getType() {
        return type;
    }

    /**
     * 获取事件内容。
     *
     * @return 事件携带的内容
     */
    public String getContent() {
        return content;
    }
}
