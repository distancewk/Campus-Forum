package com.campus.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("\"like\"")
public class Like {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 目标类型：POST / COMMENT */
    private String targetType;

    /** 目标 ID（帖子 ID 或评论 ID） */
    private Long targetId;

    private LocalDateTime createdAt;
}
