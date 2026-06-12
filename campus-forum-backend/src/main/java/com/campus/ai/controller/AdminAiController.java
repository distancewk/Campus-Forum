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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
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
