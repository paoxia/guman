package io.github.paoxia.guman.infrastructure.agentscope;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 保存可用模型配置以及当前生效的配置名称。 */
@ConfigurationProperties(prefix = "guman.model")
public class AgentModelProperties {

    /*
     * 当前生效的模型配置名称，必须对应 configurations 中的一个键。
     */
    private String active;

    /*
     * 按名称保存的模型配置，允许同时声明多个 Ollama 或 OpenAI-compatible 模型。
     */
    private Map<String, ModelSettings> configurations = new LinkedHashMap<>();

    /** 创建供 Spring Boot 配置绑定使用的模型配置集合，无入参。 */
    public AgentModelProperties() {}

    /**
     * 获取当前生效的模型配置名称。
     *
     * @return 当前生效的模型配置名称
     */
    public String getActive() {
        return active;
    }

    /**
     * 设置当前生效的模型配置名称，无返回值。
     *
     * @param active 当前生效的模型配置名称
     */
    public void setActive(String active) {
        this.active = active;
    }

    /**
     * 获取全部命名模型配置。
     *
     * @return 以配置名称为键的模型配置
     */
    public Map<String, ModelSettings> getConfigurations() {
        return configurations;
    }

    /**
     * 设置全部命名模型配置，无返回值。
     *
     * @param configurations 以配置名称为键的模型配置
     */
    public void setConfigurations(Map<String, ModelSettings> configurations) {
        this.configurations = configurations;
    }

    /** 描述一套可被选中的模型连接与能力配置。 */
    public static class ModelSettings {

        /*
         * 模型提供方类型，目前支持 ollama 和 openai。
         */
        private String provider;

        /*
         * 提供方识别的模型名称。
         */
        private String modelName;

        /*
         * 模型服务的基础地址。
         */
        private String baseUrl;

        /*
         * OpenAI-compatible 服务使用的 API Key，允许为空以支持本地服务。
         */
        private String apiKey;

        /*
         * OpenAI-compatible 服务的 Chat Completions 请求路径。
         */
        private String endpointPath;

        /*
         * 是否启用模型流式响应。
         */
        private boolean stream = true;

        /*
         * 是否启用 OpenAI 原生结构化输出，未配置时采用 AgentScope 默认值。
         */
        private Boolean nativeStructuredOutput;

        /*
         * 工具调用期间是否启用 OpenAI 原生结构化输出，未配置时采用 AgentScope 默认值。
         */
        private Boolean nativeStructuredOutputWithTools;

        /** 创建供 Spring Boot 配置绑定使用的单套模型配置，无入参。 */
        public ModelSettings() {}

        /**
         * 获取模型提供方类型。
         *
         * @return 模型提供方类型
         */
        public String getProvider() {
            return provider;
        }

        /**
         * 设置模型提供方类型，无返回值。
         *
         * @param provider 模型提供方类型
         */
        public void setProvider(String provider) {
            this.provider = provider;
        }

        /**
         * 获取模型名称。
         *
         * @return 模型名称
         */
        public String getModelName() {
            return modelName;
        }

        /**
         * 设置模型名称，无返回值。
         *
         * @param modelName 模型名称
         */
        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        /**
         * 获取模型服务基础地址。
         *
         * @return 模型服务基础地址
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置模型服务基础地址，无返回值。
         *
         * @param baseUrl 模型服务基础地址
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * 获取 OpenAI-compatible API Key。
         *
         * @return API Key，未配置时为空
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * 设置 OpenAI-compatible API Key，无返回值。
         *
         * @param apiKey API Key
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * 获取 OpenAI-compatible 请求路径。
         *
         * @return Chat Completions 请求路径
         */
        public String getEndpointPath() {
            return endpointPath;
        }

        /**
         * 设置 OpenAI-compatible 请求路径，无返回值。
         *
         * @param endpointPath Chat Completions 请求路径
         */
        public void setEndpointPath(String endpointPath) {
            this.endpointPath = endpointPath;
        }

        /**
         * 判断是否启用模型流式响应。
         *
         * @return 启用时返回 true
         */
        public boolean isStream() {
            return stream;
        }

        /**
         * 设置是否启用模型流式响应，无返回值。
         *
         * @param stream 是否启用模型流式响应
         */
        public void setStream(boolean stream) {
            this.stream = stream;
        }

        /**
         * 获取 OpenAI 原生结构化输出开关。
         *
         * @return 开关值，未配置时为空
         */
        public Boolean getNativeStructuredOutput() {
            return nativeStructuredOutput;
        }

        /**
         * 设置 OpenAI 原生结构化输出开关，无返回值。
         *
         * @param nativeStructuredOutput 是否启用原生结构化输出
         */
        public void setNativeStructuredOutput(Boolean nativeStructuredOutput) {
            this.nativeStructuredOutput = nativeStructuredOutput;
        }

        /**
         * 获取工具调用期间的 OpenAI 原生结构化输出开关。
         *
         * @return 开关值，未配置时为空
         */
        public Boolean getNativeStructuredOutputWithTools() {
            return nativeStructuredOutputWithTools;
        }

        /**
         * 设置工具调用期间的 OpenAI 原生结构化输出开关，无返回值。
         *
         * @param nativeStructuredOutputWithTools 工具调用期间是否启用原生结构化输出
         */
        public void setNativeStructuredOutputWithTools(
                Boolean nativeStructuredOutputWithTools) {
            this.nativeStructuredOutputWithTools = nativeStructuredOutputWithTools;
        }
    }
}
