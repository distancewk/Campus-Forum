package com.campus.ai.service;

import com.campus.ai.client.AiProviderClient;
import com.campus.ai.entity.AiKnowledgeChunk;
import com.campus.ai.entity.AiKnowledgeDocument;
import com.campus.ai.mapper.AiKnowledgeChunkMapper;
import com.campus.ai.mapper.AiKnowledgeDocumentMapper;
import com.campus.comment.entity.Comment;
import com.campus.comment.mapper.CommentMapper;
import com.campus.post.dto.PostVO;
import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);
    private static final int CHUNK_MAX_CHARS = 1000;
    private static final int CHUNK_OVERLAP_CHARS = 150;
    private static final String STATUS_INDEXING = "INDEXING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_FAILED = "FAILED";

    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final AiProviderClient aiProviderClient;
    private final TextChunker textChunker;
    private final DocumentTextExtractor documentTextExtractor;

    public void indexPublishedPost(Long postId) {
        PostVO post = postMapper.selectPublishedPostDetail(postId);
        if (post == null) {
            return;
        }

        String title = titleOrFallback(post.getTitle(), "帖子 " + postId);
        String content = title + "\n" + htmlToText(post.getContent());
        AiKnowledgeDocument document = createDocument(title, "POST", postId, null, null, null);
        indexTextIntoDocument(document, content, true);
    }

    public void indexFeaturedComment(Long commentId) {
        Comment comment = commentMapper.selectPublishedFeaturedComment(commentId);
        if (comment == null) {
            return;
        }

        String title = "精华评论 " + commentId;
        String content = title + "\n" + (comment.getContent() == null ? "" : comment.getContent());
        AiKnowledgeDocument document = createDocument(title, "COMMENT", commentId, null, null, null);
        indexTextIntoDocument(document, content, true);
    }

    public AiKnowledgeDocument indexUploadedDocument(String title, String fileUrl, String fileType,
                                                     MultipartFile file, Long adminId) {
        AiKnowledgeDocument document = createDocument(
                titleOrFallback(title, filenameOrFallback(file, "上传资料")),
                "DOCUMENT",
                null,
                fileUrl,
                fileType,
                adminId
        );

        try {
            String content = documentTextExtractor.extract(file);
            indexTextIntoDocument(document, content, false);
        } catch (RuntimeException e) {
            deleteDocumentChunks(document.getId());
            markFailed(document, e);
        }
        return document;
    }

    @Transactional
    public void deleteDocumentChunks(Long documentId) {
        if (documentId != null) {
            chunkMapper.deleteByDocumentId(documentId);
        }
    }

    public void reindexExistingDocument(AiKnowledgeDocument document) {
        if (document == null || document.getId() == null) {
            return;
        }
        String content;
        try {
            content = resolveReindexContent(document);
            document.setStatus(STATUS_INDEXING);
            document.setErrorMessage(null);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
            deleteDocumentChunks(document.getId());
            indexTextIntoDocument(document, content, false);
        } catch (RuntimeException e) {
            deleteDocumentChunks(document.getId());
            markFailed(document, e);
        }
    }

    private AiKnowledgeDocument createDocument(String title, String sourceType, Long sourceId,
                                               String fileUrl, String fileType, Long createdBy) {
        LocalDateTime now = LocalDateTime.now();
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setTitle(truncate(titleOrFallback(title, "未命名资料"), 200));
        document.setSourceType(sourceType);
        document.setSourceId(sourceId);
        document.setFileUrl(fileUrl);
        document.setFileType(fileType);
        document.setStatus(STATUS_INDEXING);
        document.setCreatedBy(createdBy);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        documentMapper.insert(document);
        return document;
    }

    private void indexTextIntoDocument(AiKnowledgeDocument document, String content, boolean rethrowOnFailure) {
        try {
            insertDocumentChunks(document, content);
            markActive(document);
        } catch (RuntimeException e) {
            deleteDocumentChunks(document.getId());
            markFailed(document, e);
            if (rethrowOnFailure) {
                throw e;
            }
        }
    }

    private void insertDocumentChunks(AiKnowledgeDocument document, String text) {
        List<String> chunks = textChunker.chunk(text, CHUNK_MAX_CHARS, CHUNK_OVERLAP_CHARS);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("未提取到可索引文本");
        }
        List<AiKnowledgeChunk> knowledgeChunks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            // 入库前清洗：剥离 Markdown 代码围栏等易用于注入的格式。
            String sanitized = stripInjectionMarkers(content);
            // 疑似提示注入的片段：记录告警并跳过，避免污染检索语料。
            if (isPotentialPromptInjection(sanitized)) {
                log.warn("跳过疑似提示注入的检索片段 documentId={} chunkIndex={} snippet={}",
                        document.getId(), i, truncate(sanitized, 120));
                continue;
            }
            List<Double> embedding = aiProviderClient.createEmbedding(sanitized);
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setSourceType(document.getSourceType());
            chunk.setSourceId(document.getSourceId());
            chunk.setChunkIndex(i);
            chunk.setTitle(document.getTitle());
            chunk.setContent(sanitized);
            chunk.setContentHash(sha256(sanitized));
            chunk.setEmbedding(toVectorLiteral(embedding));
            chunk.setTokenCount(sanitized.length());
            chunk.setCreatedAt(now);
            chunk.setUpdatedAt(now);
            knowledgeChunks.add(chunk);
        }

        for (AiKnowledgeChunk chunk : knowledgeChunks) {
            chunkMapper.insertChunk(chunk);
        }
    }

    private void markActive(AiKnowledgeDocument document) {
        document.setStatus(STATUS_ACTIVE);
        document.setErrorMessage(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    private void markFailed(AiKnowledgeDocument document, RuntimeException e) {
        document.setStatus(STATUS_FAILED);
        document.setErrorMessage(truncate(e.getMessage(), 500));
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    private String resolveReindexContent(AiKnowledgeDocument document) {
        if ("POST".equals(document.getSourceType())) {
            PostVO post = postMapper.selectPublishedPostDetail(document.getSourceId());
            if (post == null) {
                throw new IllegalStateException("帖子不存在或未发布");
            }
            return titleOrFallback(post.getTitle(), "帖子 " + document.getSourceId()) + "\n" + htmlToText(post.getContent());
        }
        if ("COMMENT".equals(document.getSourceType())) {
            Comment comment = commentMapper.selectPublishedFeaturedComment(document.getSourceId());
            if (comment == null) {
                throw new IllegalStateException("精华评论不存在或未发布");
            }
            return "精华评论 " + document.getSourceId() + "\n" + (comment.getContent() == null ? "" : comment.getContent());
        }
        List<AiKnowledgeChunk> chunks = chunkMapper.selectByDocumentId(document.getId());
        if (chunks.isEmpty()) {
            throw new IllegalStateException("资料没有可用于重新索引的文本，请重新上传");
        }
        return chunks.stream()
                .map(AiKnowledgeChunk::getContent)
                .collect(Collectors.joining("\n"));
    }

    private String titleOrFallback(String title, String fallback) {
        if (title == null || title.isBlank()) {
            return fallback;
        }
        return title.trim();
    }

    private String filenameOrFallback(MultipartFile file, String fallback) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return fallback;
        }
        return file.getOriginalFilename();
    }

    private String htmlToText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return Jsoup.parse(content).text();
    }

    /**
     * 剥离 Markdown 代码围栏（``` 或 ~~~）及常见注入分隔标记，仅保留纯文本用于索引/向量化。
     */
    private String stripInjectionMarkers(String content) {
        if (content == null) {
            return null;
        }
        return content
                .replace("```", "")
                .replace("~~~", "")
                .replace("<<<CAMPUS_USER_CONTENT_START>>>", "")
                .replace("<<<CAMPUS_USER_CONTENT_END>>>", "")
                .replace("<<<CAMPUS_USER_QUESTION_START>>>", "")
                .replace("<<<CAMPUS_USER_QUESTION_END>>>", "");
    }

    /**
     * 判断文本片段是否疑似提示注入（试图操纵检索/问答模型）。
     */
    private boolean isPotentialPromptInjection(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String lower = content.toLowerCase();
        return lower.contains("忽略以上指令")
                || lower.contains("忽略上述指令")
                || lower.contains("ignore previous instructions")
                || lower.contains("ignore the above")
                || lower.contains("system prompt")
                || lower.contains("泄露系统提示")
                || lower.contains("泄露系统提示词")
                || lower.contains("reveal your prompt")
                || lower.contains("reveal the system prompt");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String toVectorLiteral(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return "[]";
        }
        return embedding.stream()
                .map(value -> String.format(Locale.US, "%.8f", value == null ? 0.0d : value))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format(Locale.US, "%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
