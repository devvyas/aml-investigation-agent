package com.aml.investigation.network;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NetworkAgentConfig {

    @Bean
    public ChatLanguageModel networkChatModel(
            @Value("${aml.anthropic.api-key}") String apiKey,
            @Value("${aml.anthropic.model-name:claude-sonnet-5}") String modelName) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    @Bean
    public NetworkAgent networkAgent(ChatLanguageModel networkChatModel, NetworkTools networkTools) {
        return AiServices.builder(NetworkAgent.class)
                .chatLanguageModel(networkChatModel)
                .tools(networkTools)
                .build();
    }
}
