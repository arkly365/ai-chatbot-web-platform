package com.example.aichatbot.service;

import com.example.aichatbot.config.OpenAiProperties;
import com.example.aichatbot.dto.ChatRequest;
import com.example.aichatbot.dto.ChatResponse;
import com.example.aichatbot.dto.OpenAiResponse;
import com.example.aichatbot.entity.ChatLog;
import com.example.aichatbot.repository.ChatLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final RestClient restClient;
    private final OpenAiProperties openAiProperties;
    private final ChatLogRepository chatLogRepository;
    
    
    public ChatService(
            RestClient restClient,
            OpenAiProperties openAiProperties,
            ChatLogRepository chatLogRepository
    ) {
        this.restClient = restClient;
        this.openAiProperties = openAiProperties;
        this.chatLogRepository = chatLogRepository;
    }

    public ChatResponse chat(ChatRequest request) {
    	
    	 String userMessage = request.getMessage();

    	    if (userMessage != null && !userMessage.isBlank()) {
    	        chatLogRepository.save(new ChatLog(
    	                "user",
    	                userMessage,
    	                LocalDateTime.now()
    	        ));
    	    }
    	    
        String aiReply = callOpenAi(request);
        
        chatLogRepository.save(new ChatLog(
                "assistant",
                aiReply,
                LocalDateTime.now()
        ));

        return new ChatResponse(
                aiReply,
                "assistant",
                Arrays.asList("醫院", "圖書館", "加油站", "百貨公司"),
                LocalDateTime.now()
        );
    }

    private String callOpenAi(ChatRequest request) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            return "尚未設定 OPENAI_API_KEY，請先設定環境變數。";
        }

        List<Map<String, String>> input = buildInputMessages(request);

        Map<String, Object> openAiRequest = Map.of(
                "model", openAiProperties.getModel(),
                "input", input
        );

        OpenAiResponse response = restClient.post()
                .uri(openAiProperties.getBaseUrl())
                .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                .header("Content-Type", "application/json")
                .body(openAiRequest)
                .retrieve()
                .body(OpenAiResponse.class);

        return extractText(response);
    }

    private List<Map<String, String>> buildInputMessages(ChatRequest request) {
        List<Map<String, String>> input = new ArrayList<>();

        input.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt(request.getAiRole())
        ));

        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            for (ChatRequest.ChatMessage msg : request.getMessages()) {
                if (msg.getRole() == null || msg.getContent() == null || msg.getContent().isBlank()) {
                    continue;
                }

                input.add(Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent()
                ));
            }

            return input;
        }

        input.add(Map.of(
                "role", "user",
                "content", request.getMessage()
        ));

        return input;
    }
    
    private String buildSystemPrompt(String role) {

        if (role == null || role.isBlank()) {
            return "你是一位友善的繁體中文 AI 助理，請根據前後文回答問題。";
        }

        switch (role) {

            case "travel":
                return "你是一位專業旅遊導覽，請用親切語氣介紹景點、交通與建議行程。";

            case "customer_service":
                return "你是一位客服人員，請用禮貌、清楚的方式回答問題。";

            case "medical":
                return "你是一位醫療助理，請提供一般健康資訊，但避免診斷。";

            default:
                return "你是一位友善的繁體中文 AI 助理，請根據前後文回答問題。";
        }
    }

    private String extractText(OpenAiResponse response) {
        if (response == null || response.getOutput() == null || response.getOutput().isEmpty()) {
            return "AI 沒有回傳內容。";
        }

        for (OpenAiResponse.Output output : response.getOutput()) {
            if (output.getContent() == null) {
                continue;
            }

            for (OpenAiResponse.Content content : output.getContent()) {
                if ("output_text".equals(content.getType())) {
                    return content.getText();
                }
            }
        }

        return "AI 回應格式無法解析。";
    }
}