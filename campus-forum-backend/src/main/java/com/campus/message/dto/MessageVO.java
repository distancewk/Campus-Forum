package com.campus.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;

    private Long senderId;

    private String senderNickname;

    private String senderAvatar;

    private Long receiverId;

    private String receiverNickname;

    private String content;

    private Boolean isRead;

    private LocalDateTime createdAt;
}
