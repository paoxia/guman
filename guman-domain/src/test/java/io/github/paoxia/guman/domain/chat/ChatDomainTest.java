package io.github.paoxia.guman.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
}
