package com.campus.interaction.controller;

import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import com.campus.common.util.SecurityUtil;
import com.campus.interaction.dto.FavoriteRequest;
import com.campus.interaction.dto.LikeRequest;
import com.campus.interaction.dto.ToggleResponse;
import com.campus.interaction.service.FavoriteService;
import com.campus.interaction.service.LikeService;
import com.campus.post.dto.PostListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InteractionController {

    private final LikeService likeService;
    private final FavoriteService favoriteService;

    /**
     * 点赞/取消点赞（幂等）
     * POST /api/likes
     */
    @PostMapping("/likes")
    public R<ToggleResponse> toggleLike(@RequestBody @Valid LikeRequest request) {
        Long userId = SecurityUtil.requireCurrentUserId();
        ToggleResponse response = likeService.toggleLike(userId, request.getTargetType(), request.getTargetId());
        return R.ok(response);
    }

    /**
     * 收藏/取消收藏（幂等）
     * POST /api/favorites
     */
    @PostMapping("/favorites")
    public R<ToggleResponse> toggleFavorite(@RequestBody @Valid FavoriteRequest request) {
        Long userId = SecurityUtil.requireCurrentUserId();
        ToggleResponse response = favoriteService.toggleFavorite(userId, request.getPostId());
        return R.ok(response);
    }

    /**
     * 获取我的收藏列表（分页）
     * GET /api/favorites?page=1&size=20
     */
    @GetMapping("/favorites")
    public R<PageResult<PostListVO>> myFavorites(@Valid PageQuery query) {
        Long userId = SecurityUtil.requireCurrentUserId();
        PageResult<PostListVO> result = favoriteService.listMyFavorites(userId, query.getPage(), query.getSize());
        return R.ok(result);
    }
}
