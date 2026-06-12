package com.campus.ai.client;

import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;

import java.util.List;

public interface AiProviderClient {
    List<Double> createEmbedding(String input);

    String createChatCompletion(List<AiChatMessage> messages);

    AiModerationAdvice moderate(String targetType, String title, String content);
}
