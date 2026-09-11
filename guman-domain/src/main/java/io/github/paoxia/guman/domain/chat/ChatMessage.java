package io.github.paoxia.guman.domain.chat;

/** 表示用户在一次对话中发送的消息。 */
public class ChatMessage {

    /*
     * 消息原始内容，保留用户输入格式但禁止为空。
     */
    private final String content;

    /**
     * 创建经过领域规则校验的聊天消息。
     *
     * @param content 用户输入的消息内容
     */
    public ChatMessage(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        this.content = content;
    }

    /**
     * 获取用户输入的消息内容。
     *
     * @return 非空的消息内容
     */
    public String getContent() {
        return content;
    }
}
