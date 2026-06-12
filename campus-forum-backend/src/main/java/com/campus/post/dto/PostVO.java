package com.campus.post.dto;

import com.campus.board.dto.BoardVO;
import com.campus.user.dto.AuthorVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostVO {

    private Long id;

    private String title;

    /** 完整 HTML 内容 */
    private String content;

    private AuthorVO author;

    private BoardVO board;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer favCount;

    private Boolean isPinned;

    private Boolean isFeatured;

    private Integer status;

    private Boolean pendingReview;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 当前用户是否已收藏 */
    private Boolean isFavorited;

    /** 当前用户是否为作者 */
    private Boolean isOwner;

    private LocalDateTime createdAt;
}
