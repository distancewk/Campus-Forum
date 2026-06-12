package com.campus.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageResult;
import com.campus.interaction.dto.ToggleResponse;
import com.campus.interaction.entity.Favorite;
import com.campus.interaction.mapper.FavoriteMapper;
import com.campus.post.dto.PostListVO;
import com.campus.post.entity.Post;
import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final PostMapper postMapper;

    /**
     * 收藏/取消收藏（幂等切换）
     * <p>
     * 1. 查询是否已收藏
     * 2. 已收藏 -> 删除记录，帖子收藏数 -1
     * 3. 未收藏 -> 插入记录，帖子收藏数 +1
     *
     * @param userId 当前用户 ID
     * @param postId 帖子 ID
     * @return ToggleResponse(active, count)
     */
    @Transactional
    public ToggleResponse toggleFavorite(Long userId, Long postId) {
        // 校验帖子存在
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 查询是否已收藏
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getPostId, postId);

        Favorite existing = favoriteMapper.selectOne(wrapper);

        if (existing != null) {
            // 取消收藏
            int deleted = favoriteMapper.deleteById(existing.getId());
            if (deleted > 0) {
                postMapper.updateFavCount(postId, -1);
            }
            int count = getFavCount(postId);
            return new ToggleResponse(false, count);
        } else {
            // 收藏
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setPostId(postId);
            favorite.setCreatedAt(LocalDateTime.now());
            try {
                favoriteMapper.insert(favorite);
                postMapper.updateFavCount(postId, 1);
            } catch (DuplicateKeyException e) {
                // 并发请求已经插入同一条收藏记录，当前请求按已收藏处理，不重复增加计数。
            }
            int count = getFavCount(postId);
            return new ToggleResponse(true, count);
        }
    }

    /**
     * 分页查询用户收藏的帖子
     *
     * @param userId 当前用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页结果
     */
    public PageResult<PostListVO> listMyFavorites(Long userId, int page, int size) {
        long total = favoriteMapper.countFavoritePosts(userId);

        if (total == 0) {
            return new PageResult<>(Collections.emptyList(), 0, page, size);
        }

        int offset = (page - 1) * size;
        List<PostListVO> records = favoriteMapper.selectFavoritePosts(userId, offset, size);

        return new PageResult<>(records, total, page, size);
    }

    private int getFavCount(Long postId) {
        Post post = postMapper.selectById(postId);
        return post == null || post.getFavCount() == null ? 0 : post.getFavCount();
    }
}
