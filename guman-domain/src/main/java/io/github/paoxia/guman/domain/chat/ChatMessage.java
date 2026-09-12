package io.github.paoxia.guman.domain.chat;

import java.util.List;

/** 表示用户在一次对话中发送的消息。 */
public class ChatMessage {

    /*
     * 消息原始内容，保留用户输入格式但禁止为空。
     */
    private final String content;

    /*
     * 与消息同时交给模型处理的附件，集合本身不可变。
     */
    private final List<ChatAttachment> attachments;

    /**
     * 创建经过领域规则校验的聊天消息。
     *
     * @param content 用户输入的消息内容
     */
    public ChatMessage(String content) {
        this(content, List.of());
    }

    /**
     * 创建可同时包含文本和附件的聊天消息。
     *
     * @param content 用户输入的文本内容
     * @param attachments 用户选择的附件
     */
    public ChatMessage(String content, List<ChatAttachment> attachments) {
        List<ChatAttachment> safeAttachments =
                attachments == null ? List.of() : List.copyOf(attachments);
        String safeContent = content == null ? "" : content;
        if (safeContent.isBlank() && safeAttachments.isEmpty()) {
            throw new IllegalArgumentException("message or attachment must be provided");
        }
        this.content = safeContent;
        this.attachments = safeAttachments;
    }

    /**
     * 获取用户输入的消息内容。
     *
     * @return 非空的消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取消息携带的附件。
     *
     * @return 不可变附件列表
     */
    public List<ChatAttachment> getAttachments() {
        return attachments;
    }
}
