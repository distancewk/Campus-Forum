package com.campus.comment.dto;

import com.campus.user.dto.AuthorVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;

    private String content;

    /** 评论作者 */
    private AuthorVO author;

    /** 父评论 ID（NULL = 顶层评论） */
    private Long parentId;

    /** 回复目标用户（楼中楼场景） */
    private AuthorVO replyToUser;

    private Integer likeCount;

    private Integer status;

    private Boolean pendingReview;

    private Boolean isFeatured;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 当前用户是否为作者 */
    private Boolean isOwner;

    private LocalDateTime createdAt;

    /** 子回复列表（一级） */
    private List<CommentVO> replies;
}
