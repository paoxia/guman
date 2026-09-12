package io.github.paoxia.guman.infrastructure.agentscope;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** 验证命名模型配置的选择、实例化和边界校验。 */
class AgentModelConfigurationTest {

    /*
     * 每个测试使用独立的轻量 Spring 上下文，避免创建完整 HarnessAgent。
     */
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(AgentModelConfiguration.class);

    /** 验证 active 指向 Ollama 配置时只创建对应 Ollama 模型，无入参且无返回值。 */
    @Test
    void selectsNamedOllamaConfiguration() {
        contextRunner
                .withPropertyValues(
                        "guman.model.active=local-qwen",
                        "guman.model.configurations.local-qwen.provider=ollama",
                        "guman.model.configurations.local-qwen.model-name=qwen3.5:9b",
                        "guman.model.configurations.local-qwen.base-url=http://localhost:11434",
                        "guman.model.configurations.remote.provider=openai")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(Model.class);
                            Model model = context.getBean(Model.class);
                            assertThat(model).isInstanceOf(OllamaChatModel.class);
                            assertThat(((OllamaChatModel) model).getModelName())
                                    .isEqualTo("qwen3.5:9b");
                            assertThat(((OllamaChatModel) model).isStreaming()).isTrue();
                        });
    }

    /** 验证 active 可以从多套配置中选择 OpenAI-compatible 模型，无入参且无返回值。 */
    @Test
    void selectsNamedOpenAiCompatibleConfiguration() {
        contextRunner
                .withPropertyValues(
                        "guman.model.active=remote-primary",
                        "guman.model.configurations.local-qwen.provider=ollama",
                        "guman.model.configurations.local-qwen.model-name=qwen3.5:9b",
                        "guman.model.configurations.local-qwen.base-url=http://localhost:11434",
                        "guman.model.configurations.remote-primary.provider=openai",
                        "guman.model.configurations.remote-primary.model-name=compatible-chat",
                        "guman.model.configurations.remote-primary.base-url=https://llm.example.com",
                        "guman.model.configurations.remote-primary.endpoint-path=/v1/chat/completions",
                        "guman.model.configurations.remote-primary.stream=false",
                        "guman.model.configurations.remote-primary.native-structured-output=false",
                        "guman.model.configurations.remote-primary.native-structured-output-with-tools=false")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(Model.class);
                            Model model = context.getBean(Model.class);
                            assertThat(model).isInstanceOf(OpenAIChatModel.class);
                            assertThat(((OpenAIChatModel) model).getModelName())
                                    .isEqualTo("compatible-chat");
                        });
    }

    /** 验证 active 引用不存在的配置时应用上下文启动失败，无入参且无返回值。 */
    @Test
    void rejectsUnknownActiveConfiguration() {
        contextRunner
                .withPropertyValues(
                        "guman.model.active=missing",
                        "guman.model.configurations.local-qwen.provider=ollama")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasRootCauseMessage(
                                            "guman.model.active must reference an entry in "
                                                    + "guman.model.configurations: missing");
                        });
    }

    /** 验证当前配置使用未知 Provider 时应用上下文启动失败，无入参且无返回值。 */
    @Test
    void rejectsUnsupportedProvider() {
        contextRunner
                .withPropertyValues(
                        "guman.model.active=unsupported",
                        "guman.model.configurations.unsupported.provider=unknown")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasRootCauseMessage(
                                            "Unsupported model provider for "
                                                    + "guman.model.configurations.unsupported.provider: "
                                                    + "unknown");
                        });
    }
}
