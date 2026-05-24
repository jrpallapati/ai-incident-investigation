package org.pallapati.aiincidentinvestigation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables Mongo auditing for @CreatedDate and @LastModifiedDate fields.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}

