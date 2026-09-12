package io.github.paoxia.guman.infrastructure.agentscope;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.extensions.model.ollama.dto.OllamaMessage;
import io.agentscope.extensions.model.ollama.dto.OllamaResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 验证 Ollama thinking 兼容格式化逻辑。 */
class ThinkingOllamaChatFormatterTest {

    /** 验证 Ollama thinking 字段会保留为 AgentScope 思考块，无入参且无返回值。 */
    @Test
    void parsesThinkingAsDedicatedContentBlock() {
        OllamaMessage message = new OllamaMessage("assistant", "最终回答");
        message.setThinking("内部思考");
        OllamaResponse sourceResponse = new OllamaResponse();
        sourceResponse.setMessage(message);

        ChatResponse response =
                new ThinkingOllamaChatFormatter()
                        .parseResponse(sourceResponse, Instant.parse("2026-09-12T00:00:00Z"));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().getFirst()).isInstanceOf(ThinkingBlock.class);
        assertThat(((ThinkingBlock) response.getContent().getFirst()).getThinking())
                .isEqualTo("内部思考");
    }
}
