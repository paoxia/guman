package io.github.paoxia.guman.chat;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final HarnessAgent agent;

    public ChatController(HarnessAgent agent) {
        this.agent = agent;
    }

    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEvent>> stream(@RequestBody ChatRequest request) {
        RuntimeContext context =
                RuntimeContext.builder()
                        .userId(request.userId())
                        .sessionId(request.sessionId())
                        .build();

        return agent.streamEvents(new UserMessage(request.message()), context)
                .mapNotNull(this::toServerSentEvent)
                .onErrorResume(
                        error ->
                                Flux.just(
                                        serverSentEvent(
                                                new ChatEvent("error", safeMessage(error)))));
    }

    private ServerSentEvent<ChatEvent> toServerSentEvent(AgentEvent event) {
        ChatEvent chatEvent =
                switch (event.getType()) {
                    case TEXT_BLOCK_DELTA ->
                            new ChatEvent(
                                    "text-delta", ((TextBlockDeltaEvent) event).getDelta());
                    case TOOL_CALL_START ->
                            new ChatEvent(
                                    "tool-start",
                                    ((ToolCallStartEvent) event).getToolCallName());
                    case TOOL_RESULT_END -> {
                        ToolResultEndEvent result = (ToolResultEndEvent) event;
                        yield new ChatEvent(
                                "tool-end",
                                result.getToolCallName() + ":" + result.getState());
                    }
                    case AGENT_END -> new ChatEvent("done", "");
                    default -> null;
                };
        return chatEvent == null ? null : serverSentEvent(chatEvent);
    }

    private static ServerSentEvent<ChatEvent> serverSentEvent(ChatEvent event) {
        return ServerSentEvent.<ChatEvent>builder()
                .event(event.type())
                .data(event)
                .build();
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
