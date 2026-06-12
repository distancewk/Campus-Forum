package com.campus.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.comment.mapper.CommentMapper;
import com.campus.interaction.dto.ToggleResponse;
import com.campus.interaction.entity.Like;
import com.campus.interaction.mapper.LikeMapper;
import com.campus.post.entity.Post;
import com.campus.post.mapper.PostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeMapper likeMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private CommentMapper commentMapper;

    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeService = new LikeService(likeMapper, postMapper, commentMapper);
    }

    @Test
    void duplicateLikeInsertReturnsActiveWithoutIncrementingCountAgain() {
        Post post = new Post();
        post.setId(10L);
        post.setLikeCount(7);

        when(postMapper.selectById(10L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(likeMapper.insert(any(Like.class))).thenThrow(new DuplicateKeyException("duplicate like"));

        ToggleResponse response = likeService.toggleLike(1L, "POST", 10L);

        assertThat(response.getActive()).isTrue();
        assertThat(response.getCount()).isEqualTo(7);
        verify(postMapper, never()).updateLikeCount(10L, 1);
    }

    @Test
    void alreadyRemovedLikeDoesNotDecrementCountAgain() {
        Like existing = new Like();
        existing.setId(100L);

        Post post = new Post();
        post.setId(10L);
        post.setLikeCount(7);

        when(postMapper.selectById(10L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(likeMapper.deleteById(100L)).thenReturn(0);

        ToggleResponse response = likeService.toggleLike(1L, "POST", 10L);

        assertThat(response.getActive()).isFalse();
        assertThat(response.getCount()).isEqualTo(7);
        verify(postMapper, never()).updateLikeCount(10L, -1);
    }
}
