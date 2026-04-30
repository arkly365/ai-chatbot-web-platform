package com.example.aichatbot.faq;


import com.example.aichatbot.config.OpenAiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class EmbeddingClient {

    private final RestClient restClient;
    private final OpenAiProperties openAiProperties;

    public EmbeddingClient(RestClient restClient, OpenAiProperties openAiProperties) {
        this.restClient = restClient;
        this.openAiProperties = openAiProperties;
    }

    public List<Double> getEmbedding(String text) {

        Map<String, Object> request = Map.of(
                "model", "text-embedding-3-small",
                "input", text
        );

        Map response = restClient.post()
                .uri("https://api.openai.com/v1/embeddings")
                .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(Map.class);

        List data = (List) response.get("data");
        Map first = (Map) data.get(0);

        return (List<Double>) first.get("embedding");
    }
}