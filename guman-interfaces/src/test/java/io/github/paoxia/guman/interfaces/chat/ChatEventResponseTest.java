package io.github.paoxia.guman.interfaces.chat;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import io.github.paoxia.guman.domain.chat.ChatStreamEventType;
import org.junit.jupiter.api.Test;

class ChatEventResponseTest {

    /**
     * 验证领域事件类型会映射为原有的稳定 SSE 事件名，无入参且无返回值。
     */
    @Test
    void mapsDomainEventTypesToStableSseNames() {
        assertThat(responseTypeOf(ChatStreamEventType.TEXT_DELTA)).isEqualTo("text-delta");
        assertThat(responseTypeOf(ChatStreamEventType.TOOL_STARTED)).isEqualTo("tool-start");
        assertThat(responseTypeOf(ChatStreamEventType.TOOL_COMPLETED)).isEqualTo("tool-end");
        assertThat(responseTypeOf(ChatStreamEventType.COMPLETED)).isEqualTo("done");
    }

    /**
     * 创建指定类型的领域事件并读取转换后的接口事件名。
     *
     * @param eventType 待转换的领域事件类型
     * @return 对应的 SSE 事件名
     */
    private String responseTypeOf(ChatStreamEventType eventType) {
        ChatStreamEvent event = new ChatStreamEvent(eventType, "content");
        return ChatEventResponse.from(event).getType();
    }
}
