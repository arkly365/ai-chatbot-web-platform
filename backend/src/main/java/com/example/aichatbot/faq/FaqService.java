package com.example.aichatbot.faq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class FaqService {

    private static final double THRESHOLD = 0.60;

    private final EmbeddingClient embeddingClient;
    private List<FaqItem> faqList;

    public FaqService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("faq/faq-data.json");

            if (is == null) {
                throw new RuntimeException("faq/faq-data.json not found");
            }

            faqList = mapper.readValue(is, new TypeReference<List<FaqItem>>() {});

            for (FaqItem item : faqList) {
                item.setEmbedding(embeddingClient.getEmbedding(item.getQuestion()));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load FAQ data", e);
        }
    }

    public FaqItem findRelevantFaq(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }

        List<Double> userEmbedding = embeddingClient.getEmbedding(userMessage);

        double bestScore = 0.0;
        FaqItem bestItem = null;

        for (FaqItem item : faqList) {
            double score = cosineSimilarity(userEmbedding, item.getEmbedding());

            System.out.println("Compare with: " + item.getQuestion() + " => score=" + score);

            if (score > bestScore) {
                bestScore = score;
                bestItem = item;
            }
        }

        System.out.println("Best score = " + bestScore);

        if (bestScore >= THRESHOLD) {
            return bestItem;
        }

        return null;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}