package com.campus.ai.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TextChunker {

    public List<String> chunk(String text, int maxChars, int overlapChars) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        if (normalized.length() <= maxChars) {
            return List.of(normalized);
        }

        int safeOverlap = Math.max(0, Math.min(overlapChars, maxChars / 2));
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChars, normalized.length());
            chunks.add(normalized.substring(start, end).trim());
            if (end == normalized.length()) {
                break;
            }
            start = end - safeOverlap;
        }
        return chunks;
    }

    private String normalize(String text) {
        String value = text == null ? "" : text;
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
