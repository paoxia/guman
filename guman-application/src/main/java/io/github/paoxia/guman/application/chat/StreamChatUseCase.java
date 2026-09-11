package io.github.paoxia.guman.application.chat;

import io.github.paoxia.guman.domain.chat.ChatStreamEvent;
import org.reactivestreams.Publisher;

/** 定义供外部接口调用的流式聊天用例。 */
public interface StreamChatUseCase {

    /**
     * 在指定会话中执行一次流式聊天。
     *
     * @param command 包含消息与会话信息的聊天命令
     * @return 按生成顺序发布的聊天领域事件
     */
    Publisher<ChatStreamEvent> stream(ChatCommand command);
}
