package com.campus.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;

    private String type;

    private String sourceType;

    private Long sourceId;

    private Long actorId;

    private String title;

    private String content;

    private String targetUrl;

    private Boolean isRead;

    private LocalDateTime createdAt;

    private String actorNickname;

    private String actorAvatar;
}
