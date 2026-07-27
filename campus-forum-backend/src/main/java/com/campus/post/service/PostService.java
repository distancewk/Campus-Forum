package com.campus.post.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.config.AiProperties;
import com.campus.ai.service.AsyncModerationService;
import com.campus.ai.service.KnowledgeIngestionService;
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
import com.campus.common.util.HtmlSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final AsyncModerationService asyncModerationService;
    private final AiProperties aiProperties;
    private final KnowledgeIngestionService knowledgeIngestionService;

    @Value("${campus.ai.moderation-enabled:true}")
    private boolean moderationEnabled;

    /** AI 审核是否真正可用（与 AsyncModerationService 同源判断）。 */
    private boolean aiActive() {
        return aiProperties.isEnabled() && moderationEnabled;
    }

    /** 审核模式：off / post（默认，事后复核）/ pre（先审后发）。 */
    private String moderationMode() {
        String m = aiProperties.getModeration().getMode();
        return m == null ? "post" : m;
    }

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

        // 可见性控制：未发布（待审核/已拒绝）的帖子仅作者本人与管理员可见，其余一律视为不存在。
        if (vo.getStatus() != null && vo.getStatus() != 1) {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            boolean isAuthor = currentUserId != null
                    && vo.getAuthor() != null
                    && currentUserId.equals(vo.getAuthor().getId());
            if (!isAuthor && !SecurityUtil.isAdmin()) {
                throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
            }
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

        // 后端 HTML 清洗（信任边界）：标题剥离所有标签，正文仅保留安全富文本标签。
        String sanitizedContent = HtmlSanitizer.cleanBasic(request.getContent());

        Long currentUserId = SecurityUtil.requireCurrentUserId();

        Post post = new Post();
        post.setTitle(HtmlSanitizer.cleanPlain(request.getTitle()));
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
        // 发布策略（第一性原理）：默认直接公开可见，HTML 清洗已保底；
        // 仅当 AI 启用且 moderation.mode=pre 时才"先审后发"。AI 作为可插拔增强层，不阻断发布。
        boolean prePublish = aiActive() && "pre".equals(moderationMode());
        post.setStatus(prePublish ? 0 : 1);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setDeleted(0);
        postMapper.insert(post);
        // AI 启用时做后台审核：pre 模式审核通过才公开；post 模式事后复核。
        if (aiActive()) {
            dispatchPostModeration(post.getId(), post.getTitle(), sanitizedContent, currentUserId, prePublish);
        }

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
        vo.setPendingReview(prePublish);
        vo.setCreatedAt(post.getCreatedAt());
        return vo;
    }

    /**
     * 事务提交后再触发异步 AI 审核，避免异步线程在帖子尚未提交时就读不到记录。
     * 若无活跃事务（理论不应发生），则直接触发。
     */
    private void dispatchPostModeration(Long postId, String title, String content, Long authorId, boolean wasPending) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            Long id = postId;
            String t = title;
            String c = content;
            Long uid = authorId;
            boolean pending = wasPending;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    asyncModerationService.moderatePost(id, t, c, uid, pending);
                }
            });
        } else {
            asyncModerationService.moderatePost(postId, title, content, authorId, wasPending);
        }
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

        boolean changed = false;
        if (request.getTitle() != null) {
            String newTitle = HtmlSanitizer.cleanPlain(request.getTitle());
            if (!newTitle.equals(post.getTitle())) {
                post.setTitle(newTitle);
                changed = true;
            }
        }
        if (request.getContent() != null) {
            String newContent = HtmlSanitizer.cleanBasic(request.getContent());
            if (!newContent.equals(post.getContent())) {
                post.setContent(newContent);
                changed = true;
            }
        }
        post.setUpdatedAt(LocalDateTime.now());
        if (changed) {
            // 已发布帖编辑后保持可见（status 保持 1），由后台 AI 复核；
            // 仅当 AI 启用且 mode=pre 时，才重新进入待审，等审核/人工处理。
            // 若 AI 明确判拒，由 AsyncModerationService 下架（post-hoc），不会因编辑抖动而消失。
            boolean prePublish = aiActive() && "pre".equals(moderationMode());
            if (prePublish) {
                post.setStatus(0);
            }
            postMapper.updateById(post);
            if (aiActive()) {
                dispatchPostModeration(post.getId(), post.getTitle(), post.getContent(), currentUserId, prePublish);
            }
        } else {
            postMapper.updateById(post);
        }
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

        // 1. 回收帖子正文中引用的图片文件（幂等：文件不存在则跳过）
        if (post.getContent() != null && !post.getContent().isBlank()) {
            Document doc = Jsoup.parse(post.getContent());
            for (Element img : doc.select("img[src]")) {
                fileUtil.deleteByUrl(img.attr("src"));
            }
        }

        // 2. 逻辑删除（与 AdminService 保持一致）
        post.setDeleted(1);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);

        // 3. 清理该帖在 AI 问答向量库中的索引，避免问答召回已删除内容
        knowledgeIngestionService.removeBySource("POST", postId);
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
