package io.github.paoxia.guman.infrastructure.agentscope;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.extensions.model.ollama.dto.OllamaMessage;
import io.agentscope.extensions.model.ollama.dto.OllamaResponse;
import io.agentscope.extensions.model.ollama.formatter.OllamaChatFormatter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 在 AgentScope 原有 Ollama 格式化能力上补充 thinking 响应解析。 */
final class ThinkingOllamaChatFormatter extends OllamaChatFormatter {

    /** 创建支持 Ollama thinking 字段的格式化器，无入参。 */
    ThinkingOllamaChatFormatter() {
        super();
    }

    /**
     * 将 Ollama 独立返回的 thinking 内容转换为 AgentScope 原生思考块。
     *
     * @param sourceResponse Ollama 原始响应
     * @param timestamp 接收到本次响应的时间
     * @return 同时包含思考块、文本块和工具调用块的模型响应
     */
    @Override
    public ChatResponse parseResponse(OllamaResponse sourceResponse, Instant timestamp) {
        ChatResponse response = super.parseResponse(sourceResponse, timestamp);
        OllamaMessage message = sourceResponse.getMessage();
        if (message == null || message.getThinking() == null || message.getThinking().isEmpty()) {
            return response;
        }

        List<ContentBlock> content = new ArrayList<>();
        content.add(ThinkingBlock.builder().thinking(message.getThinking()).build());
        content.addAll(response.getContent());
        return new ChatResponse(
                response.getId(),
                content,
                response.getUsage(),
                response.getMetadata(),
                response.getFinishReason());
    }
}
