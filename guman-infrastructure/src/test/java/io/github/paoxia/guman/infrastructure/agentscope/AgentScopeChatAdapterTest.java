package io.github.paoxia.guman.infrastructure.agentscope;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import io.github.paoxia.guman.domain.chat.ChatStreamEventType;
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
}
