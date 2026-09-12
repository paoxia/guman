package io.github.paoxia.guman.interfaces.chat;

import io.github.paoxia.guman.application.chat.StreamChatUseCase;
import io.github.paoxia.guman.domain.chat.ChatAttachment;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 将应用层聊天事件流转换为前端可直接消费的 SSE 事件。 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final int MAX_ATTACHMENT_COUNT = 4;

    private static final Map<String, String> MEDIA_TYPES_BY_EXTENSION =
            Map.ofEntries(
                    Map.entry("png", "image/png"),
                    Map.entry("jpg", "image/jpeg"),
                    Map.entry("jpeg", "image/jpeg"),
                    Map.entry("webp", "image/webp"),
                    Map.entry("pdf", "application/pdf"),
                    Map.entry("json", "application/json"),
                    Map.entry("xml", "application/xml"),
                    Map.entry("yaml", "application/yaml"),
                    Map.entry("yml", "application/yaml"),
                    Map.entry("md", "text/markdown"),
                    Map.entry("markdown", "text/markdown"),
                    Map.entry("csv", "text/csv"),
                    Map.entry("txt", "text/plain"),
                    Map.entry("java", "text/plain"),
                    Map.entry("kt", "text/plain"),
                    Map.entry("js", "text/plain"),
                    Map.entry("ts", "text/plain"),
                    Map.entry("jsx", "text/plain"),
                    Map.entry("tsx", "text/plain"),
                    Map.entry("py", "text/plain"),
                    Map.entry("go", "text/plain"),
                    Map.entry("rs", "text/plain"),
                    Map.entry("c", "text/plain"),
                    Map.entry("h", "text/plain"),
                    Map.entry("cpp", "text/plain"),
                    Map.entry("hpp", "text/plain"),
                    Map.entry("html", "text/plain"),
                    Map.entry("css", "text/plain"),
                    Map.entry("scss", "text/plain"),
                    Map.entry("sql", "text/plain"),
                    Map.entry("sh", "text/plain"),
                    Map.entry("bash", "text/plain"),
                    Map.entry("zsh", "text/plain"),
                    Map.entry("properties", "text/plain"),
                    Map.entry("toml", "text/plain"),
                    Map.entry("ini", "text/plain"),
                    Map.entry("log", "text/plain"));

    /*
     * 记录聊天用例失败的完整服务端诊断信息。
     */
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /*
     * 流式聊天用例由应用模块提供实现。
     */
    private final StreamChatUseCase streamChatUseCase;

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
    public Flux<ServerSentEvent<ChatEventResponse>> streamJson(
            @Valid @RequestBody ChatRequest request) {
        return stream(request, List.of());
    }

    /**
     * 读取 multipart 附件并持续返回模型事件。
     *
     * @param request 用户消息和隔离会话所需的标识
     * @param files 用户选择的图片、文本或 PDF 文件流
     * @return 聊天用例执行过程中产生的 SSE 事件流
     */
    @PostMapping(
            value = "/stream",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEventResponse>> streamMultipart(
            @Valid @RequestPart("request") ChatRequest request,
            @RequestPart(name = "files", required = false) Flux<FilePart> files) {
        return readAttachments(files)
                .flatMapMany(attachments -> stream(request, attachments))
                .onErrorResume(error -> failedStream(request, error));
    }

    /**
     * 调用聊天用例并统一完成事件转换和安全异常处理。
     *
     * @param request 当前聊天请求
     * @param attachments 已读取并完成边界校验的附件
     * @return 可发送给客户端的 SSE 事件流
     */
    private Flux<ServerSentEvent<ChatEventResponse>> stream(
            ChatRequest request, List<ChatAttachment> attachments) {
        return Flux.defer(
                        () ->
                                Flux.from(
                                        streamChatUseCase.stream(
                                                request.toCommand(attachments))))
                .map(ChatEventResponse::from)
                .map(ChatController::serverSentEvent)
                .onErrorResume(error -> failedStream(request, error));
    }

    /**
     * 按顺序读取附件，并限制单次请求的附件数量。
     *
     * @param files WebFlux 提供的文件部件流
     * @return 已完成类型、大小和内容校验的附件集合
     */
    private Mono<List<ChatAttachment>> readAttachments(Flux<FilePart> files) {
        Flux<FilePart> safeFiles = files == null ? Flux.empty() : files;
        return safeFiles
                .index()
                .concatMap(
                        indexedFile -> {
                            if (indexedFile.getT1() >= MAX_ATTACHMENT_COUNT) {
                                return Mono.error(
                                        new IllegalArgumentException(
                                                "a maximum of 4 attachments is allowed"));
                            }
                            return readAttachment(indexedFile.getT2());
                        })
                .collectList();
    }

    /**
     * 在限定内存占用的情况下读取单个文件部件。
     *
     * @param filePart 待读取的 WebFlux 文件部件
     * @return 已通过领域约束校验的附件
     */
    private Mono<ChatAttachment> readAttachment(FilePart filePart) {
        String name = normalizeFilename(filePart.filename());
        String mediaType = resolveMediaType(filePart, name);
        if (!ChatAttachment.isSupportedMediaType(mediaType)) {
            return Mono.error(
                    new IllegalArgumentException("unsupported attachment type: " + name));
        }
        return DataBufferUtils.join(filePart.content(), ChatAttachment.MAX_SIZE_BYTES)
                .map(buffer -> createAttachment(name, mediaType, buffer))
                .onErrorMap(
                        DataBufferLimitException.class,
                        error ->
                                new IllegalArgumentException(
                                        "attachment exceeds the 5 MB size limit: " + name,
                                        error));
    }

    /**
     * 复制并释放 WebFlux 数据缓冲区，然后创建领域附件。
     *
     * @param name 附件显示名称
     * @param mediaType 已解析的 MIME 类型
     * @param buffer 包含附件内容的数据缓冲区
     * @return 领域附件
     */
    private ChatAttachment createAttachment(String name, String mediaType, DataBuffer buffer) {
        byte[] content = new byte[buffer.readableByteCount()];
        try {
            buffer.read(content);
        } finally {
            DataBufferUtils.release(buffer);
        }
        return new ChatAttachment(name, mediaType, content);
    }

    /**
     * 解析浏览器声明或由扩展名推断出的附件 MIME 类型。
     *
     * @param filePart 文件部件
     * @param name 已规范化的文件名
     * @return 规范化 MIME 类型，无法识别时返回空文本
     */
    private String resolveMediaType(FilePart filePart, String name) {
        MediaType contentType = filePart.headers().getContentType();
        if (contentType != null) {
            String declaredType =
                    (contentType.getType() + "/" + contentType.getSubtype())
                            .toLowerCase(Locale.ROOT);
            if (ChatAttachment.isSupportedMediaType(declaredType)) {
                return declaredType;
            }
        }
        int extensionStart = name.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == name.length() - 1) {
            return "";
        }
        String extension = name.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
        return MEDIA_TYPES_BY_EXTENSION.getOrDefault(extension, "");
    }

    /**
     * 移除浏览器可能附带的路径部分，只保留安全显示名称。
     *
     * @param filename 浏览器提交的原始文件名
     * @return 不包含目录的文件名
     */
    private String normalizeFilename(String filename) {
        String normalized = filename == null ? "" : filename.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    /**
     * 记录完整异常并向客户端返回不包含内部细节的失败事件。
     *
     * @param request 当前聊天请求
     * @param error 调用或附件处理异常
     * @return 仅包含一个安全错误事件的流
     */
    private Flux<ServerSentEvent<ChatEventResponse>> failedStream(
            ChatRequest request, Throwable error) {
        log.error(
                "聊天流式调用失败，userId={}, sessionId={}",
                request.getUserId(),
                request.getSessionId(),
                error);
        String message = clientErrorMessage(error);
        return Flux.just(serverSentEvent(new ChatEventResponse("error", message)));
    }

    /**
     * 将服务端异常转换为不泄露内部细节的客户端错误提示。
     *
     * @param error 调用或附件处理异常
     * @return 可直接展示给用户的安全错误信息
     */
    static String clientErrorMessage(Throwable error) {
        if (containsContextLimitError(error)) {
            return "图片过大或模型上下文不足，请压缩图片后重试。";
        }
        if (error instanceof IllegalArgumentException) {
            return "附件处理失败，请检查文件类型、大小和内容。";
        }
        return "生成失败，请稍后重试。";
    }

    /**
     * 判断异常链是否包含模型上下文窗口超限标识。
     *
     * @param error 待检查的异常
     * @return 上下文窗口超限时返回 true
     */
    private static boolean containsContextLimitError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && (message.contains("exceed_context_size_error")
                            || message.contains("exceeds the available context size"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
