package io.github.paoxia.guman.application.chat;

import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import java.util.Objects;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 编排聊天领域对象与外部 Agent 端口的应用服务。 */
@Service
public class ChatApplicationService implements StreamChatUseCase {

    /*
     * 外部 Agent 能力由基础设施模块提供实现。
     */
    @Autowired
    private AgentChatPort agentChatPort;

    /**
     * 校验聊天命令并委托外部 Agent 生成流式回复。
     *
     * @param command 包含领域消息和会话信息的聊天命令
     * @return 外部 Agent 生成的聊天领域事件流
     */
    @Override
    public Publisher<ChatStreamEvent> stream(ChatCommand command) {
        ChatCommand requiredCommand = Objects.requireNonNull(command, "command must not be null");
        return agentChatPort.stream(requiredCommand.getMessage(), requiredCommand.getSession());
    }
}
