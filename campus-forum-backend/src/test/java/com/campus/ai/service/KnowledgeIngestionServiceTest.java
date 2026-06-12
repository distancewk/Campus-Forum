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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionServiceTest {

    @Mock
    private AiKnowledgeDocumentMapper documentMapper;

    @Mock
    private AiKnowledgeChunkMapper chunkMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private AiProviderClient aiProviderClient;

    @Mock
    private DocumentTextExtractor documentTextExtractor;

    private KnowledgeIngestionService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeIngestionService(
                documentMapper,
                chunkMapper,
                postMapper,
                commentMapper,
                aiProviderClient,
                new TextChunker(),
                documentTextExtractor
        );
    }

    @Test
    void documentIndexingMethodsDoNotWrapProviderCallsInTransaction() throws Exception {
        Method upload = KnowledgeIngestionService.class.getMethod(
                "indexUploadedDocument", String.class, String.class, String.class, org.springframework.web.multipart.MultipartFile.class, Long.class);
        Method reindex = KnowledgeIngestionService.class.getMethod("reindexExistingDocument", AiKnowledgeDocument.class);

        assertThat(upload.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(reindex.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    void uploadedDocumentCreatesActiveDocumentAndVectorChunks() {
        stubDocumentInsertId();
        MockMultipartFile file = new MockMultipartFile("file", "print.txt", "text/plain", "ignored".getBytes());
        when(documentTextExtractor.extract(file)).thenReturn("东门打印店营业到晚上。");
        when(aiProviderClient.createEmbedding(anyString())).thenReturn(List.of(0.1, 0.2));

        AiKnowledgeDocument document = service.indexUploadedDocument("打印指南", "/uploads/ai/print.txt", "txt", file, 7L);

        assertThat(document.getStatus()).isEqualTo("ACTIVE");
        ArgumentCaptor<AiKnowledgeChunk> chunkCaptor = ArgumentCaptor.forClass(AiKnowledgeChunk.class);
        verify(chunkMapper).insertChunk(chunkCaptor.capture());
        AiKnowledgeChunk chunk = chunkCaptor.getValue();
        assertThat(chunk.getDocumentId()).isEqualTo(99L);
        assertThat(chunk.getSourceType()).isEqualTo("DOCUMENT");
        assertThat(chunk.getContent()).isEqualTo("东门打印店营业到晚上。");
        assertThat(chunk.getEmbedding()).isEqualTo("[0.10000000,0.20000000]");
        assertThat(chunk.getContentHash()).hasSize(64);
        verify(documentMapper).updateById(any(AiKnowledgeDocument.class));
    }

    @Test
    void uploadedDocumentFailureMarksDocumentFailedAndDeletesChunks() {
        stubDocumentInsertId();
        MockMultipartFile file = new MockMultipartFile("file", "bad.pdf", "application/pdf", "bad".getBytes());
        when(documentTextExtractor.extract(file)).thenThrow(new IllegalArgumentException("资料文本抽取失败"));

        AiKnowledgeDocument document = service.indexUploadedDocument("坏资料", "/uploads/ai/bad.pdf", "pdf", file, 7L);

        assertThat(document.getStatus()).isEqualTo("FAILED");
        assertThat(document.getErrorMessage()).contains("资料文本抽取失败");
        verify(chunkMapper).deleteByDocumentId(99L);
        verify(chunkMapper, never()).insertChunk(any(AiKnowledgeChunk.class));
        verify(documentMapper).updateById(any(AiKnowledgeDocument.class));
    }

    @Test
    void uploadedDocumentWithoutExtractedTextIsMarkedFailed() {
        stubDocumentInsertId();
        MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", "scan".getBytes());
        when(documentTextExtractor.extract(file)).thenReturn("   ");

        AiKnowledgeDocument document = service.indexUploadedDocument("扫描件", "/uploads/ai/scan.pdf", "pdf", file, 7L);

        assertThat(document.getStatus()).isEqualTo("FAILED");
        assertThat(document.getErrorMessage()).contains("未提取到可索引文本");
        verify(chunkMapper).deleteByDocumentId(99L);
        verify(chunkMapper, never()).insertChunk(any(AiKnowledgeChunk.class));
    }

    @Test
    void publishedPostIndexUsesPublishedOnlyMapper() {
        stubDocumentInsertId();
        PostVO post = new PostVO();
        post.setTitle("报修经验");
        post.setContent("<p>宿舍报修通常一到两天处理。</p>");
        when(postMapper.selectPublishedPostDetail(5L)).thenReturn(post);
        when(aiProviderClient.createEmbedding(anyString())).thenReturn(List.of(0.3, 0.4));

        service.indexPublishedPost(5L);

        verify(postMapper).selectPublishedPostDetail(5L);
        ArgumentCaptor<AiKnowledgeChunk> chunkCaptor = ArgumentCaptor.forClass(AiKnowledgeChunk.class);
        verify(chunkMapper).insertChunk(chunkCaptor.capture());
        assertThat(chunkCaptor.getValue().getSourceType()).isEqualTo("POST");
        assertThat(chunkCaptor.getValue().getSourceId()).isEqualTo(5L);
        assertThat(chunkCaptor.getValue().getContent()).contains("报修经验", "宿舍报修通常一到两天处理。");
    }

    @Test
    void missingPublishedPostDoesNotCreateDocument() {
        when(postMapper.selectPublishedPostDetail(404L)).thenReturn(null);

        service.indexPublishedPost(404L);

        verify(documentMapper, never()).insert(any(AiKnowledgeDocument.class));
        verify(chunkMapper, never()).insertChunk(any(AiKnowledgeChunk.class));
    }

    @Test
    void featuredCommentIndexCreatesCommentSourceChunks() {
        stubDocumentInsertId();
        Comment comment = new Comment();
        comment.setId(8L);
        comment.setContent("Java 校招面试要多刷集合和并发。");
        when(commentMapper.selectPublishedFeaturedComment(8L)).thenReturn(comment);
        when(aiProviderClient.createEmbedding(anyString())).thenReturn(List.of(0.5, 0.6));

        service.indexFeaturedComment(8L);

        verify(commentMapper).selectPublishedFeaturedComment(8L);
        ArgumentCaptor<AiKnowledgeChunk> chunkCaptor = ArgumentCaptor.forClass(AiKnowledgeChunk.class);
        verify(chunkMapper).insertChunk(chunkCaptor.capture());
        assertThat(chunkCaptor.getValue().getTitle()).isEqualTo("精华评论 8");
        assertThat(chunkCaptor.getValue().getSourceType()).isEqualTo("COMMENT");
        assertThat(chunkCaptor.getValue().getSourceId()).isEqualTo(8L);
        assertThat(chunkCaptor.getValue().getContent()).contains("Java 校招面试");
    }

    private void stubDocumentInsertId() {
        when(documentMapper.insert(any(AiKnowledgeDocument.class))).thenAnswer(invocation -> {
            AiKnowledgeDocument document = invocation.getArgument(0);
            document.setId(99L);
            return 1;
        });
    }
}
