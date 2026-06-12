package com.campus.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.interaction.dto.ToggleResponse;
import com.campus.interaction.entity.Favorite;
import com.campus.interaction.mapper.FavoriteMapper;
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
class FavoriteServiceTest {

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private PostMapper postMapper;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteMapper, postMapper);
    }

    @Test
    void duplicateFavoriteInsertReturnsActiveWithoutIncrementingCountAgain() {
        Post post = new Post();
        post.setId(10L);
        post.setFavCount(3);

        when(postMapper.selectById(10L)).thenReturn(post);
        when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(favoriteMapper.insert(any(Favorite.class))).thenThrow(new DuplicateKeyException("duplicate favorite"));

        ToggleResponse response = favoriteService.toggleFavorite(1L, 10L);

        assertThat(response.getActive()).isTrue();
        assertThat(response.getCount()).isEqualTo(3);
        verify(postMapper, never()).updateFavCount(10L, 1);
    }

    @Test
    void alreadyRemovedFavoriteDoesNotDecrementCountAgain() {
        Favorite existing = new Favorite();
        existing.setId(100L);

        Post post = new Post();
        post.setId(10L);
        post.setFavCount(3);

        when(postMapper.selectById(10L)).thenReturn(post);
        when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(favoriteMapper.deleteById(100L)).thenReturn(0);

        ToggleResponse response = favoriteService.toggleFavorite(1L, 10L);

        assertThat(response.getActive()).isFalse();
        assertThat(response.getCount()).isEqualTo(3);
        verify(postMapper, never()).updateFavCount(10L, -1);
    }
}
