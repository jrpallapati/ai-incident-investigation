package org.pallapati.aiincidentinvestigation;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@SpringBootTest(properties = {
        "app.ingestion.enabled=false",
        "spring.ai.openai.api-key=TEST_KEY",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
class AiIncidentInvestigationApplicationTests {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        VectorStore vectorStore() {
            return new VectorStore() {
                @Override public void add(List<Document> documents) {}
                @Override public List<Document> similaritySearch(SearchRequest request) { return List.of(); }
                @Override public void delete(Filter.Expression filterExpression) {}
                @Override public void delete(List<String> idList) {}
            };
        }
    }

    @Test
    void contextLoads() {
    }

}
