package com.campus.post.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.service.AiModerationService;
import com.campus.board.entity.Board;
import com.campus.board.mapper.BoardMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageResult;
import com.campus.common.util.FileUtil;
import com.campus.common.util.SecurityUtil;
import com.campus.post.dto.*;
import com.campus.post.entity.Post;
import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    private final BoardMapper boardMapper;
    private final FileUtil fileUtil;
    private final AiModerationService aiModerationService;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    /**
     * 分页查询帖子列表
     */
    public PageResult<PostListVO> listPosts(PostQuery query) {
        Page<PostListVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<PostListVO> result = postMapper.selectPostList(page, query);

        return new PageResult<>(result.getRecords(), result.getTotal(),
                query.getPage(), query.getSize());
    }

    /**
     * 获取帖子详情（浏览量 +1，查询当前用户点赞/收藏状态）
     */
    @Transactional
    public PostVO getPostDetail(Long postId) {
        PostVO vo = postMapper.selectPostDetail(postId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 浏览量 +1（原子操作）
        postMapper.incrementViewCount(postId);

        // 查询当前用户的点赞/收藏/作者状态
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null) {
            vo.setIsLiked(postMapper.countLikeByUserAndPost(currentUserId, postId) > 0);
            vo.setIsFavorited(postMapper.countFavoriteByUserAndPost(currentUserId, postId) > 0);
            vo.setIsOwner(currentUserId.equals(vo.getAuthor().getId()));
        } else {
            vo.setIsLiked(false);
            vo.setIsFavorited(false);
            vo.setIsOwner(false);
        }
        vo.setPendingReview(Integer.valueOf(0).equals(vo.getStatus()));

        return vo;
    }

    /**
     * 发布帖子
     */
    @Transactional
    public PostVO createPost(PostCreateRequest request) {
        // 校验板块是否存在且启用
        Board board = boardMapper.selectById(request.getBoardId());
        if (board == null || board.getStatus() == null || board.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "板块不存在或已禁用");
        }

        // XSS 过滤
        String sanitizedContent = sanitizeHtml(request.getContent());

        Long currentUserId = SecurityUtil.requireCurrentUserId();

        Post post = new Post();
        post.setTitle(request.getTitle() != null ? request.getTitle().trim() : "");
        post.setContent(sanitizedContent);
        post.setAuthorId(currentUserId);
        post.setBoardId(request.getBoardId());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavCount(0);
        post.setHotScore(0.0);
        post.setIsPinned(false);
        post.setIsFeatured(false);
        AiModerationAdvice advice = aiModerationService.review("POST", post.getTitle(), sanitizedContent, currentUserId, null);
        boolean pendingReview = advice == null || !"LOW".equalsIgnoreCase(advice.getRiskLevel());
        post.setStatus(pendingReview ? 0 : 1);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setDeleted(0);
        postMapper.insert(post);
        aiModerationService.bindTargetAndSave(advice, "POST", post.getId(), currentUserId);

        // 构建返回值，避免调用 getPostDetail（会导致 viewCount 从 1 开始）
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setViewCount(0);
        vo.setLikeCount(0);
        vo.setCommentCount(0);
        vo.setFavCount(0);
        vo.setIsLiked(false);
        vo.setIsFavorited(false);
        vo.setIsOwner(true);
        vo.setStatus(post.getStatus());
        vo.setPendingReview(pendingReview);
        vo.setCreatedAt(post.getCreatedAt());
        return vo;
    }

    /**
     * 编辑帖子（仅作者）
     */
    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        Long currentUserId = SecurityUtil.requireCurrentUserId();
        if (!currentUserId.equals(post.getAuthorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己的帖子");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            post.setContent(sanitizeHtml(request.getContent()));
        }
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
    }

    /**
     * 删除帖子（作者或管理员）
     */
    @Transactional
    public void deletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        Long currentUserId = SecurityUtil.requireCurrentUserId();
        // 判断是否为作者或管理员：此处简化为检查作者，管理员由 @PreAuthorize 或上层控制
        if (!currentUserId.equals(post.getAuthorId()) && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此帖子");
        }

        // 逻辑删除（与 AdminService 保持一致）
        post.setDeleted(1);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
    }

    /**
     * 上传帖子图片
     */
    public Map<String, String> uploadImage(MultipartFile file) {
        String url = fileUtil.upload(file, "post/");
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return result;
    }

    /**
     * XSS 过滤：使用 Jsoup.clean()，允许 img 标签
     */
    private String sanitizeHtml(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, Safelist.relaxed()
                .addTags("img")
                .addAttributes("img", "src", "alt", "width", "height")
                .addProtocols("img", "src", "http", "https"));
    }

    /**
     * 生成内容摘要：去除 HTML 标签后取前 100 字
     */
    public static String generateSummary(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "";
        }
        String plainText = HTML_TAG_PATTERN.matcher(htmlContent).replaceAll("");
        plainText = plainText.replaceAll("&[a-zA-Z0-9#]+;", "").trim();
        if (plainText.length() <= 100) {
            return plainText;
        }
        return plainText.substring(0, 100) + "...";
    }
}
