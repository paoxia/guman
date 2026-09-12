package io.github.paoxia.guman.interfaces.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ChatRequestTest {

    /**
     * 验证接口请求转换后会应用领域会话默认值，无入参且无返回值。
     */
    @Test
    void suppliesDefaultsWhenConvertingRequest() {
        ChatRequest request = new ChatRequest("hello", null, " ");

        assertThat(request.toCommand().getSession().getUserId()).isEqualTo("anonymous");
        assertThat(request.toCommand().getSession().getSessionId()).isEqualTo("default");
    }

    /**
     * 验证接口请求转换时拒绝空白消息，无入参且无返回值。
     */
    @Test
    void rejectsBlankMessagesWhenConvertingRequest() {
        ChatRequest request = new ChatRequest(" ", "user", "session");

        assertThatIllegalArgumentException().isThrownBy(request::toCommand);
    }
}
