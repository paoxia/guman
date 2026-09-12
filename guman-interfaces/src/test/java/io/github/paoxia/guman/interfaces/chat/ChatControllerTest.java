package io.github.paoxia.guman.interfaces.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 验证聊天接口对服务端异常的安全错误映射。 */
class ChatControllerTest {

    /** 验证 Ollama 上下文窗口超限时返回可操作的图片提示，无入参且无返回值。 */
    @Test
    void mapsContextLimitErrorsToActionableMessage() {
        RuntimeException error =
                new RuntimeException(
                        "HTTP 400",
                        new IllegalStateException(
                                "request exceeds the available context size "
                                        + "(exceed_context_size_error)"));

        assertThat(ChatController.clientErrorMessage(error))
                .isEqualTo("图片过大或模型上下文不足，请压缩图片后重试。");
    }

    /** 验证普通模型异常仍返回通用安全提示，无入参且无返回值。 */
    @Test
    void mapsUnknownErrorsToGenericMessage() {
        assertThat(ChatController.clientErrorMessage(new RuntimeException("upstream failed")))
                .isEqualTo("生成失败，请稍后重试。");
    }
}
