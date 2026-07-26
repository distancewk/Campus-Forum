package com.campus.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long senderId;

    private Long receiverId;

    private String content;

    private Boolean isRead;

    private LocalDateTime createdAt;

    private Integer deletedBySender;

    private Integer deletedByReceiver;

    /**
     * 以下为联表查询（selectChatHistory）回填的展示字段，非数据库列
     */
    @TableField(exist = false)
    private String senderNickname;

    @TableField(exist = false)
    private String senderAvatar;

    @TableField(exist = false)
    private String receiverNickname;
}
