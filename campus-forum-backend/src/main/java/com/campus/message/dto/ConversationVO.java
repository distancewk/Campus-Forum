package com.campus.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    /** 对方用户 ID */
    private Long userId;

    private String nickname;

    private String avatar;

    /** 最后一条消息摘要 */
    private String lastMessage;

    /** 最后消息时间 */
    private LocalDateTime lastTime;

    /** 未读消息数 */
    private Integer unreadCount;
}
