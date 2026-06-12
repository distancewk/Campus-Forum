package com.campus.ai.service;

import com.campus.ai.client.AiProviderClient;
import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.RetrievedChunk;
import com.campus.ai.mapper.AiKnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService implements KnowledgeRetriever {
    private final AiProviderClient aiProviderClient;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiProperties properties;

    @Override
    public List<RetrievedChunk> retrieve(String question) {
        List<Double> embedding = aiProviderClient.createEmbedding(question);
        String vector = embedding.stream()
                .map(value -> String.format(Locale.US, "%.8f", value))
                .collect(Collectors.joining(",", "[", "]"));
        int limit = Math.max(properties.getQa().getMaxSources() * 4, 12);
        return chunkMapper.hybridSearch(question, vector, limit);
    }
}
