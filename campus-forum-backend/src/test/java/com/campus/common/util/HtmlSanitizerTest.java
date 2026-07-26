package com.campus.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for HTML sanitization (stored XSS / HTML injection prevention).
 */
class HtmlSanitizerTest {

    @Test
    void cleanPlainStripsScriptTags() {
        assertThat(HtmlSanitizer.cleanPlain("<script>alert(1)</script>hi")).isEqualTo("hi");
    }

    @Test
    void cleanPlainStripsFormattingTags() {
        assertThat(HtmlSanitizer.cleanPlain("<b>x</b>")).isEqualTo("x");
    }

    @Test
    void cleanBasicKeepsTextButRemovesScript() {
        String result = HtmlSanitizer.cleanBasic("<script>alert(1)</script><p>ok</p>");
        assertThat(result).doesNotContain("script");
        assertThat(result).contains("ok");
    }
}
