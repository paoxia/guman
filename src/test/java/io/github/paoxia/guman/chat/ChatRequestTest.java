package io.github.paoxia.guman.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ChatRequestTest {

    /**
     * 验证缺少用户或会话标识时会应用默认值，无入参且无返回值。
     */
    @Test
    void suppliesDefaultsForMissingConversationIdentifiers() {
        ChatRequest request = new ChatRequest("hello", null, " ");

        assertThat(request.getUserId()).isEqualTo("anonymous");
        assertThat(request.getSessionId()).isEqualTo("default");
    }

    /**
     * 验证空白消息会被请求模型拒绝，无入参且无返回值。
     */
    @Test
    void rejectsBlankMessages() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChatRequest(" ", "user", "session"));
    }
}
