package com.campus.post.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Long authorId;

    private Long boardId;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数（冗余） */
    private Integer likeCount;

    /** 评论数（冗余） */
    private Integer commentCount;

    /** 收藏数（冗余） */
    private Integer favCount;

    /** 热度分（预计算） */
    private Double hotScore;

    /** 是否置顶 */
    private Boolean isPinned;

    /** 是否精华 */
    private Boolean isFeatured;

    /** 1=正常 0=待审核 -1=已删除 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
