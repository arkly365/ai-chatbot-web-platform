package com.example.aichatbot.service;

import com.example.aichatbot.config.OpenAiProperties;
import com.example.aichatbot.dto.ChatRequest;
import com.example.aichatbot.dto.ChatResponse;
import com.example.aichatbot.dto.OpenAiResponse;
import com.example.aichatbot.entity.ChatLog;
import com.example.aichatbot.faq.FaqItem;
import com.example.aichatbot.faq.FaqService;
import com.example.aichatbot.repository.ChatLogRepository;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;
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
    private final FaqService faqService;
    private final MeterRegistry meterRegistry;
    
    
    public ChatService(
            RestClient restClient,
            OpenAiProperties openAiProperties,
            ChatLogRepository chatLogRepository,
            FaqService faqService,
            MeterRegistry meterRegistry
    ) {
        this.restClient = restClient;
        this.openAiProperties = openAiProperties;
        this.chatLogRepository = chatLogRepository;
        this.faqService = faqService;
        this.meterRegistry = meterRegistry;
    }

    public ChatResponse chat(ChatRequest request) {
    	 FaqItem faqItem = faqService.findRelevantFaq(request.getMessage());

    	 if (faqItem != null) {

    		    String userMessage = request.getMessage();

    		    chatLogRepository.save(new ChatLog(
    		            "user",
    		            userMessage,
    		            LocalDateTime.now()
    		    ));

    		    String ragReply = recordAiLatency("rag", () -> callOpenAiWithRagContext(request, faqItem));

    		    chatLogRepository.save(new ChatLog(
    		            "assistant",
    		            ragReply,
    		            LocalDateTime.now()
    		    ));

    		    return new ChatResponse(
    		            ragReply,
    		            "assistant",
    		            Arrays.asList("醫院", "圖書館", "加油站", "百貨公司"),
    		            LocalDateTime.now()
    		    );
    		}
    	
    	 String userMessage = request.getMessage();

    	    if (userMessage != null && !userMessage.isBlank()) {
    	        chatLogRepository.save(new ChatLog(
    	                "user",
    	                userMessage,
    	                LocalDateTime.now()
    	        ));
    	    }
    	    
        String aiReply = recordAiLatency("normal", () -> callOpenAi(request));
        
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
    
    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);

        new Thread(() -> {
            try {
                ChatResponse response = chat(request);
                String reply = response.getReply();

                if (reply == null || reply.isBlank()) {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data("AI 沒有回傳內容。"));
                    emitter.complete();
                    return;
                }

                // 模擬逐字 streaming
                for (int i = 0; i < reply.length(); i++) {
                    String chunk = String.valueOf(reply.charAt(i));

                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(chunk));

                    Thread.sleep(30);
                }

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("[DONE]"));

                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Streaming failed: " + e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    private String callOpenAiWithRagContext(ChatRequest request, FaqItem faqItem) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            return "尚未設定 OPENAI_API_KEY，請先設定環境變數。";
        }

        List<Map<String, String>> input = new ArrayList<>();

        input.add(Map.of(
                "role", "system",
                "content",
                buildSystemPrompt(request.getAiRole())
                        + "\n\n你現在會收到一段 FAQ 知識庫資料。"
                        + "\n請優先根據 FAQ 內容回答使用者問題。"
                        + "\n如果 FAQ 已經足夠回答，請不要自行編造資訊。"
                        + "\n回答請自然、親切、簡潔。"
        ));

        input.add(Map.of(
                "role", "user",
                "content",
                "FAQ 知識庫：\n"
                        + "問題：" + faqItem.getQuestion() + "\n"
                        + "答案：" + faqItem.getAnswer() + "\n\n"
                        + "使用者問題：" + request.getMessage()
        ));

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
    
    private String recordAiLatency(String type, java.util.function.Supplier<String> supplier) {
        long start = System.nanoTime();

        try {
            return supplier.get();
        } finally {
            long duration = System.nanoTime() - start;

            Timer.builder("ai.chat.latency")
                    .description("AI chat response latency")
                    .tag("type", type)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.NANOSECONDS);
        }
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