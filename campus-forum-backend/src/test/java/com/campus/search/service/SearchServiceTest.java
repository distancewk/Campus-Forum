package com.campus.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.response.PageResult;
import com.campus.post.dto.PostListVO;
import com.campus.post.mapper.PostMapper;
import com.campus.search.dto.SearchQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private PostMapper postMapper;

    @Test
    void searchReturnsPagedResultFromMapper() {
        SearchQuery query = new SearchQuery();
        query.setKeyword("校园");
        query.setPage(1);
        query.setSize(10);

        PostListVO vo = new PostListVO();
        Page<PostListVO> pageResult = new Page<>(1, 10, 1);
        pageResult.setRecords(List.of(vo));
        when(postMapper.searchPosts(any(Page.class), any(SearchQuery.class))).thenReturn(pageResult);

        SearchService service = new SearchService(postMapper);
        PageResult<PostListVO> result = service.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
    }
}
