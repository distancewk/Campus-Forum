package com.campus.ai.controller;

import com.campus.ai.dto.AiAskRequest;
import com.campus.ai.dto.AiAskResponse;
import com.campus.ai.dto.AiSessionVO;
import com.campus.ai.service.AiQuestionAnswerService;
import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import com.campus.common.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/sessions")
    public R<PageResult<AiSessionVO>> listSessions(@Valid PageQuery query) {
        Long userId = SecurityUtil.requireCurrentUserId();
        return R.ok(aiQuestionAnswerService.listSessions(userId, query));
    }

    @GetMapping("/sessions/{id}")
    public R<AiSessionVO> getSession(@PathVariable Long id) {
        Long userId = SecurityUtil.requireCurrentUserId();
        return R.ok(aiQuestionAnswerService.getSession(userId, id));
    }
}
