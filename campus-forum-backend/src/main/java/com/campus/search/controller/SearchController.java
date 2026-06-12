package com.campus.search.controller;

import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import com.campus.post.dto.PostListVO;
import com.campus.search.dto.SearchQuery;
import com.campus.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 搜索帖子（公开接口）
     * GET /api/search?keyword=xxx&boardId=1&page=1&size=20
     */
    @GetMapping
    public R<PageResult<PostListVO>> search(@Valid SearchQuery query) {
        return R.ok(searchService.search(query));
    }
}
