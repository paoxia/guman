package io.github.paoxia.guman.infrastructure.agentscope;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.github.paoxia.guman.application.chat.AgentChatPort;
import io.github.paoxia.guman.domain.chat.ChatAttachment;
import io.github.paoxia.guman.domain.chat.ChatMessage;
import io.github.paoxia.guman.domain.chat.ChatSession;
import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import io.github.paoxia.guman.domain.chat.ChatStreamEventType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** 使用 AgentScope HarnessAgent 实现应用层定义的聊天输出端口。 */
@Component
@RequiredArgsConstructor
public class AgentScopeChatAdapter implements AgentChatPort {

    private static final int MAX_PDF_PAGES = 100;
    private static final int MAX_EXTRACTED_TEXT_CHARACTERS = 200_000;

    /*
     * 全局共享的无状态 Agent，每次调用通过 RuntimeContext 隔离会话。
     */
    private final HarnessAgent agent;

    /**
     * 调用 AgentScope 并将其事件转换为领域事件。
     *
     * @param message 已通过领域校验的用户消息
     * @param session 用于隔离 AgentScope 状态的聊天会话
     * @return AgentScope 执行过程中产生的聊天领域事件流
     */
    @Override
    public Publisher<ChatStreamEvent> stream(ChatMessage message, ChatSession session) {
        RuntimeContext context =
                RuntimeContext.builder()
                        .userId(session.getUserId())
                        .sessionId(session.getSessionId())
                        .build();

        return Mono.fromCallable(() -> toUserMessage(message))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(userMessage -> agent.streamEvents(userMessage, context))
                .mapNotNull(this::toDomainEvent);
    }

    /**
     * 将领域消息转换为 AgentScope 可消费的多模态用户消息。
     *
     * @param message 包含文本和附件的领域消息
     * @return 由文本块与图片块组成的 AgentScope 用户消息
     */
    UserMessage toUserMessage(ChatMessage message) {
        List<ContentBlock> blocks = new ArrayList<>();
        if (!message.getContent().isBlank()) {
            blocks.add(TextBlock.builder().text(message.getContent()).build());
        }
        for (ChatAttachment attachment : message.getAttachments()) {
            if (attachment.isImage()) {
                blocks.add(toImageBlock(attachment));
            } else {
                blocks.add(toAttachmentTextBlock(attachment));
            }
        }
        return new UserMessage(blocks);
    }

    /**
     * 将图片附件转换为 Ollama 可识别的 Base64 图片块。
     *
     * @param attachment 已校验的图片附件
     * @return AgentScope 图片内容块
     */
    private ImageBlock toImageBlock(ChatAttachment attachment) {
        Base64Source source =
                Base64Source.builder()
                        .mediaType(attachment.getMediaType())
                        .data(Base64.getEncoder().encodeToString(attachment.getContent()))
                        .build();
        return ImageBlock.builder().source(source).build();
    }

    /**
     * 将文本或 PDF 附件转换为带文件边界的文本块。
     *
     * @param attachment 已校验的文档附件
     * @return 包含文件名和文件内容的 AgentScope 文本块
     */
    private TextBlock toAttachmentTextBlock(ChatAttachment attachment) {
        String content =
                attachment.isPdf()
                        ? extractPdfText(attachment)
                        : decodeUtf8Text(attachment);
        return TextBlock.builder()
                .text(
                        "<attachment name=\""
                                + attachment.getName()
                                + "\">\n"
                                + content
                                + "\n</attachment>")
                .build();
    }

    /**
     * 使用严格 UTF-8 解码文本附件，避免把二进制内容错误传给模型。
     *
     * @param attachment 待解码的文本附件
     * @return UTF-8 文本内容
     */
    private String decodeUtf8Text(ChatAttachment attachment) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(attachment.getContent()))
                    .toString();
        } catch (CharacterCodingException error) {
            throw new IllegalArgumentException(
                    "text attachment must use UTF-8 encoding: " + attachment.getName(), error);
        }
    }

    /**
     * 从文本型 PDF 中提取受长度限制的文本。
     *
     * @param attachment 待解析的 PDF 附件
     * @return 提取出的 PDF 文本
     */
    private String extractPdfText(ChatAttachment attachment) {
        try (PDDocument document = Loader.loadPDF(attachment.getContent())) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                        "encrypted PDF is not supported: " + attachment.getName());
            }
            if (document.getNumberOfPages() > MAX_PDF_PAGES) {
                throw new IllegalArgumentException(
                        "PDF exceeds the 100 page limit: " + attachment.getName());
            }
            String extractedText = new PDFTextStripper().getText(document).strip();
            if (extractedText.isEmpty()) {
                throw new IllegalArgumentException(
                        "PDF contains no extractable text: " + attachment.getName());
            }
            if (extractedText.length() > MAX_EXTRACTED_TEXT_CHARACTERS) {
                return extractedText.substring(0, MAX_EXTRACTED_TEXT_CHARACTERS)
                        + "\n[PDF 文本超过 200000 字符，后续内容已截断]";
            }
            return extractedText;
        } catch (IOException error) {
            throw new IllegalArgumentException(
                    "unable to read PDF attachment: " + attachment.getName(), error);
        }
    }

    /**
     * 将 AgentScope 原始事件转换为领域事件。
     *
     * @param event AgentScope 在执行过程中产生的原始事件
     * @return 对应的聊天领域事件；上层无需处理该事件时返回 null
     */
    ChatStreamEvent toDomainEvent(AgentEvent event) {
        return switch (event.getType()) {
            case THINKING_BLOCK_DELTA ->
                    new ChatStreamEvent(
                            ChatStreamEventType.THINKING_DELTA,
                            ((ThinkingBlockDeltaEvent) event).getDelta());
            case TEXT_BLOCK_DELTA ->
                    new ChatStreamEvent(
                            ChatStreamEventType.TEXT_DELTA,
                            ((TextBlockDeltaEvent) event).getDelta());
            case TOOL_CALL_START ->
                    new ChatStreamEvent(
                            ChatStreamEventType.TOOL_STARTED,
                            ((ToolCallStartEvent) event).getToolCallName());
            case TOOL_RESULT_END -> {
                ToolResultEndEvent result = (ToolResultEndEvent) event;
                yield new ChatStreamEvent(
                        ChatStreamEventType.TOOL_COMPLETED,
                        result.getToolCallName() + ":" + result.getState());
            }
            case AGENT_END -> new ChatStreamEvent(ChatStreamEventType.COMPLETED, "");
            default -> null;
        };
    }
}
