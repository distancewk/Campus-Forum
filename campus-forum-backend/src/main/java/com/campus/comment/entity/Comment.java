package com.campus.comment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 评论内容（纯文本） */
    private String content;

    /** 作者 ID */
    private Long authorId;

    /** 所属帖子 ID */
    private Long postId;

    /** 父评论 ID（NULL = 顶层评论） */
    private Long parentId;

    /** 回复目标用户 ID（楼中楼场景） */
    private Long replyToUserId;

    /** 点赞数 */
    private Integer likeCount;

    private Boolean isFeatured;

    /** 1=正常 -1=已删除 */
    private Integer status;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
