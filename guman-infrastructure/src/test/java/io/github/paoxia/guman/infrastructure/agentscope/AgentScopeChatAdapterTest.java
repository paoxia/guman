package io.github.paoxia.guman.infrastructure.agentscope;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.github.paoxia.guman.domain.chat.ChatAttachment;
import io.github.paoxia.guman.domain.chat.ChatMessage;
import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import io.github.paoxia.guman.domain.chat.ChatStreamEventType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

/** 验证 AgentScope 流式事件会被准确转换为领域事件。 */
class AgentScopeChatAdapterTest {

    /** 验证思考增量会保留内容并转换为独立领域事件，无入参且无返回值。 */
    @Test
    void mapsThinkingDeltaEvent() {
        AgentScopeChatAdapter adapter = new AgentScopeChatAdapter(null);
        ThinkingBlockDeltaEvent sourceEvent =
                new ThinkingBlockDeltaEvent("reply-id", "block-id", "正在分析问题");

        ChatStreamEvent domainEvent = adapter.toDomainEvent(sourceEvent);

        assertThat(domainEvent).isNotNull();
        assertThat(domainEvent.getType()).isEqualTo(ChatStreamEventType.THINKING_DELTA);
        assertThat(domainEvent.getContent()).isEqualTo("正在分析问题");
    }

    /** 验证图片和文本附件会转换为对应 AgentScope 内容块，无入参且无返回值。 */
    @Test
    void createsMultimodalUserMessage() {
        AgentScopeChatAdapter adapter = new AgentScopeChatAdapter(null);
        ChatAttachment image =
                new ChatAttachment("diagram.png", "image/png", new byte[] {1, 2, 3});
        ChatAttachment text =
                new ChatAttachment(
                        "notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        UserMessage userMessage =
                adapter.toUserMessage(new ChatMessage("请分析", List.of(image, text)));

        assertThat(userMessage.getContentBlocks(TextBlock.class)).hasSize(2);
        assertThat(userMessage.getContentBlocks(TextBlock.class).get(1).getText())
                .contains("notes.txt", "hello");
        ImageBlock imageBlock = userMessage.getFirstContentBlock(ImageBlock.class);
        assertThat(imageBlock.getSource()).isInstanceOf(Base64Source.class);
        assertThat(((Base64Source) imageBlock.getSource()).getData()).isEqualTo("AQID");
    }

    /** 验证文本型 PDF 会提取为带文件名边界的模型文本，无入参且无返回值。 */
    @Test
    void extractsTextFromPdfAttachments() throws IOException {
        AgentScopeChatAdapter adapter = new AgentScopeChatAdapter(null);
        ChatAttachment pdf =
                new ChatAttachment("guide.pdf", "application/pdf", createTextPdf("PDF content"));

        UserMessage userMessage = adapter.toUserMessage(new ChatMessage("总结", List.of(pdf)));

        assertThat(userMessage.getContentBlocks(TextBlock.class).get(1).getText())
                .contains("guide.pdf", "PDF content");
    }

    /**
     * 创建仅包含一行可提取文本的 PDF 测试数据。
     *
     * @param text 写入 PDF 的文本
     * @return PDF 原始字节
     * @throws IOException PDF 生成失败时抛出
     */
    private byte[] createTextPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream =
                    new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
