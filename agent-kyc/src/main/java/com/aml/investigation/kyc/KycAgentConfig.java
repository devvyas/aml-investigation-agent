package com.aml.investigation.kyc;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the private KycAgent proxy. This is the one place in agent-kyc that actually calls
 * AiServices.builder() — everywhere else in the module only ever sees the KycAgent interface
 * or the KycInvestigator port, never the construction mechanics.
 */
@Configuration
public class KycAgentConfig {

    @Bean
    public ChatLanguageModel kycChatModel(
            @Value("${aml.anthropic.api-key}") String apiKey,
            @Value("${aml.anthropic.model-name:claude-sonnet-5}") String modelName) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    @Bean
    public KycAgent kycAgent(ChatLanguageModel kycChatModel, KycTools kycTools) {
        return AiServices.builder(KycAgent.class)
                .chatLanguageModel(kycChatModel)
                .tools(kycTools)
                .build();
    }
}
