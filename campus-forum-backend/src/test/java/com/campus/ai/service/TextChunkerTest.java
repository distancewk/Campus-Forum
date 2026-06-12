package com.campus.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {
    @Test
    void chunkNullOrBlankTextReturnsEmptyList() {
        TextChunker chunker = new TextChunker();

        assertThat(chunker.chunk(null, 200, 40)).isEmpty();
        assertThat(chunker.chunk(" \t\n", 200, 40)).isEmpty();
    }

    @Test
    void chunkNormalizesNbspAndRepeatedWhitespace() {
        TextChunker chunker = new TextChunker();

        assertThat(chunker.chunk("  宿舍\u00A0\u00A0报修\t\n一般   当天受理。  ", 200, 40))
                .containsExactly("宿舍 报修 一般 当天受理。");
    }

    @Test
    void chunkUsesConfiguredOverlapBetweenChunks() {
        TextChunker chunker = new TextChunker();

        assertThat(chunker.chunk("abcdefghij", 6, 2))
                .containsExactly("abcdef", "efghij");
    }

    @Test
    void chunkShortTextReturnsSingleChunk() {
        TextChunker chunker = new TextChunker();
        assertThat(chunker.chunk("宿舍报修一般当天受理。", 200, 40))
                .containsExactly("宿舍报修一般当天受理。");
    }

    @Test
    void chunkLongTextCreatesOverlappingChunks() {
        TextChunker chunker = new TextChunker();
        String text = "一二三四五六七八九十".repeat(30);
        assertThat(chunker.chunk(text, 80, 10)).hasSizeGreaterThan(1);
        assertThat(chunker.chunk(text, 80, 10).get(0)).hasSize(80);
    }
}
