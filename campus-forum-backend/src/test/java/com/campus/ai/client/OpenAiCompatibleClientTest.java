package com.campus.ai.client;

import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private AiProperties properties;
    private AtomicReference<RecordedRequest> recordedRequest;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        recordedRequest = new AtomicReference<>();

        properties = new AiProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setApiKey("test-api-key");
        properties.setChatModel("chat-model");
        properties.setEmbeddingModel("embedding-model");
        properties.setTimeoutMs(3000);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createEmbeddingRejectsDisabledAi() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.createEmbedding("hello"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI 功能未启用");
    }

    @Test
    void createChatCompletionRejectsMissingApiKey() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey(" ");
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.createChatCompletion(List.of()))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI API key 未配置");
    }

    @Test
    void createEmbeddingPostsExpectedRequestAndParsesEmbedding() throws Exception {
        properties.setEmbeddingDimension(3);
        respond("/embeddings", 200, """
                {
                  "data": [
                    {
                      "embedding": [0.125, -2.5, 3]
                    }
                  ]
                }
                """);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        List<Double> embedding = client.createEmbedding("hello campus");

        assertThat(embedding).containsExactly(0.125, -2.5, 3.0);
        RecordedRequest request = recordedRequest.get();
        assertThat(request.path()).isEqualTo("/embeddings");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.authorization()).isEqualTo("Bearer test-api-key");
        assertThat(request.contentType()).contains("application/json");
        JsonNode body = objectMapper.readTree(request.body());
        assertThat(body.path("model").asText()).isEqualTo("embedding-model");
        assertThat(body.path("input").asText()).isEqualTo("hello campus");
    }

    @Test
    void createEmbeddingRejectsUnexpectedDimension() {
        properties.setEmbeddingDimension(1536);
        respond("/embeddings", 200, """
                {
                  "data": [
                    {
                      "embedding": [0.125, -2.5, 3]
                    }
                  ]
                }
                """);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.createEmbedding("hello campus"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("embedding 维度不匹配");
    }

    @Test
    void createChatCompletionPostsExpectedRequestAndParsesAnswer() throws Exception {
        respond("/chat/completions", 200, chatResponse("校园回答"));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        String answer = client.createChatCompletion(List.of(
                new AiChatMessage("system", "answer policy"),
                new AiChatMessage("user", "question")
        ));

        assertThat(answer).isEqualTo("校园回答");
        RecordedRequest request = recordedRequest.get();
        assertThat(request.path()).isEqualTo("/chat/completions");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.authorization()).isEqualTo("Bearer test-api-key");
        assertThat(request.contentType()).contains("application/json");
        JsonNode body = objectMapper.readTree(request.body());
        assertThat(body.path("model").asText()).isEqualTo("chat-model");
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(body.path("messages").get(0).path("content").asText()).isEqualTo("answer policy");
        assertThat(body.path("messages").get(1).path("role").asText()).isEqualTo("user");
        assertThat(body.path("messages").get(1).path("content").asText()).isEqualTo("question");
    }

    @Test
    void moderatePromptUsesApprovedTaxonomyAndAllowAction() throws Exception {
        respond("/chat/completions", 200, chatResponse("""
                {
                  "riskLevel": "LOW",
                  "riskTypes": ["ADVERTISEMENT"],
                  "confidence": 0.93,
                  "reasons": ["广告风险"],
                  "suggestedAction": "ALLOW"
                }
                """));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        AiModerationAdvice advice = client.moderate("POST", "二手教材", "出一本线代教材");

        assertThat(advice.getSuggestedAction()).isEqualTo("ALLOW");
        assertThat(advice.getModelName()).isEqualTo("chat-model");
        JsonNode messages = objectMapper.readTree(recordedRequest.get().body()).path("messages");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
        String systemPrompt = messages.get(0).path("content").asText();
        assertThat(systemPrompt)
                .contains("ADVERTISEMENT", "ABUSE", "SCAM", "CONTACT_DIVERSION", "SENSITIVE_INFO", "FLOODING")
                .contains("ALLOW", "REVIEW", "REJECT")
                .doesNotContain("POLITICS", "PORNOGRAPHY", "VIOLENCE", "HARASSMENT", "SPAM", "PRIVACY", "ILLEGAL", "OTHER", "APPROVE");
        assertThat(messages.get(1).path("content").asText())
                .contains("审核对象类型：POST")
                .contains("标题：二手教材")
                .contains("内容：出一本线代教材");
    }

    @Test
    void moderateRejectsUnknownEnumValuesFromProvider() throws Exception {
        respond("/chat/completions", 200, chatResponse("""
                {
                  "riskLevel": "LOW",
                  "riskTypes": ["OTHER"],
                  "confidence": 0.75,
                  "reasons": ["旧枚举"],
                  "suggestedAction": "ALLOW"
                }
                """));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.moderate("POST", "标题", "内容"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI 审核结果包含未知枚举");
    }

    @Test
    void createChatCompletionRejectsMalformedProviderResponse() {
        respond("/chat/completions", 200, """
                {
                  "choices": [
                    {
                      "message": {}
                    }
                  ]
                }
                """);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.createChatCompletion(List.of(new AiChatMessage("user", "hello"))))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI 服务响应格式异常");
    }

    @Test
    void createEmbeddingRejectsMalformedProviderResponse() {
        respond("/embeddings", 200, """
                {
                  "data": [
                    {
                      "embedding": "not an array"
                    }
                  ]
                }
                """);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.createEmbedding("hello"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI 服务响应格式异常");
    }

    @Test
    void nonSuccessProviderResponseIncludesStatusAndSafeBodySnippet() {
        respond("/chat/completions", 429, "provider rejected test-api-key quota " + "x".repeat(600) + " response-tail");
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.createChatCompletion(List.of(new AiChatMessage("user", "hello"))))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI 服务返回错误状态：429")
                .hasMessageContaining("provider rejected")
                .hasMessageContaining("[REDACTED]")
                .hasMessageNotContaining("test-api-key")
                .hasMessageNotContaining("response-tail");
    }

    private void respond(String path, int statusCode, String responseBody) {
        server.createContext(path, exchange -> {
            recordedRequest.set(readRequest(exchange));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    private RecordedRequest readRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new RecordedRequest(
                exchange.getRequestURI().getPath(),
                exchange.getRequestMethod(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                body
        );
    }

    private String chatResponse(String content) throws IOException {
        return objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", content)
                ))
        ));
    }

    private record RecordedRequest(
            String path,
            String method,
            String authorization,
            String contentType,
            String body
    ) {
    }
}
