package com.aml.investigation.orchestrator;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrchestratorAgentConfig {

    @Bean
    public ChatLanguageModel orchestratorChatModel(
            @Value("${aml.anthropic.api-key}") String apiKey,
            @Value("${aml.anthropic.model-name:claude-sonnet-5}") String modelName) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    @Bean
    public OrchestratorAgent orchestratorAgent(ChatLanguageModel orchestratorChatModel) {
        // No .tools(...) call here, unlike the three sub-agent configs — this AiService is a
        // single reasoning pass over evidence already gathered, never a ReAct loop of its own.
        return AiServices.builder(OrchestratorAgent.class)
                .chatLanguageModel(orchestratorChatModel)
                .build();
    }
}
