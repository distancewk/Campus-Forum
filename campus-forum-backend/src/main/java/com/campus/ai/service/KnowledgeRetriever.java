package com.campus.ai.service;

import com.campus.ai.dto.RetrievedChunk;

import java.util.List;

@FunctionalInterface
public interface KnowledgeRetriever {
    List<RetrievedChunk> retrieve(String question);
}
