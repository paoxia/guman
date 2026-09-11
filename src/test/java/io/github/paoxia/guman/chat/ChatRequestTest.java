package io.github.paoxia.guman.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ChatRequestTest {

    @Test
    void suppliesDefaultsForMissingConversationIdentifiers() {
        ChatRequest request = new ChatRequest("hello", null, " ");

        assertThat(request.userId()).isEqualTo("anonymous");
        assertThat(request.sessionId()).isEqualTo("default");
    }

    @Test
    void rejectsBlankMessages() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChatRequest(" ", "user", "session"));
    }
}
