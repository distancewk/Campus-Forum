package com.campus.ai.service;

import com.campus.ai.client.AiProviderClient;
import com.campus.ai.client.AiProviderException;
import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiAskResponse;
import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.dto.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuestionAnswerServiceTest {

    @Test
    void askDoesNotWrapProviderCallsInTransaction() throws Exception {
        Method ask = AiQuestionAnswerService.class.getMethod("ask", Long.class, String.class);

        assertThat(ask.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    void askRefusesWhenSourcesAreInsufficient() {
        AiProperties properties = new AiProperties();
        properties.getQa().setMinScore(0.32);
        AiQuestionAnswerService service = new AiQuestionAnswerService(
                new FakeProvider(),
                question -> List.of(),
                null,
                null,
                properties,
                null
        );

        AiAskResponse response = service.ask(1L, "宿舍报修多久处理");

        assertThat(response.getAnswerStatus()).isEqualTo("INSUFFICIENT_SOURCES");
        assertThat(response.getAnswer()).contains("暂未找到足够可靠");
        assertThat(response.getCitations()).isEmpty();
    }

    @Test
    void askReturnsAnswerWithCitationWhenSourceIsAvailable() {
        AiProperties properties = new AiProperties();
        properties.getQa().setMinScore(0.32);
        RetrievedChunk chunk = new RetrievedChunk(10L, 20L, "POST", 30L, "宿舍报修经验", "通常 1-2 天处理", 0.82);
        AiQuestionAnswerService service = new AiQuestionAnswerService(
                new FakeProvider(),
                question -> List.of(chunk),
                null,
                null,
                properties,
                null
        );

        AiAskResponse response = service.ask(1L, "宿舍报修多久处理");

        assertThat(response.getAnswerStatus()).isEqualTo("ANSWERED");
        assertThat(response.getAnswer()).contains("根据论坛资料");
        assertThat(response.getCitations()).hasSize(1);
        assertThat(response.getCitations().get(0).getTitle()).isEqualTo("宿舍报修经验");
    }

    @Test
    void askReturnsFailedWhenProviderIsUnavailable() {
        AiProperties properties = new AiProperties();
        properties.getQa().setMinScore(0.32);
        RetrievedChunk chunk = new RetrievedChunk(10L, 20L, "POST", 30L, "宿舍报修经验", "通常 1-2 天处理", 0.82);
        AiQuestionAnswerService service = new AiQuestionAnswerService(
                new FailingProvider(),
                question -> List.of(chunk),
                null,
                null,
                properties,
                null
        );

        AiAskResponse response = service.ask(1L, "宿舍报修多久处理");

        assertThat(response.getAnswerStatus()).isEqualTo("FAILED");
        assertThat(response.getAnswer()).contains("AI 服务暂时不可用");
        assertThat(response.getCitations()).isEmpty();
    }

    static class FakeProvider implements AiProviderClient {
        public List<Double> createEmbedding(String input) {
            return List.of(0.1, 0.2, 0.3);
        }

        public String createChatCompletion(List<AiChatMessage> messages) {
            return "根据论坛资料，宿舍报修通常 1-2 天处理。";
        }

        public AiModerationAdvice moderate(String targetType, String title, String content) {
            return new AiModerationAdvice("LOW", List.of(), 0.1, List.of(), "ALLOW", "fake");
        }
    }

    static class FailingProvider implements AiProviderClient {
        public List<Double> createEmbedding(String input) {
            throw new AiProviderException("embedding failed");
        }

        public String createChatCompletion(List<AiChatMessage> messages) {
            throw new AiProviderException("chat failed");
        }

        public AiModerationAdvice moderate(String targetType, String title, String content) {
            throw new AiProviderException("moderation failed");
        }
    }
}
