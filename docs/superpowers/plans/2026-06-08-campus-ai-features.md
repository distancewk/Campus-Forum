# Campus AI Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build AI-powered campus Q&A with cited forum/document sources and AI-assisted moderation that routes medium/high-risk posts and comments to human review.

**Architecture:** Add a focused `com.campus.ai` backend module for provider calls, knowledge ingestion, retrieval, Q&A, moderation, and admin document management. Use PostgreSQL + `pgvector` for knowledge chunks, Redis for user-level Q&A rate limits, and Vue routes for the user Q&A page plus admin AI management screens.

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus, Flyway, PostgreSQL `pgvector`, Redis, Apache PDFBox, Apache POI, Vue 3, Element Plus, Axios.

---

## Execution Notes

- Current workspace has no `.git` directory. Commit steps below are written as conditional commands; they are no-ops in this workspace and become real commits if the project is later placed under git.
- The frontend project to modify is `campus-forum-frontend-new`, not the old `campus-forum-frontend`.
- Do not expose AI API keys to frontend code. All AI provider calls must stay in the backend.
- Keep the model provider swappable through configuration. No controller or service should hard-code a vendor URL or model name.

## File Structure

### Backend Files To Create

- `campus-forum-backend/src/main/resources/db/migration/V4__ai_features.sql`
  - Adds `pgvector`, AI knowledge tables, Q&A tables, moderation table, and comment feature/status indexes.
- `campus-forum-backend/src/main/java/com/campus/ai/config/AiProperties.java`
  - Binds `campus.ai.*` configuration.
- `campus-forum-backend/src/main/java/com/campus/ai/client/AiProviderClient.java`
  - Interface for chat, embedding, and moderation-compatible calls.
- `campus-forum-backend/src/main/java/com/campus/ai/client/OpenAiCompatibleClient.java`
  - Java `HttpClient` implementation for `/chat/completions` and `/embeddings`.
- `campus-forum-backend/src/main/java/com/campus/ai/client/AiProviderException.java`
  - Runtime exception for provider failures.
- `campus-forum-backend/src/main/java/com/campus/ai/dto/*`
  - Request/response DTOs for ask, citations, sessions, moderation, documents.
- `campus-forum-backend/src/main/java/com/campus/ai/entity/*`
  - MyBatis-Plus entities for AI tables.
- `campus-forum-backend/src/main/java/com/campus/ai/mapper/*Mapper.java`
  - Mapper interfaces for AI tables.
- `campus-forum-backend/src/main/resources/mapper/AiKnowledgeMapper.xml`
  - Hybrid retrieval SQL with keyword and vector ranking.
- `campus-forum-backend/src/main/java/com/campus/ai/service/TextChunker.java`
  - Deterministic text splitting.
- `campus-forum-backend/src/main/java/com/campus/ai/service/DocumentTextExtractor.java`
  - Text extraction for PDF, DOCX, TXT, and Markdown.
- `campus-forum-backend/src/main/java/com/campus/ai/service/KnowledgeIngestionService.java`
  - Indexes posts, featured comments, and uploaded documents.
- `campus-forum-backend/src/main/java/com/campus/ai/service/KnowledgeRetriever.java`
  - Functional boundary used by Q&A service and tests.
- `campus-forum-backend/src/main/java/com/campus/ai/service/KnowledgeRetrievalService.java`
  - Runs hybrid retrieval and source filtering.
- `campus-forum-backend/src/main/java/com/campus/ai/service/AiQuestionAnswerService.java`
  - Handles Q&A flow, citations, refusal, and session records.
- `campus-forum-backend/src/main/java/com/campus/ai/service/AiModerationService.java`
  - Produces moderation advice and local fallback results.
- `campus-forum-backend/src/main/java/com/campus/ai/service/AdminKnowledgeService.java`
  - Manages document upload, deletion, and reindex.
- `campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java`
  - User Q&A endpoints.
- `campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java`
  - Admin document and moderation endpoints.

### Backend Files To Modify

- `campus-forum-backend/pom.xml`
  - Add PDFBox and POI dependencies.
- `campus-forum-backend/src/main/resources/application.yml`
  - Add `campus.ai.*` configuration defaults with env var binding.
- `campus-forum-backend/src/main/java/com/campus/post/service/PostService.java`
  - Call moderation before storing published state.
- `campus-forum-backend/src/main/java/com/campus/comment/service/CommentService.java`
  - Call moderation before counting visible comments.
- `campus-forum-backend/src/main/java/com/campus/admin/service/AdminService.java`
  - Add pending comment audit, comment feature toggle, and moderation status updates.
- `campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java`
  - Add comment audit and feature endpoints.
- `campus-forum-backend/src/main/java/com/campus/comment/entity/Comment.java`
  - Add `isFeatured`.
- `campus-forum-backend/src/main/java/com/campus/comment/dto/CommentVO.java`
  - Add `isFeatured`, `status`, and `pendingReview` for admin and submission-state displays.
- `campus-forum-backend/src/main/resources/mapper/CommentMapper.xml`
  - Filter visible comments by `status=1`; add pending comment and featured-comment queries.
- `campus-forum-backend/src/main/java/com/campus/comment/mapper/CommentMapper.java`
  - Add mapper methods used by admin and ingestion.

### Frontend Files To Create

- `campus-forum-frontend-new/src/api/ai.js`
  - User Q&A APIs.
- `campus-forum-frontend-new/src/api/adminAi.js`
  - Admin document, moderation, comment audit APIs.
- `campus-forum-frontend-new/src/views/ai/AiAsk.vue`
  - User Q&A interface with citations and history.
- `campus-forum-frontend-new/src/views/admin/AiKnowledge.vue`
  - Admin document upload and indexing status.

### Frontend Files To Modify

- `campus-forum-frontend-new/src/router/index.js`
  - Add `/ai` and `/admin/ai-knowledge` routes.
- `campus-forum-frontend-new/src/components/layout/Navbar.vue`
  - Add AI Q&A entry.
- `campus-forum-frontend-new/src/components/layout/AdminLayout.vue`
  - Add AI knowledge menu item and breadcrumb title.
- `campus-forum-frontend-new/src/views/admin/ContentAudit.vue`
  - Show post/comment tabs, risk filters, AI reasons, and comment audit actions.
- `campus-forum-frontend-new/src/views/post/PostCreate.vue`
  - Display “submitted for review” state when backend returns pending status.
- `campus-forum-frontend-new/src/components/CommentList.vue`
  - Display “comment submitted for review” state when backend returns pending status.

---

## Task 1: Backend Dependencies, Configuration, And Migration

**Files:**
- Modify: `campus-forum-backend/pom.xml`
- Modify: `campus-forum-backend/src/main/resources/application.yml`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/config/AiProperties.java`
- Create: `campus-forum-backend/src/main/resources/db/migration/V4__ai_features.sql`

- [ ] **Step 1: Add backend dependencies**

Add these dependencies to `campus-forum-backend/pom.xml` after the Commons IO dependency:

```xml
<!-- PDF text extraction for admin-uploaded AI knowledge documents -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.30</version>
</dependency>

<!-- DOCX text extraction for admin-uploaded AI knowledge documents -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

- [ ] **Step 2: Add AI configuration defaults**

Add this block under the existing `campus:` block in `campus-forum-backend/src/main/resources/application.yml`:

```yaml
  ai:
    enabled: ${CAMPUS_AI_ENABLED:false}
    base-url: ${CAMPUS_AI_BASE_URL:https://api.openai.com/v1}
    api-key: ${CAMPUS_AI_API_KEY:}
    chat-model: ${CAMPUS_AI_CHAT_MODEL:gpt-4o-mini}
    embedding-model: ${CAMPUS_AI_EMBEDDING_MODEL:text-embedding-3-small}
    embedding-dimension: ${CAMPUS_AI_EMBEDDING_DIMENSION:1536}
    timeout-ms: ${CAMPUS_AI_TIMEOUT_MS:15000}
    qa:
      max-sources: ${CAMPUS_AI_QA_MAX_SOURCES:6}
      min-score: ${CAMPUS_AI_QA_MIN_SCORE:0.32}
      rate-limit-per-hour: ${CAMPUS_AI_QA_RATE_LIMIT_PER_HOUR:30}
    moderation:
      enabled: ${CAMPUS_AI_MODERATION_ENABLED:true}
      medium-threshold: ${CAMPUS_AI_MODERATION_MEDIUM_THRESHOLD:0.55}
      high-threshold: ${CAMPUS_AI_MODERATION_HIGH_THRESHOLD:0.82}
    document:
      max-file-size: ${CAMPUS_AI_DOCUMENT_MAX_FILE_SIZE:10485760}
```

- [ ] **Step 3: Create `AiProperties`**

Create `campus-forum-backend/src/main/java/com/campus/ai/config/AiProperties.java`:

```java
package com.campus.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "campus.ai")
public class AiProperties {
    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String chatModel;
    private String embeddingModel;
    private int embeddingDimension = 1536;
    private int timeoutMs = 15000;
    private Qa qa = new Qa();
    private Moderation moderation = new Moderation();
    private Document document = new Document();

    @Data
    public static class Qa {
        private int maxSources = 6;
        private double minScore = 0.32;
        private int rateLimitPerHour = 30;
    }

    @Data
    public static class Moderation {
        private boolean enabled = true;
        private double mediumThreshold = 0.55;
        private double highThreshold = 0.82;
    }

    @Data
    public static class Document {
        private long maxFileSize = 10 * 1024 * 1024;
    }
}
```

- [ ] **Step 4: Create Flyway migration**

Create `campus-forum-backend/src/main/resources/db/migration/V4__ai_features.sql`:

```sql
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE comment
    ADD COLUMN IF NOT EXISTS is_featured BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_comment_status_deleted
    ON comment(status, deleted, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT,
    file_url VARCHAR(500),
    file_type VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'INDEXING',
    created_by BIGINT,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT,
    chunk_index INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(1536),
    token_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_chunk_document FOREIGN KEY (document_id) REFERENCES ai_knowledge_document(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_chunk_doc_index
    ON ai_knowledge_chunk(document_id, chunk_index);

CREATE INDEX IF NOT EXISTS idx_ai_chunk_source
    ON ai_knowledge_chunk(source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_ai_chunk_content_trgm
    ON ai_knowledge_chunk USING gin (content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_ai_chunk_embedding_hnsw
    ON ai_knowledge_chunk USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS ai_qa_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    answer_status VARCHAR(30) NOT NULL,
    latency_ms INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_qa_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_ai_qa_user_created
    ON ai_qa_session(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_qa_citation (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    chunk_id BIGINT,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT,
    title VARCHAR(200) NOT NULL,
    snippet TEXT NOT NULL,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_citation_session FOREIGN KEY (session_id) REFERENCES ai_qa_session(id)
);

CREATE INDEX IF NOT EXISTS idx_ai_citation_session
    ON ai_qa_citation(session_id);

CREATE TABLE IF NOT EXISTS ai_moderation_result (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 0,
    reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggested_action VARCHAR(20) NOT NULL,
    model_name VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_moderation_target
    ON ai_moderation_result(target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_ai_moderation_status
    ON ai_moderation_result(status, risk_level, created_at DESC);
```

- [ ] **Step 5: Fix migration dependency on trigram extension**

If `V3__search_and_count_indexes.sql` already creates `pg_trgm`, no change is needed. If it does not, add this line before `idx_ai_chunk_content_trgm` in `V4__ai_features.sql`:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

- [ ] **Step 6: Run backend compile**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -DskipTests compile
```

Expected: compile exits with code `0`.

- [ ] **Step 7: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-backend/pom.xml campus-forum-backend/src/main/resources/application.yml campus-forum-backend/src/main/java/com/campus/ai/config/AiProperties.java campus-forum-backend/src/main/resources/db/migration/V4__ai_features.sql && git commit -m "feat: add ai configuration and schema" || true
```

Expected in current workspace: no output or a git error suppressed by `|| true`, because there is no `.git` directory.

---

## Task 2: AI Provider Client And Deterministic Test Doubles

**Files:**
- Create: `campus-forum-backend/src/main/java/com/campus/ai/client/AiProviderClient.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/client/OpenAiCompatibleClient.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/client/AiProviderException.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AiChatMessage.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AiModerationAdvice.java`
- Create: `campus-forum-backend/src/test/java/com/campus/ai/client/OpenAiCompatibleClientTest.java`

- [ ] **Step 1: Write provider contract**

Create `AiProviderClient.java` with these methods:

```java
package com.campus.ai.client;

import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;

import java.util.List;

public interface AiProviderClient {
    List<Double> createEmbedding(String input);

    String createChatCompletion(List<AiChatMessage> messages);

    AiModerationAdvice moderate(String targetType, String title, String content);
}
```

- [ ] **Step 2: Create shared DTOs**

Create `AiChatMessage.java`:

```java
package com.campus.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessage {
    private String role;
    private String content;
}
```

Create `AiModerationAdvice.java`:

```java
package com.campus.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModerationAdvice {
    private String riskLevel;
    private List<String> riskTypes = new ArrayList<>();
    private double confidence;
    private List<String> reasons = new ArrayList<>();
    private String suggestedAction;
    private String modelName;
}
```

- [ ] **Step 3: Create provider exception**

Create `AiProviderException.java`:

```java
package com.campus.ai.client;

public class AiProviderException extends RuntimeException {
    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Write failing tests for disabled and invalid provider calls**

Create `OpenAiCompatibleClientTest.java`:

```java
package com.campus.ai.client;

import com.campus.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleClientTest {

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
        properties.setApiKey("");
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);

        assertThatThrownBy(() -> client.createChatCompletion(java.util.List.of()))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("AI API key 未配置");
    }
}
```

- [ ] **Step 5: Run tests and verify they fail**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -Dtest=OpenAiCompatibleClientTest test
```

Expected: FAIL because `OpenAiCompatibleClient` does not exist.

- [ ] **Step 6: Implement minimal client guard behavior**

Create `OpenAiCompatibleClient.java` with constructor, guard methods, and method stubs that use Java `HttpClient`:

```java
package com.campus.ai.client;

import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleClient implements AiProviderClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Double> createEmbedding(String input) {
        ensureEnabled();
        ensureApiKey();
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", properties.getEmbeddingModel(),
                    "input", input
            ));
            JsonNode root = post("/embeddings", body);
            return objectMapper.convertValue(
                    root.path("data").get(0).path("embedding"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class)
            );
        } catch (Exception e) {
            throw new AiProviderException("生成 embedding 失败", e);
        }
    }

    @Override
    public String createChatCompletion(List<AiChatMessage> messages) {
        ensureEnabled();
        ensureApiKey();
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", properties.getChatModel(),
                    "messages", messages,
                    "temperature", 0.2
            ));
            JsonNode root = post("/chat/completions", body);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new AiProviderException("生成 AI 回答失败", e);
        }
    }

    @Override
    public AiModerationAdvice moderate(String targetType, String title, String content) {
        String prompt = """
                你是校园论坛审核辅助系统。只输出 JSON，不要输出解释文本。
                targetType=%s
                title=%s
                content=%s
                JSON 字段：riskLevel, riskTypes, confidence, reasons, suggestedAction。
                riskLevel 只能是 LOW, MEDIUM, HIGH。
                suggestedAction 只能是 ALLOW, REVIEW, REJECT。
                """.formatted(targetType, title == null ? "" : title, content == null ? "" : content);
        String json = createChatCompletion(List.of(
                new AiChatMessage("system", "你只输出严格 JSON。"),
                new AiChatMessage("user", prompt)
        ));
        try {
            AiModerationAdvice advice = objectMapper.readValue(json, AiModerationAdvice.class);
            advice.setModelName(properties.getChatModel());
            return advice;
        } catch (Exception e) {
            throw new AiProviderException("解析 AI 审核结果失败", e);
        }
    }

    private JsonNode post(String path, String body) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiProviderException("AI 服务返回错误状态：" + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new AiProviderException("AI 功能未启用");
        }
    }

    private void ensureApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderException("AI API key 未配置");
        }
    }
}
```

- [ ] **Step 7: Run provider tests**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -Dtest=OpenAiCompatibleClientTest test
```

Expected: PASS.

- [ ] **Step 8: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-backend/src/main/java/com/campus/ai campus-forum-backend/src/test/java/com/campus/ai/client/OpenAiCompatibleClientTest.java && git commit -m "feat: add openai-compatible ai client" || true
```

---

## Task 3: Knowledge Entities, Chunking, Extraction, And Ingestion

**Files:**
- Create: `campus-forum-backend/src/main/java/com/campus/ai/entity/AiKnowledgeDocument.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/entity/AiKnowledgeChunk.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/mapper/AiKnowledgeDocumentMapper.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/mapper/AiKnowledgeChunkMapper.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/TextChunker.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/DocumentTextExtractor.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/KnowledgeIngestionService.java`
- Create: `campus-forum-backend/src/test/java/com/campus/ai/service/TextChunkerTest.java`
- Create: `campus-forum-backend/src/test/java/com/campus/ai/service/DocumentTextExtractorTest.java`

- [ ] **Step 1: Write chunker tests**

Create `TextChunkerTest.java`:

```java
package com.campus.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

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
```

- [ ] **Step 2: Implement `TextChunker`**

Create `TextChunker.java`:

```java
package com.campus.ai.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {
    public List<String> chunk(String text, int maxChars, int overlapChars) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (normalized.length() <= maxChars) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int safeOverlap = Math.max(0, Math.min(overlapChars, maxChars / 2));
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
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
```

- [ ] **Step 3: Write extractor tests**

Create `DocumentTextExtractorTest.java`:

```java
package com.campus.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTextExtractorTest {

    @Test
    void extractsTxtContent() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        MockMultipartFile file = new MockMultipartFile("file", "notice.txt", "text/plain", "打印店在东门附近".getBytes());

        assertThat(extractor.extract(file)).contains("打印店在东门附近");
    }

    @Test
    void rejectsUnsupportedFileType() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        MockMultipartFile file = new MockMultipartFile("file", "table.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "x".getBytes());

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的资料格式");
    }
}
```

- [ ] **Step 4: Implement `DocumentTextExtractor`**

Create `DocumentTextExtractor.java`:

```java
package com.campus.ai.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class DocumentTextExtractor {
    public String extract(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (filename.endsWith(".txt") || filename.endsWith(".md") || filename.endsWith(".markdown")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            if (filename.endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    return new PDFTextStripper().getText(document);
                }
            }
            if (filename.endsWith(".docx")) {
                try (InputStream input = file.getInputStream(); XWPFDocument document = new XWPFDocument(input)) {
                    return document.getParagraphs().stream()
                            .map(paragraph -> paragraph.getText())
                            .collect(Collectors.joining("\n"));
                }
            }
            throw new IllegalArgumentException("不支持的资料格式");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("资料文本抽取失败", e);
        }
    }
}
```

- [ ] **Step 5: Create entities and mappers**

Create entities with MyBatis-Plus annotations:

```java
@Data
@TableName("ai_knowledge_document")
public class AiKnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String sourceType;
    private Long sourceId;
    private String fileUrl;
    private String fileType;
    private String status;
    private Long createdBy;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
@Data
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String sourceType;
    private Long sourceId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private String contentHash;
    private String embedding;
    private Integer tokenCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Mapper interfaces:

```java
@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {
}
```

```java
@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunk> {
    void insertChunk(AiKnowledgeChunk chunk);
    void deleteByDocumentId(Long documentId);
}
```

- [ ] **Step 6: Implement vector string handling in XML**

Create `campus-forum-backend/src/main/resources/mapper/AiKnowledgeMapper.xml` with insert SQL for vector casts:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.campus.ai.mapper.AiKnowledgeChunkMapper">
    <insert id="insertChunk" parameterType="com.campus.ai.entity.AiKnowledgeChunk">
        INSERT INTO ai_knowledge_chunk
        (document_id, source_type, source_id, chunk_index, title, content, content_hash, embedding, token_count, created_at, updated_at)
        VALUES
        (#{documentId}, #{sourceType}, #{sourceId}, #{chunkIndex}, #{title}, #{content}, #{contentHash},
         CAST(#{embedding} AS vector), #{tokenCount}, #{createdAt}, #{updatedAt})
    </insert>

    <delete id="deleteByDocumentId">
        DELETE FROM ai_knowledge_chunk WHERE document_id = #{documentId}
    </delete>
</mapper>
```

- [ ] **Step 7: Implement ingestion service**

Create `KnowledgeIngestionService.java` with these public methods:

```java
public void indexPublishedPost(Long postId);
public void indexFeaturedComment(Long commentId);
public AiKnowledgeDocument indexUploadedDocument(String title, String fileUrl, String fileType, MultipartFile file, Long adminId);
public void deleteDocumentChunks(Long documentId);
```

Core implementation requirements:

```java
private String toVectorLiteral(List<Double> embedding) {
    return embedding.stream()
            .map(value -> String.format(java.util.Locale.US, "%.8f", value))
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
}

private String sha256(String content) {
    try {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 不可用", e);
    }
}
```

- [ ] **Step 8: Run chunker and extractor tests**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -Dtest=TextChunkerTest,DocumentTextExtractorTest test
```

Expected: PASS.

- [ ] **Step 9: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-backend/src/main/java/com/campus/ai campus-forum-backend/src/main/resources/mapper/AiKnowledgeMapper.xml campus-forum-backend/src/test/java/com/campus/ai/service && git commit -m "feat: add ai knowledge ingestion primitives" || true
```

---

## Task 4: Hybrid Retrieval And Q&A API

**Files:**
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AiAskRequest.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AiAskResponse.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AiCitationVO.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AiSessionVO.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/RetrievedChunk.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/entity/AiQaSession.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/entity/AiQaCitation.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/mapper/AiQaSessionMapper.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/mapper/AiQaCitationMapper.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/KnowledgeRetriever.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/ai/mapper/AiKnowledgeChunkMapper.java`
- Modify: `campus-forum-backend/src/main/resources/mapper/AiKnowledgeMapper.xml`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/KnowledgeRetrievalService.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/AiQuestionAnswerService.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java`
- Create: `campus-forum-backend/src/test/java/com/campus/ai/service/AiQuestionAnswerServiceTest.java`

- [ ] **Step 1: Write Q&A service tests**

Create `AiQuestionAnswerServiceTest.java` with a fake provider and fake retrieval service:

```java
package com.campus.ai.service;

import com.campus.ai.client.AiProviderClient;
import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiAskResponse;
import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.dto.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuestionAnswerServiceTest {

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
}
```

- [ ] **Step 2: Create Q&A DTOs**

Use these fields:

```java
@Data
public class AiAskRequest {
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500字")
    private String question;
}
```

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {
    private Long chunkId;
    private Long documentId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String content;
    private double score;
}
```

```java
@Data
public class AiCitationVO {
    private Long chunkId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String snippet;
    private double score;
}
```

```java
@Data
public class AiAskResponse {
    private String answerStatus;
    private String answer;
    private List<AiCitationVO> citations = new ArrayList<>();
    private List<PostListVO> relatedPosts = new ArrayList<>();
}
```

- [ ] **Step 3: Add retrieval mapper method**

Add to `AiKnowledgeChunkMapper.java`:

```java
List<RetrievedChunk> hybridSearch(@Param("keyword") String keyword,
                                  @Param("embedding") String embedding,
                                  @Param("limit") int limit);
```

Add this select to `AiKnowledgeMapper.xml`:

```xml
<select id="hybridSearch" resultType="com.campus.ai.dto.RetrievedChunk">
    SELECT
        c.id AS chunk_id,
        c.document_id,
        c.source_type,
        c.source_id,
        c.title,
        c.content,
        (
            0.65 * (1 - (c.embedding &lt;=&gt; CAST(#{embedding} AS vector))) +
            0.35 * CASE
                WHEN c.content ILIKE CONCAT('%', #{keyword}, '%') OR c.title ILIKE CONCAT('%', #{keyword}, '%')
                THEN 1 ELSE 0
            END
        ) AS score
    FROM ai_knowledge_chunk c
    JOIN ai_knowledge_document d ON c.document_id = d.id
    WHERE d.status = 'ACTIVE'
      AND c.embedding IS NOT NULL
    ORDER BY score DESC
    LIMIT #{limit}
</select>
```

- [ ] **Step 4: Implement retrieval service as a functional dependency**

Create `KnowledgeRetriever.java`:

```java
package com.campus.ai.service;

import com.campus.ai.dto.RetrievedChunk;

import java.util.List;

@FunctionalInterface
public interface KnowledgeRetriever {
    List<RetrievedChunk> retrieve(String question);
}
```

Create `KnowledgeRetrievalService.java`:

```java
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

    public List<RetrievedChunk> retrieve(String question) {
        List<Double> embedding = aiProviderClient.createEmbedding(question);
        String vector = embedding.stream()
                .map(value -> String.format(Locale.US, "%.8f", value))
                .collect(Collectors.joining(",", "[", "]"));
        int limit = Math.max(properties.getQa().getMaxSources() * 4, 12);
        return chunkMapper.hybridSearch(question, vector, limit);
    }
}
```

- [ ] **Step 5: Implement Q&A service**

Create `AiQuestionAnswerService.java` with constructor dependencies:

```java
private final AiProviderClient aiProviderClient;
private final KnowledgeRetriever knowledgeRetriever;
private final AiQaSessionMapper sessionMapper;
private final AiQaCitationMapper citationMapper;
private final AiProperties properties;
private final RedisUtil redisUtil;
```

Core public method:

```java
@Transactional
public AiAskResponse ask(Long userId, String question) {
    long started = System.currentTimeMillis();
    enforceRateLimit(userId);
    List<RetrievedChunk> chunks = knowledgeRetriever.retrieve(question).stream()
            .filter(chunk -> chunk.getScore() >= properties.getQa().getMinScore())
            .limit(properties.getQa().getMaxSources())
            .toList();
    if (chunks.isEmpty()) {
        return saveAndReturnInsufficient(userId, question, started);
    }
    String answer = aiProviderClient.createChatCompletion(buildMessages(question, chunks));
    return saveAndReturnAnswered(userId, question, answer, chunks, started);
}
```

Prompt builder must include this system message:

```java
new AiChatMessage("system", "你是校园论坛智能问答助手。只能基于用户提供的引用资料回答。资料不足时必须说明暂未找到足够可靠资料。不要编造地点、流程、时间、联系人或政策。")
```

Rate limit key:

```java
String key = "ai:qa:hour:" + userId + ":" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH"));
```

- [ ] **Step 6: Implement controller**

Create `AiController.java`:

```java
package com.campus.ai.controller;

import com.campus.ai.dto.AiAskRequest;
import com.campus.ai.dto.AiAskResponse;
import com.campus.ai.service.AiQuestionAnswerService;
import com.campus.common.response.R;
import com.campus.common.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiQuestionAnswerService aiQuestionAnswerService;

    @PostMapping("/ask")
    public R<AiAskResponse> ask(@RequestBody @Valid AiAskRequest request) {
        Long userId = SecurityUtil.requireCurrentUserId();
        return R.ok(aiQuestionAnswerService.ask(userId, request.getQuestion()));
    }
}
```

- [ ] **Step 7: Run Q&A tests**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -Dtest=AiQuestionAnswerServiceTest test
```

Expected: PASS.

- [ ] **Step 8: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-backend/src/main/java/com/campus/ai campus-forum-backend/src/main/resources/mapper/AiKnowledgeMapper.xml campus-forum-backend/src/test/java/com/campus/ai/service/AiQuestionAnswerServiceTest.java && git commit -m "feat: add ai qna retrieval api" || true
```

---

## Task 5: AI Moderation Service And Post/Comment Integration

**Files:**
- Create: `campus-forum-backend/src/main/java/com/campus/ai/entity/AiModerationResult.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/mapper/AiModerationResultMapper.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/AiModerationService.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/post/service/PostService.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/comment/service/CommentService.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/post/dto/PostVO.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/comment/dto/CommentVO.java`
- Test: `campus-forum-backend/src/test/java/com/campus/ai/service/AiModerationServiceTest.java`
- Test: `campus-forum-backend/src/test/java/com/campus/post/service/PostServiceAiModerationTest.java`
- Test: `campus-forum-backend/src/test/java/com/campus/comment/service/CommentServiceAiModerationTest.java`

- [ ] **Step 1: Write moderation service tests**

Create `AiModerationServiceTest.java`:

```java
package com.campus.ai.service;

import com.campus.ai.client.AiProviderClient;
import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiModerationAdvice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiModerationServiceTest {

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

    static class FailingProvider implements AiProviderClient {
        public List<Double> createEmbedding(String input) { throw new RuntimeException("down"); }
        public String createChatCompletion(List<com.campus.ai.dto.AiChatMessage> messages) { throw new RuntimeException("down"); }
        public AiModerationAdvice moderate(String targetType, String title, String content) { throw new RuntimeException("down"); }
    }
}
```

- [ ] **Step 2: Implement `AiModerationResult` and mapper**

Entity fields must match migration:

```java
@Data
@TableName("ai_moderation_result")
public class AiModerationResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private Long authorId;
    private String riskLevel;
    private String riskTypes;
    private Double confidence;
    private String reasons;
    private String suggestedAction;
    private String modelName;
    private String status;
    private LocalDateTime createdAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
}
```

Mapper:

```java
@Mapper
public interface AiModerationResultMapper extends BaseMapper<AiModerationResult> {
}
```

- [ ] **Step 3: Implement moderation service**

Create `AiModerationService.java` with:

```java
public AiModerationAdvice review(String targetType, String title, String content, Long authorId, Long targetId)
public void bindTargetAndSave(AiModerationAdvice advice, String targetType, Long targetId, Long authorId)
public void markAdminReviewed(String targetType, Long targetId, String status, Long adminId)
```

Local fallback rules:

```java
private AiModerationAdvice localFallback(String content) {
    String text = content == null ? "" : content;
    List<String> types = new ArrayList<>();
    if (text.matches("(?is).*(微信|vx|QQ|q群|手机号|电话|加群).*")) {
        types.add("CONTACT_DIVERSION");
    }
    if (text.matches("(?is).*(刷单|兼职日结|返利|稳赚|贷款|中奖).*")) {
        types.add("SCAM");
    }
    if (text.length() > 20 && text.chars().distinct().count() < 8) {
        types.add("FLOODING");
    }
    if (types.isEmpty()) {
        return new AiModerationAdvice("LOW", List.of(), 0.1, List.of("AI 服务不可用，本地规则未命中"), "ALLOW", "local-fallback");
    }
    return new AiModerationAdvice("MEDIUM", types, 0.6, List.of("AI 服务不可用，本地兜底规则命中"), "REVIEW", "local-fallback");
}
```

- [ ] **Step 4: Integrate moderation in post creation**

Modify `PostService.createPost`:

```java
AiModerationAdvice advice = aiModerationService.review("POST", post.getTitle(), sanitizedContent, currentUserId, null);
boolean pendingReview = !"LOW".equals(advice.getRiskLevel());
post.setStatus(pendingReview ? 0 : 1);
postMapper.insert(post);
aiModerationService.bindTargetAndSave(advice, "POST", post.getId(), currentUserId);
vo.setStatus(post.getStatus());
vo.setPendingReview(pendingReview);
```

Add fields to `PostVO`:

```java
private Integer status;
private Boolean pendingReview;
```

- [ ] **Step 5: Integrate moderation in comment creation**

Modify `CommentService.createComment`:

```java
AiModerationAdvice advice = aiModerationService.review("COMMENT", null, safeContent, currentUserId, null);
boolean pendingReview = !"LOW".equals(advice.getRiskLevel());
comment.setStatus(pendingReview ? 0 : 1);
commentMapper.insert(comment);
aiModerationService.bindTargetAndSave(advice, "COMMENT", comment.getId(), currentUserId);
if (!pendingReview) {
    postMapper.updateCommentCount(postId, 1);
}
CommentVO vo = buildCommentVO(comment, currentUserId);
vo.setStatus(comment.getStatus());
vo.setPendingReview(pendingReview);
return vo;
```

Add fields to `CommentVO`:

```java
private Integer status;
private Boolean pendingReview;
private Boolean isFeatured;
```

- [ ] **Step 6: Make existing comment lists show only published comments**

Modify comment queries in `CommentMapper.xml` so all front-facing comment list queries include:

```sql
AND c.deleted = 0
AND c.status = 1
```

- [ ] **Step 7: Run moderation tests**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -Dtest=AiModerationServiceTest,PostServiceAiModerationTest,CommentServiceAiModerationTest test
```

Expected: PASS after the service tests are completed with Mockito for `PostMapper`, `CommentMapper`, `BoardMapper`, `AiModerationService`, and `LikeMapper`.

- [ ] **Step 8: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-backend/src/main/java campus-forum-backend/src/main/resources/mapper/CommentMapper.xml campus-forum-backend/src/test/java/com/campus/ai/service campus-forum-backend/src/test/java/com/campus/post/service campus-forum-backend/src/test/java/com/campus/comment/service && git commit -m "feat: add ai moderation flow" || true
```

---

## Task 6: Admin Backend For Documents, Moderation, And Comment Review

**Files:**
- Create: `campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/service/AdminKnowledgeService.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AdminAiDocumentVO.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AdminModerationQuery.java`
- Create: `campus-forum-backend/src/main/java/com/campus/ai/dto/AdminModerationVO.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/admin/service/AdminService.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java`
- Modify: `campus-forum-backend/src/main/java/com/campus/comment/mapper/CommentMapper.java`
- Modify: `campus-forum-backend/src/main/resources/mapper/CommentMapper.xml`
- Test: `campus-forum-backend/src/test/java/com/campus/admin/service/AdminServiceCommentAuditTest.java`

- [ ] **Step 1: Add admin document service methods**

Create `AdminKnowledgeService.java` with:

```java
public AdminAiDocumentVO upload(MultipartFile file, String title)
public PageResult<AdminAiDocumentVO> list(PageQuery query)
public void delete(Long documentId)
public void reindex(Long documentId)
```

Upload must:

```java
Long adminId = SecurityUtil.requireCurrentUserId();
String filename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
String fileType = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
AiKnowledgeDocument document = ingestionService.indexUploadedDocument(title, "/uploads/ai/" + filename, fileType, file, adminId);
return AdminAiDocumentVO.from(document);
```

- [ ] **Step 2: Add admin AI controller**

Create `AdminAiController.java`:

```java
package com.campus.ai.controller;

import com.campus.ai.dto.AdminAiDocumentVO;
import com.campus.ai.dto.AdminModerationQuery;
import com.campus.ai.dto.AdminModerationVO;
import com.campus.ai.service.AdminKnowledgeService;
import com.campus.ai.service.AiModerationService;
import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {
    private final AdminKnowledgeService adminKnowledgeService;
    private final AiModerationService aiModerationService;

    @PostMapping("/documents")
    public R<AdminAiDocumentVO> uploadDocument(@RequestPart("file") MultipartFile file,
                                               @RequestParam("title") String title) {
        return R.ok(adminKnowledgeService.upload(file, title));
    }

    @GetMapping("/documents")
    public R<PageResult<AdminAiDocumentVO>> listDocuments(@Valid PageQuery query) {
        return R.ok(adminKnowledgeService.list(query));
    }

    @DeleteMapping("/documents/{id}")
    public R<Void> deleteDocument(@PathVariable Long id) {
        adminKnowledgeService.delete(id);
        return R.ok();
    }

    @PostMapping("/documents/{id}/reindex")
    public R<Void> reindexDocument(@PathVariable Long id) {
        adminKnowledgeService.reindex(id);
        return R.ok();
    }

    @GetMapping("/moderation")
    public R<PageResult<AdminModerationVO>> listModeration(@Valid AdminModerationQuery query) {
        return R.ok(aiModerationService.listForAdmin(query));
    }
}
```

- [ ] **Step 3: Add moderation admin listing**

Add this method to `AiModerationService`:

```java
public PageResult<AdminModerationVO> listForAdmin(AdminModerationQuery query) {
    int offset = query.getOffset();
    int size = query.getSize();
    List<AdminModerationVO> records = moderationResultMapper.selectAdminModerationList(
            query.getTargetType(), query.getRiskLevel(), query.getRiskType(), offset, size);
    long total = moderationResultMapper.countAdminModerationList(
            query.getTargetType(), query.getRiskLevel(), query.getRiskType());
    return new PageResult<>(records, total, query.getPage(), query.getSize());
}
```

Add these mapper methods to `AiModerationResultMapper`:

```java
List<AdminModerationVO> selectAdminModerationList(@Param("targetType") String targetType,
                                                  @Param("riskLevel") String riskLevel,
                                                  @Param("riskType") String riskType,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

long countAdminModerationList(@Param("targetType") String targetType,
                              @Param("riskLevel") String riskLevel,
                              @Param("riskType") String riskType);
```

Create `campus-forum-backend/src/main/resources/mapper/AiModerationResultMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.campus.ai.mapper.AiModerationResultMapper">
    <select id="selectAdminModerationList" resultType="com.campus.ai.dto.AdminModerationVO">
        SELECT
            id,
            target_type,
            target_id,
            author_id,
            risk_level,
            risk_types::text AS risk_types,
            confidence,
            reasons::text AS reasons,
            suggested_action,
            status,
            created_at
        FROM ai_moderation_result
        WHERE 1 = 1
        <if test="targetType != null and targetType != ''">
            AND target_type = #{targetType}
        </if>
        <if test="riskLevel != null and riskLevel != ''">
            AND risk_level = #{riskLevel}
        </if>
        <if test="riskType != null and riskType != ''">
            AND jsonb_exists(risk_types, #{riskType})
        </if>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <select id="countAdminModerationList" resultType="long">
        SELECT COUNT(*)
        FROM ai_moderation_result
        WHERE 1 = 1
        <if test="targetType != null and targetType != ''">
            AND target_type = #{targetType}
        </if>
        <if test="riskLevel != null and riskLevel != ''">
            AND risk_level = #{riskLevel}
        </if>
        <if test="riskType != null and riskType != ''">
            AND jsonb_exists(risk_types, #{riskType})
        </if>
    </select>
</mapper>
```

- [ ] **Step 4: Add comment audit methods to `AdminService`**

Add:

```java
public PageResult<CommentVO> listPendingComments(PageQuery query)
public void auditComment(Long commentId, boolean approved)
public void toggleCommentFeature(Long commentId)
```

`auditComment` must:

```java
Comment comment = commentMapper.selectById(commentId);
if (comment == null) {
    throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
}
Long adminId = SecurityUtil.requireCurrentUserId();
if (approved) {
    comment.setStatus(1);
    commentMapper.updateById(comment);
    postMapper.updateCommentCount(comment.getPostId(), 1);
    aiModerationService.markAdminReviewed("COMMENT", commentId, "ADMIN_APPROVED", adminId);
} else {
    comment.setStatus(-1);
    comment.setDeleted(1);
    commentMapper.updateById(comment);
    aiModerationService.markAdminReviewed("COMMENT", commentId, "ADMIN_REJECTED", adminId);
}
```

- [ ] **Step 5: Add admin controller endpoints**

Add to `AdminController.java`:

```java
@GetMapping("/comments/pending")
public R<PageResult<CommentVO>> listPendingComments(@Valid PageQuery query) {
    return R.ok(adminService.listPendingComments(query));
}

@PutMapping("/comments/{id}/audit")
public R<Void> auditComment(@PathVariable Long id, @RequestBody @Valid AuditRequest request) {
    adminService.auditComment(id, request.getApproved());
    return R.ok();
}

@PutMapping("/comments/{id}/feature")
public R<Void> toggleCommentFeature(@PathVariable Long id) {
    adminService.toggleCommentFeature(id);
    return R.ok();
}
```

- [ ] **Step 6: Update moderation status when posts are audited**

Modify existing `AdminService.auditPost`:

```java
Long adminId = SecurityUtil.requireCurrentUserId();
if (approved) {
    post.setStatus(1);
    postMapper.updateById(post);
    aiModerationService.markAdminReviewed("POST", postId, "ADMIN_APPROVED", adminId);
} else {
    post.setStatus(-1);
    post.setDeleted(1);
    postMapper.updateById(post);
    aiModerationService.markAdminReviewed("POST", postId, "ADMIN_REJECTED", adminId);
}
```

- [ ] **Step 7: Run admin tests**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -Dtest=AdminServiceCommentAuditTest test
```

Expected: PASS.

- [ ] **Step 8: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-backend/src/main/java/com/campus/ai campus-forum-backend/src/main/java/com/campus/admin campus-forum-backend/src/main/java/com/campus/comment campus-forum-backend/src/main/resources/mapper/CommentMapper.xml campus-forum-backend/src/test/java/com/campus/admin/service/AdminServiceCommentAuditTest.java && git commit -m "feat: add admin ai review tools" || true
```

---

## Task 7: Frontend User AI Q&A

**Files:**
- Create: `campus-forum-frontend-new/src/api/ai.js`
- Create: `campus-forum-frontend-new/src/views/ai/AiAsk.vue`
- Modify: `campus-forum-frontend-new/src/router/index.js`
- Modify: `campus-forum-frontend-new/src/components/layout/Navbar.vue`

- [ ] **Step 1: Add user AI API module**

Create `campus-forum-frontend-new/src/api/ai.js`:

```js
import request from '@/utils/request'

export function askAi(question) {
  return request.post('/ai/ask', { question })
}

export function getAiSessions(params) {
  return request.get('/ai/sessions', { params })
}

export function getAiSession(id) {
  return request.get(`/ai/sessions/${id}`)
}
```

- [ ] **Step 2: Add route**

Add child route under the main layout in `router/index.js`:

```js
{
  path: 'ai',
  name: 'AiAsk',
  component: () => import('@/views/ai/AiAsk.vue'),
  meta: { requiresAuth: true }
}
```

- [ ] **Step 3: Create `AiAsk.vue`**

Create a page with this behavior:

```vue
<template>
  <div class="ai-ask-page">
    <section class="ask-panel">
      <div class="ask-heading">
        <p class="eyebrow">校园智能问答</p>
        <h1>基于论坛资料回答校园问题</h1>
      </div>
      <el-input
        v-model="question"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="例如：学校附近哪里可以打印论文？"
      />
      <div class="ask-actions">
        <el-button type="primary" :loading="loading" @click="submitQuestion">
          提问
        </el-button>
      </div>
    </section>

    <section v-if="answerStatus" class="answer-panel">
      <el-alert
        v-if="answerStatus === 'INSUFFICIENT_SOURCES'"
        type="warning"
        :closable="false"
        title="暂未找到足够可靠的校园论坛资料"
      />
      <div v-else class="answer-text">{{ answer }}</div>

      <div v-if="citations.length" class="citations">
        <h2>引用来源</h2>
        <button
          v-for="citation in citations"
          :key="`${citation.sourceType}-${citation.sourceId}-${citation.chunkId}`"
          class="citation"
          type="button"
          @click="openCitation(citation)"
        >
          <span>{{ citation.title }}</span>
          <small>{{ citation.sourceType }} · 相关度 {{ Math.round(citation.score * 100) }}%</small>
          <p>{{ citation.snippet }}</p>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { askAi } from '@/api/ai'

const router = useRouter()
const question = ref('')
const loading = ref(false)
const answer = ref('')
const answerStatus = ref('')
const citations = ref([])

const submitQuestion = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  loading.value = true
  try {
    const res = await askAi(question.value.trim())
    answerStatus.value = res.data?.answerStatus || ''
    answer.value = res.data?.answer || ''
    citations.value = res.data?.citations || []
  } finally {
    loading.value = false
  }
}

const openCitation = (citation) => {
  if (citation.sourceType === 'POST' || citation.sourceType === 'COMMENT') {
    router.push(`/post/${citation.sourceId}`)
  }
}
</script>
```

Style requirements:

```scss
.ai-ask-page {
  max-width: 960px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.ask-panel,
.answer-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 22px;
}

.citation {
  width: 100%;
  text-align: left;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  padding: 14px;
  cursor: pointer;
}
```

- [ ] **Step 4: Add navbar entry**

In `Navbar.vue`, add an AI entry near search/post actions:

```vue
<el-button class="ai-link" @click="$router.push('/ai')">
  <el-icon><ChatDotRound /></el-icon>
  AI 问答
</el-button>
```

Import `ChatDotRound` from `@element-plus/icons-vue`.

- [ ] **Step 5: Build frontend**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-frontend-new
npm run build
```

Expected: build exits with code `0`. Existing Rolldown warnings from dependencies are acceptable if there are no project compile errors.

- [ ] **Step 6: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-frontend-new/src/api/ai.js campus-forum-frontend-new/src/views/ai/AiAsk.vue campus-forum-frontend-new/src/router/index.js campus-forum-frontend-new/src/components/layout/Navbar.vue && git commit -m "feat: add ai qna frontend" || true
```

---

## Task 8: Frontend Admin AI Management And Audit Enhancements

**Files:**
- Create: `campus-forum-frontend-new/src/api/adminAi.js`
- Create: `campus-forum-frontend-new/src/views/admin/AiKnowledge.vue`
- Modify: `campus-forum-frontend-new/src/api/admin.js`
- Modify: `campus-forum-frontend-new/src/router/index.js`
- Modify: `campus-forum-frontend-new/src/components/layout/AdminLayout.vue`
- Modify: `campus-forum-frontend-new/src/views/admin/ContentAudit.vue`
- Modify: `campus-forum-frontend-new/src/views/post/PostCreate.vue`
- Modify: `campus-forum-frontend-new/src/components/CommentList.vue`

- [ ] **Step 1: Add admin AI API**

Create `adminAi.js`:

```js
import request from '@/utils/request'

export function uploadAiDocument(formData) {
  return request.post('/admin/ai/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getAiDocuments(params) {
  return request.get('/admin/ai/documents', { params })
}

export function deleteAiDocument(id) {
  return request.delete(`/admin/ai/documents/${id}`)
}

export function reindexAiDocument(id) {
  return request.post(`/admin/ai/documents/${id}/reindex`)
}

export function getAiModeration(params) {
  return request.get('/admin/ai/moderation', { params })
}
```

- [ ] **Step 2: Extend admin API**

Add to `admin.js`:

```js
export function getPendingComments(params) {
  return request.get('/admin/comments/pending', { params })
}

export function auditComment(id, approved) {
  return request.put(`/admin/comments/${id}/audit`, { approved })
}

export function toggleCommentFeature(id) {
  return request.put(`/admin/comments/${id}/feature`)
}
```

- [ ] **Step 3: Add admin route and menu**

Add route:

```js
{
  path: 'ai-knowledge',
  name: 'AiKnowledge',
  component: () => import('@/views/admin/AiKnowledge.vue')
}
```

Add menu item in `AdminLayout.vue`:

```vue
<el-menu-item index="/admin/ai-knowledge">
  <el-icon><Files /></el-icon>
  <span>AI 知识库</span>
</el-menu-item>
```

Add title map:

```js
'/admin/ai-knowledge': 'AI 知识库'
```

- [ ] **Step 4: Create AI knowledge page**

Create `AiKnowledge.vue` with upload form, table, delete, and reindex actions:

```vue
<template>
  <div class="ai-knowledge-page">
    <el-card>
      <template #header>
        <span>AI 知识库资料</span>
      </template>
      <el-form class="upload-form" @submit.prevent>
        <el-input v-model="title" placeholder="资料标题" />
        <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange">
          <el-button>选择文件</el-button>
        </el-upload>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传并索引</el-button>
      </el-form>
      <el-table :data="documents" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="fileType" label="格式" width="100" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="handleReindex(row.id)">重建</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
```

Script must call `uploadAiDocument`, `getAiDocuments`, `deleteAiDocument`, and `reindexAiDocument`.

- [ ] **Step 5: Enhance content audit page**

Modify `ContentAudit.vue` to use tabs:

```vue
<el-tabs v-model="activeTab" @tab-change="fetchRecords">
  <el-tab-pane label="待审帖子" name="posts" />
  <el-tab-pane label="待审评论" name="comments" />
</el-tabs>
```

For comment tab, call `getPendingComments`, `auditComment`. Add columns:

```vue
<el-table-column prop="riskLevel" label="风险" width="100" />
<el-table-column prop="riskTypes" label="类型" min-width="160" />
<el-table-column prop="aiReasons" label="AI 原因" min-width="220" />
```

- [ ] **Step 6: Surface pending review states in post and comment UI**

In `PostCreate.vue`, after create response:

```js
if (res.data?.pendingReview) {
  ElMessage.success('内容已提交，等待管理员审核')
  router.push('/')
  return
}
```

In `CommentList.vue`, after create response:

```js
if (res.data?.pendingReview) {
  ElMessage.success('评论已提交，等待管理员审核')
  commentContent.value = ''
  return
}
```

- [ ] **Step 7: Build frontend**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-frontend-new
npm run build
```

Expected: build exits with code `0`.

- [ ] **Step 8: Conditional commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git add campus-forum-frontend-new/src/api campus-forum-frontend-new/src/views/admin campus-forum-frontend-new/src/router/index.js campus-forum-frontend-new/src/components/layout/AdminLayout.vue campus-forum-frontend-new/src/views/post/PostCreate.vue campus-forum-frontend-new/src/components/CommentList.vue && git commit -m "feat: add admin ai management frontend" || true
```

---

## Task 9: End-To-End Verification And Operational Checks

**Files:**
- Verify: `campus-forum-backend/src/main/resources/application.yml`
- Verify: `campus-forum-backend/src/main/resources/db/migration/V4__ai_features.sql`
- Verify: `campus-forum-frontend-new/src/router/index.js`
- Verify: local running app at `http://127.0.0.1:5173/`

- [ ] **Step 1: Run full backend tests**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn test
```

Expected: all tests pass.

- [ ] **Step 2: Run frontend build**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-frontend-new
npm run build
```

Expected: build exits with code `0`.

- [ ] **Step 3: Verify migration applies**

With PostgreSQL running, run:

```bash
cd /Users/distancewk/dev/Campus\ Forum/campus-forum-backend
mvn -q -DskipTests spring-boot:run
```

Expected logs:

```text
Successfully applied
V4__ai_features.sql
Started CampusForumApplication
```

If the local database already has an edited migration checksum from prior work, run Flyway repair only after confirming the checksum mismatch is for a local non-production database.

- [ ] **Step 4: Verify user Q&A auth guard**

Open `http://127.0.0.1:5173/ai` while logged out.

Expected: app redirects to `/login?redirect=/ai`.

- [ ] **Step 5: Verify admin routes**

Log in as an admin and open:

```text
http://127.0.0.1:5173/admin/ai-knowledge
http://127.0.0.1:5173/admin/content
```

Expected:

- AI knowledge page renders upload controls and document table.
- Content audit page renders post/comment tabs.
- No console errors.
- No horizontal overflow on desktop or mobile viewport.

- [ ] **Step 6: Verify AI disabled behavior**

Run backend with:

```bash
CAMPUS_AI_ENABLED=false mvn -q spring-boot:run
```

Expected:

- Q&A returns a controlled AI unavailable response.
- Posting/commenting does not return a 500 solely because AI is disabled.
- Local moderation fallback still routes obvious contact diversion text to pending review.

- [ ] **Step 7: Verify AI enabled behavior with configured provider**

Run backend with real environment values:

```bash
test -n "$CAMPUS_AI_API_KEY" && \
CAMPUS_AI_ENABLED=true \
CAMPUS_AI_BASE_URL="${CAMPUS_AI_BASE_URL:-https://api.openai.com/v1}" \
CAMPUS_AI_CHAT_MODEL="${CAMPUS_AI_CHAT_MODEL:-gpt-4o-mini}" \
CAMPUS_AI_EMBEDDING_MODEL="${CAMPUS_AI_EMBEDDING_MODEL:-text-embedding-3-small}" \
mvn -q spring-boot:run
```

Expected:

- Uploading a TXT document creates `ACTIVE` document status and chunks.
- Asking a question related to that document returns `ANSWERED`.
- Citations list includes the uploaded document title.
- Asking an unrelated question returns `INSUFFICIENT_SOURCES`.

- [ ] **Step 8: Verify no secret leakage**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
rg -n "CAMPUS_AI_API_KEY|sk-|Bearer " campus-forum-frontend-new/src campus-forum-backend/src/main/resources campus-forum-backend/src/main/java
```

Expected:

- `CAMPUS_AI_API_KEY` appears only in backend configuration binding.
- No real key value appears.
- Frontend source has no `Bearer` AI provider call.

- [ ] **Step 9: Conditional final commit**

Run:

```bash
cd /Users/distancewk/dev/Campus\ Forum
git rev-parse --git-dir >/dev/null 2>&1 && git status --short && git add campus-forum-backend campus-forum-frontend-new docs/superpowers && git commit -m "feat: add campus ai qna and moderation" || true
```

Expected in current workspace: no git commit because there is no `.git` directory.

---

## Spec Coverage Review

- AI Q&A for logged-in users: Task 4 backend API, Task 7 frontend route and page.
- Data sources from posts, featured comments, and admin documents: Task 3 ingestion, Task 6 document admin, Task 5 featured comments.
- `pgvector` hybrid retrieval: Task 1 schema, Task 4 retrieval SQL.
- OpenAI-compatible provider configuration: Task 1 config, Task 2 provider.
- Refusal when sources are insufficient: Task 4 service tests and implementation.
- AI moderation for posts and comments: Task 5.
- Low-risk direct publish, medium/high-risk pending review: Task 5.
- Admin human review, no automatic user bans: Task 6 backend, Task 8 frontend.
- Upload formats PDF, DOCX, TXT, Markdown: Task 3 extraction, Task 8 upload UI.
- Security/privacy constraints: Task 4 prompt constraints, Task 9 secret scan.
- Verification: Task 9 full backend, frontend, migration, browser, and provider checks.
