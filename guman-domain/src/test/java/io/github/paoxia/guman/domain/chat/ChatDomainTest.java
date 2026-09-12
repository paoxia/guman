package io.github.paoxia.guman.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatDomainTest {

    /**
     * 验证会话标识缺失时会应用领域默认值，无入参且无返回值。
     */
    @Test
    void suppliesDefaultsForMissingConversationIdentifiers() {
        ChatSession session = new ChatSession(null, " ");

        assertThat(session.getUserId()).isEqualTo("anonymous");
        assertThat(session.getSessionId()).isEqualTo("default");
    }

    /**
     * 验证领域消息不接受空白内容，无入参且无返回值。
     */
    @Test
    void rejectsBlankMessages() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ChatMessage(" "));
    }

    /** 验证仅包含附件的消息仍是有效领域消息，无入参且无返回值。 */
    @Test
    void acceptsAttachmentOnlyMessages() {
        ChatAttachment attachment =
                new ChatAttachment(
                        "notes.txt",
                        "text/plain",
                        "attachment content".getBytes(StandardCharsets.UTF_8));

        ChatMessage message = new ChatMessage(" ", List.of(attachment));

        assertThat(message.getAttachments()).containsExactly(attachment);
    }

    /** 验证领域附件会拒绝不受支持的二进制类型，无入参且无返回值。 */
    @Test
    void rejectsUnsupportedAttachmentTypes() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                new ChatAttachment(
                                        "archive.zip",
                                        "application/zip",
                                        new byte[] {1, 2, 3}));
    }
}
