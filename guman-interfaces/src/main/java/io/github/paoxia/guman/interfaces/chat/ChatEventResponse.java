package io.github.paoxia.guman.interfaces.chat;

import io.github.paoxia.guman.domain.chat.ChatStreamEvent;

/** 向 Web 端发送的稳定 SSE 事件结构。 */
public class ChatEventResponse {

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
    public ChatEventResponse(String type, String content) {
        this.type = type;
        this.content = content;
    }

    /**
     * 将领域事件转换为稳定的接口响应。
     *
     * @param event 待转换的聊天领域事件
     * @return 包含前端事件名和内容的响应对象
     */
    public static ChatEventResponse from(ChatStreamEvent event) {
        String responseType =
                switch (event.getType()) {
                    case TEXT_DELTA -> "text-delta";
                    case TOOL_STARTED -> "tool-start";
                    case TOOL_COMPLETED -> "tool-end";
                    case COMPLETED -> "done";
                };
        return new ChatEventResponse(responseType, event.getContent());
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
