package com.campus.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.comment.entity.Comment;
import com.campus.comment.mapper.CommentMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.interaction.dto.ToggleResponse;
import com.campus.interaction.entity.Like;
import com.campus.interaction.mapper.LikeMapper;
import com.campus.post.entity.Post;
import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeMapper likeMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    /**
     * 点赞/取消点赞（幂等切换）
     * <p>
     * 1. 查询是否已点赞
     * 2. 已点赞 -> 删除记录，目标计数 -1
     * 3. 未点赞 -> 插入记录，目标计数 +1
     *
     * @param userId     当前用户 ID
     * @param targetType 目标类型：POST / COMMENT
     * @param targetId   目标 ID
     * @return ToggleResponse(active, count)
     */
    @Transactional
    public ToggleResponse toggleLike(Long userId, String targetType, Long targetId) {
        ensureTargetExists(targetType, targetId);

        // 查询是否已点赞
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId);

        Like existing = likeMapper.selectOne(wrapper);

        if (existing != null) {
            // 取消点赞
            int deleted = likeMapper.deleteById(existing.getId());
            if (deleted > 0) {
                updateTargetCount(targetType, targetId, -1);
            }
            int count = getTargetCount(targetType, targetId);
            return new ToggleResponse(false, count);
        } else {
            // 点赞
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            like.setCreatedAt(LocalDateTime.now());
            try {
                likeMapper.insert(like);
                updateTargetCount(targetType, targetId, 1);
            } catch (DuplicateKeyException e) {
                // 并发请求已经插入同一条点赞记录，当前请求按已点赞处理，不重复增加计数。
            }
            int count = getTargetCount(targetType, targetId);
            return new ToggleResponse(true, count);
        }
    }

    /**
     * 更新目标的点赞计数
     */
    private void updateTargetCount(String targetType, Long targetId, int delta) {
        if ("POST".equals(targetType)) {
            postMapper.updateLikeCount(targetId, delta);
        } else if ("COMMENT".equals(targetType)) {
            commentMapper.updateLikeCount(targetId, delta);
        }
    }

    /**
     * 获取目标的当前点赞数
     */
    private int getTargetCount(String targetType, Long targetId) {
        if ("POST".equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            return post == null || post.getLikeCount() == null ? 0 : post.getLikeCount();
        } else if ("COMMENT".equals(targetType)) {
            Comment comment = commentMapper.selectById(targetId);
            return comment == null || comment.getLikeCount() == null ? 0 : comment.getLikeCount();
        }
        return 0;
    }

    private void ensureTargetExists(String targetType, Long targetId) {
        if ("POST".equals(targetType)) {
            if (postMapper.selectById(targetId) == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
            }
            return;
        }
        if ("COMMENT".equals(targetType)) {
            if (commentMapper.selectById(targetId) == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
            }
            return;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "目标类型不正确");
    }
}
