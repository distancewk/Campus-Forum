package com.campus.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTextExtractorTest {
    @Test
    void extractsTxtContent() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notice.txt",
                "text/plain",
                "打印店在东门附近，营业到晚上十点。".getBytes(StandardCharsets.UTF_8)
        );

        String content = extractor.extract(file);

        assertThat(content).contains("打印店在东门附近");
    }

    @Test
    void rejectsUnsupportedFileType() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notice.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的资料格式");
    }
}
