package com.campus.ai.client;

import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;

import java.util.List;
import java.util.Map;

public interface AiProviderClient {
    List<Double> createEmbedding(String input);

    String createChatCompletion(List<AiChatMessage> messages);

    String createChatCompletion(List<AiChatMessage> messages, Map<String, Object> responseFormat);

    AiModerationAdvice moderate(String targetType, String title, String content);
}
