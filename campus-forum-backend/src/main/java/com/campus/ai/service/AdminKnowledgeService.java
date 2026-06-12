package com.campus.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.dto.AdminAiDocumentVO;
import com.campus.ai.entity.AiKnowledgeDocument;
import com.campus.ai.mapper.AiKnowledgeDocumentMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminKnowledgeService {
    private static final String SOURCE_DOCUMENT = "DOCUMENT";

    private final KnowledgeIngestionService ingestionService;
    private final AiKnowledgeDocumentMapper documentMapper;

    public AdminAiDocumentVO upload(MultipartFile file, String title) {
        Long adminId = SecurityUtil.requireCurrentUserId();
        String filename = file == null || file.getOriginalFilename() == null
                ? "document"
                : file.getOriginalFilename();
        String fileType = resolveFileType(filename);
        AiKnowledgeDocument document = ingestionService.indexUploadedDocument(
                title,
                "/uploads/ai/" + filename,
                fileType,
                file,
                adminId
        );
        return AdminAiDocumentVO.from(document);
    }

    public PageResult<AdminAiDocumentVO> list(PageQuery query) {
        Page<AiKnowledgeDocument> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getSourceType, SOURCE_DOCUMENT)
                .orderByDesc(AiKnowledgeDocument::getCreatedAt);
        IPage<AiKnowledgeDocument> result = documentMapper.selectPage(page, wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(AdminAiDocumentVO::from).toList(),
                result.getTotal(),
                query.getPage(),
                query.getSize()
        );
    }

    @Transactional
    public void delete(Long documentId) {
        AiKnowledgeDocument document = getAdminDocument(documentId);
        ingestionService.deleteDocumentChunks(document.getId());
        documentMapper.deleteById(document.getId());
    }

    public void reindex(Long documentId) {
        AiKnowledgeDocument document = getAdminDocument(documentId);
        ingestionService.reindexExistingDocument(document);
    }

    private AiKnowledgeDocument getAdminDocument(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null || !SOURCE_DOCUMENT.equals(document.getSourceType())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资料不存在");
        }
        return document;
    }

    private String resolveFileType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "txt";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
