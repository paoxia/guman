package io.github.paoxia.guman.config;

import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AgentConfiguration {

    @Bean
    HarnessAgent gumanAgent(
            Model model, @Value("${guman.agent.workspace}") Path workspace) {
        return HarnessAgent.builder()
                .name("guman")
                .sysPrompt("You are Guman, a helpful AI assistant.")
                .model(model)
                .workspace(workspace)
                .build();
    }
}
