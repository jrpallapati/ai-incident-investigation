package org.pallapati.aiincidentinvestigation.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a ChatClient built from Spring AI autoconfigured OpenAI model beans.
 * The starter (spring-ai-starter-model-openai) wires the underlying model using properties.
 */
@Configuration
public class OpenAIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // Builder comes from Spring AI auto-configuration and uses properties-defined model (gpt-4o)
        return builder.build();
    }
}
