package io.github.paoxia.guman.domain.chat;

/** 描述 Agent 执行过程中可被上层消费的领域事件类型。 */
public enum ChatStreamEventType {
    /**
     * 模型新生成的一段思考过程。
     */
    THINKING_DELTA,

    /**
     * 模型新生成的一段文本。
     */
    TEXT_DELTA,

    /**
     * Agent 开始执行一个工具。
     */
    TOOL_STARTED,

    /**
     * Agent 完成一个工具调用。
     */
    TOOL_COMPLETED,

    /**
     * Agent 完成本轮回复。
     */
    COMPLETED
}
