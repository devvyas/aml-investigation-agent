package com.aml.investigation.sanctions;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SanctionsAgentConfig {

    @Bean
    public ChatLanguageModel sanctionsChatModel(
            @Value("${aml.anthropic.api-key}") String apiKey,
            @Value("${aml.anthropic.model-name:claude-sonnet-5}") String modelName) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    @Bean
    public SanctionsAgent sanctionsAgent(ChatLanguageModel sanctionsChatModel, SanctionsTools sanctionsTools) {
        return AiServices.builder(SanctionsAgent.class)
                .chatLanguageModel(sanctionsChatModel)
                .tools(sanctionsTools)
                .build();
    }
}
