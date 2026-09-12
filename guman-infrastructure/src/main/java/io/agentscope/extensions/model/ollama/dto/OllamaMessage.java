package io.agentscope.extensions.model.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 兼容 AgentScope 2.0.3 的 Ollama 消息 DTO，并补充该版本遗漏的 thinking 字段。
 *
 * <p>该类保持上游二进制接口不变，待 AgentScope 原生支持 Ollama thinking 后删除。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaMessage {

    private String role;

    private String content;

    private String thinking;

    private List<String> images;

    @JsonProperty("tool_calls")
    private List<OllamaToolCall> toolCalls;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    private String name;

    /** 创建供 JSON 反序列化使用的空消息，无入参。 */
    public OllamaMessage() {}

    /**
     * 创建包含角色和文本内容的 Ollama 消息。
     *
     * @param role 消息角色
     * @param content 消息文本
     */
    public OllamaMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * 获取消息角色。
     *
     * @return 消息角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置消息角色，无返回值。
     *
     * @param role 消息角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取最终回答文本。
     *
     * @return 最终回答文本
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置最终回答文本，无返回值。
     *
     * @param content 最终回答文本
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取模型思考过程。
     *
     * @return 模型思考过程
     */
    public String getThinking() {
        return thinking;
    }

    /**
     * 设置模型思考过程，无返回值。
     *
     * @param thinking 模型思考过程
     */
    public void setThinking(String thinking) {
        this.thinking = thinking;
    }

    /**
     * 获取消息图片列表。
     *
     * @return Base64 编码或 URL 形式的图片列表
     */
    public List<String> getImages() {
        return images;
    }

    /**
     * 设置消息图片列表，无返回值。
     *
     * @param images Base64 编码或 URL 形式的图片列表
     */
    public void setImages(List<String> images) {
        this.images = images;
    }

    /**
     * 获取模型发起的工具调用。
     *
     * @return 工具调用列表
     */
    public List<OllamaToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * 设置模型发起的工具调用，无返回值。
     *
     * @param toolCalls 工具调用列表
     */
    public void setToolCalls(List<OllamaToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    /**
     * 获取当前工具结果对应的调用 ID。
     *
     * @return 工具调用 ID
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * 设置当前工具结果对应的调用 ID，无返回值。
     *
     * @param toolCallId 工具调用 ID
     */
    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    /**
     * 获取工具名称。
     *
     * @return 工具名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置工具名称，无返回值。
     *
     * @param name 工具名称
     */
    public void setName(String name) {
        this.name = name;
    }
}
