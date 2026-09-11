package io.github.paoxia.guman.interfaces.chat;

import io.github.paoxia.guman.application.chat.StreamChatUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** 将应用层聊天事件流转换为前端可直接消费的 SSE 事件。 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    /*
     * 记录聊天用例失败的完整服务端诊断信息。
     */
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /*
     * 流式聊天用例由应用模块提供实现。
     */
    @Autowired
    private StreamChatUseCase streamChatUseCase;

    /**
     * 校验 Web 请求并持续返回文本及工具事件。
     *
     * @param request 用户消息和隔离会话所需的标识
     * @return 聊天用例执行过程中产生的 SSE 事件流
     */
    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEventResponse>> stream(
            @Valid @RequestBody ChatRequest request) {
        return Flux.from(streamChatUseCase.stream(request.toCommand()))
                .map(ChatEventResponse::from)
                .map(ChatController::serverSentEvent)
                .onErrorResume(
                        error -> {
                            log.error(
                                    "聊天流式调用失败，userId={}, sessionId={}",
                                    request.getUserId(),
                                    request.getSessionId(),
                                    error);
                            return Flux.just(
                                    serverSentEvent(
                                            new ChatEventResponse(
                                                    "error", "生成失败，请稍后重试。")));
                        });
    }

    /**
     * 将聊天响应包装成带事件名的 Spring SSE 对象。
     *
     * @param event 待发送的聊天响应事件
     * @return 可由 WebFlux 序列化的 SSE 对象
     */
    private static ServerSentEvent<ChatEventResponse> serverSentEvent(
            ChatEventResponse event) {
        return ServerSentEvent.<ChatEventResponse>builder()
                .event(event.getType())
                .data(event)
                .build();
    }
}
