package com.campus.post.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostListVO {

    private Long id;

    private String title;

    /** 内容摘要（前100字纯文本） */
    private String summary;

    private String authorNickname;

    private String authorAvatar;

    private Long boardId;

    private String boardName;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Boolean isPinned;

    private Boolean isFeatured;

    private LocalDateTime createdAt;
}
