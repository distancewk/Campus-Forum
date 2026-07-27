package com.campus.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户 */
    private Long userId;

    /** LIKE / COMMENT / REPLY / MENTION / POST_APPROVED / POST_REJECTED / SYSTEM */
    private String type;

    /** POST / COMMENT / USER */
    private String sourceType;

    private Long sourceId;

    /** 触发者（谁点的赞 / 谁评论的） */
    private Long actorId;

    private String title;

    private String content;

    /** 前端跳转路径，如 /post/123 */
    private String targetUrl;

    private Boolean isRead;

    private LocalDateTime createdAt;

    /**
     * 触发者展示字段（notify 时由调用方预填，避免前端额外查询）。非数据库列。
     */
    @TableField(exist = false)
    private String actorNickname;

    @TableField(exist = false)
    private String actorAvatar;
}
