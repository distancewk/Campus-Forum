package com.campus.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.response.PageResult;
import com.campus.post.dto.PostListVO;
import com.campus.post.mapper.PostMapper;
import com.campus.search.dto.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostMapper postMapper;

    /**
     * 搜索帖子（标题或内容匹配关键词，可选板块筛选）
     */
    public PageResult<PostListVO> search(SearchQuery query) {
        Page<PostListVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<PostListVO> result = postMapper.searchPosts(page, query);
        return new PageResult<>(result.getRecords(), result.getTotal(),
                query.getPage(), query.getSize());
    }
}
