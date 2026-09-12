package io.github.paoxia.guman.infrastructure.agentscope;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.options.OllamaOptions;
import io.agentscope.extensions.model.ollama.options.ThinkOption;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 根据 active 配置创建应用唯一使用的 AgentScope 模型。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentModelProperties.class)
@RequiredArgsConstructor
public class AgentModelConfiguration {

    private static final String OLLAMA_PROVIDER = "ollama";
    private static final String OPENAI_PROVIDER = "openai";

    /*
     * 模型配置由 Spring Boot 从 guman.model 命名空间绑定。
     */
    private final AgentModelProperties modelProperties;

    /**
     * 根据 active 名称解析并创建当前生效的模型，无入参。
     *
     * @return 当前应用唯一使用的 AgentScope 模型
     */
    @Bean
    Model activeModel() {
        String activeName = requireText(modelProperties.getActive(), "guman.model.active");
        Map<String, AgentModelProperties.ModelSettings> configurations =
                modelProperties.getConfigurations();
        AgentModelProperties.ModelSettings settings = configurations.get(activeName);
        if (settings == null) {
            throw new IllegalStateException(
                    "guman.model.active must reference an entry in guman.model.configurations: "
                            + activeName);
        }

        String provider =
                requireText(
                                settings.getProvider(),
                                "guman.model.configurations." + activeName + ".provider")
                        .toLowerCase(Locale.ROOT);
        return switch (provider) {
            case OLLAMA_PROVIDER -> buildOllamaModel(activeName, settings);
            case OPENAI_PROVIDER -> buildOpenAIModel(activeName, settings);
            default ->
                    throw new IllegalStateException(
                            "Unsupported model provider for guman.model.configurations."
                                    + activeName
                                    + ".provider: "
                                    + provider);
        };
    }

    /**
     * 创建使用 Ollama 原生接口的模型。
     *
     * @param activeName 当前生效的配置名称
     * @param settings 当前生效的模型配置
     * @return Ollama 模型
     */
    private Model buildOllamaModel(
            String activeName, AgentModelProperties.ModelSettings settings) {
        String propertyPrefix = "guman.model.configurations." + activeName;
        OllamaChatModel.Builder builder =
                OllamaChatModel.builder()
                        .modelName(
                                requireText(
                                        settings.getModelName(), propertyPrefix + ".model-name"))
                        .baseUrl(requireText(settings.getBaseUrl(), propertyPrefix + ".base-url"))
                        .stream(settings.isStream());
        if (settings.getThinkingEnabled() != null) {
            ThinkOption thinkOption =
                    settings.getThinkingEnabled()
                            ? ThinkOption.ThinkBoolean.ENABLED
                            : ThinkOption.ThinkBoolean.DISABLED;
            builder.defaultOptions(OllamaOptions.builder().thinkOption(thinkOption).build());
            if (settings.getThinkingEnabled()) {
                builder.formatter(new ThinkingOllamaChatFormatter());
            }
        }
        return builder.build();
    }

    /**
     * 创建使用 OpenAI Chat Completions 协议的兼容模型。
     *
     * @param activeName 当前生效的配置名称
     * @param settings 当前生效的模型配置
     * @return OpenAI-compatible 模型
     */
    private Model buildOpenAIModel(
            String activeName, AgentModelProperties.ModelSettings settings) {
        String propertyPrefix = "guman.model.configurations." + activeName;
        OpenAIChatModel.Builder builder =
                OpenAIChatModel.builder()
                        .apiKey(trimToNull(settings.getApiKey()))
                        .modelName(
                                requireText(
                                        settings.getModelName(), propertyPrefix + ".model-name"))
                        .baseUrl(
                                requireText(settings.getBaseUrl(), propertyPrefix + ".base-url"))
                        .stream(settings.isStream());

        String endpointPath = trimToNull(settings.getEndpointPath());
        if (endpointPath != null) {
            builder.endpointPath(endpointPath);
        }
        if (settings.getNativeStructuredOutput() != null) {
            builder.nativeStructuredOutput(settings.getNativeStructuredOutput());
        }
        if (settings.getNativeStructuredOutputWithTools() != null) {
            builder.nativeStructuredOutputWithTools(
                    settings.getNativeStructuredOutputWithTools());
        }
        return builder.build();
    }

    /**
     * 校验必填文本配置并移除首尾空白。
     *
     * @param value 待校验的配置值
     * @param propertyName 用于错误提示的配置项名称
     * @return 清理后的非空配置值
     */
    private String requireText(String value, String propertyName) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        return normalizedValue;
    }

    /**
     * 将空白文本规范化为空值。
     *
     * @param value 待规范化的文本
     * @return 清理后的文本，空白输入返回 null
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.strip();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
