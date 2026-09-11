package io.github.paoxia.guman.config;

import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 创建并配置应用共享的 AgentScope Agent。 */
@Configuration(proxyBeanMethods = false)
public class AgentConfiguration {

    /*
     * AgentScope 模型由对应的 Spring Boot Starter 创建并注入。
     */
    @Autowired
    private Model model;

    /*
     * Agent 工作区路径来自应用配置，用于保存人格、记忆和会话数据。
     */
    @Value("${guman.agent.workspace}")
    private Path workspace;

    /**
     * 创建应用共享的无状态 HarnessAgent。
     *
     * @return 配置好模型及工作区的 HarnessAgent
     */
    @Bean
    HarnessAgent gumanAgent() {
        return HarnessAgent.builder()
                .name("guman")
                .sysPrompt("You are Guman, a helpful AI assistant.")
                .model(model)
                .workspace(workspace)
                .build();
    }
}
