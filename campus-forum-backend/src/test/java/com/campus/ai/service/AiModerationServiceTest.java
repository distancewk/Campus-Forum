package com.campus.ai.service;

import com.campus.ai.client.AiProviderClient;
import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.entity.AiModerationResult;
import com.campus.ai.mapper.AiModerationResultMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiModerationServiceTest {

    @Mock
    private AiModerationResultMapper moderationResultMapper;

    @Test
    void localFallbackFlagsContactDiversionWhenProviderFails() {
        AiProperties properties = new AiProperties();
        properties.getModeration().setEnabled(true);
        AiModerationService service = new AiModerationService(new FailingProvider(), null, properties);

        AiModerationAdvice advice = service.review("POST", "资料", "加我微信 abc123 领资料", 1L, null);

        assertThat(advice.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(advice.getRiskTypes()).contains("CONTACT_DIVERSION");
        assertThat(advice.getSuggestedAction()).isEqualTo("REVIEW");
    }

    @Test
    void bindTargetAndSavePersistsPendingStatusForMediumRisk() {
        AiProperties properties = new AiProperties();
        AiModerationService service = new AiModerationService(new FailingProvider(), moderationResultMapper, properties);
        AiModerationAdvice advice = new AiModerationAdvice(
                "MEDIUM",
                List.of("SCAM"),
                0.81,
                List.of("疑似兼职诈骗"),
                "REVIEW",
                "test-model"
        );

        service.bindTargetAndSave(advice, "COMMENT", 30L, 40L);

        ArgumentCaptor<AiModerationResult> captor = ArgumentCaptor.forClass(AiModerationResult.class);
        verify(moderationResultMapper).insertResult(captor.capture());
        AiModerationResult result = captor.getValue();
        assertThat(result.getTargetType()).isEqualTo("COMMENT");
        assertThat(result.getTargetId()).isEqualTo(30L);
        assertThat(result.getAuthorId()).isEqualTo(40L);
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(result.getRiskTypes()).contains("SCAM");
        assertThat(result.getStatus()).isEqualTo("PENDING_ADMIN");
    }

    static class FailingProvider implements AiProviderClient {
        public List<Double> createEmbedding(String input) {
            throw new RuntimeException("down");
        }

        public String createChatCompletion(List<AiChatMessage> messages) {
            throw new RuntimeException("down");
        }

        public AiModerationAdvice moderate(String targetType, String title, String content) {
            throw new RuntimeException("down");
        }
    }
}
