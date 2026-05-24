package org.pallapati.aiincidentinvestigation.vector;

import org.springframework.context.annotation.Configuration;

/**
 * VectorStore configuration.
 * We rely on Spring AI auto-configuration (spring-ai-starter-vector-store-mongodb-atlas)
 * driven by application properties to instantiate the VectorStore and EmbeddingModel beans.
 * No explicit beans are declared here to avoid duplicate definitions.
 */
@Configuration
public class VectorStoreConfig {
}
