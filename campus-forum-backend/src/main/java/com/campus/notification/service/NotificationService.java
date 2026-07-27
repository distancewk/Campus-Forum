package com.campus.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.common.response.PageResult;
import com.campus.notification.dto.NotificationVO;
import com.campus.notification.entity.Notification;
import com.campus.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 互动通知：点赞 / 评论 / 回复@ / 帖子过审·驳回 / 系统通知。
 *
 * <p>notify 写入库后，若目标用户在线则经 STOMP 用户目的地
 * {@code /user/{userId}/queue/notifications} 实时推送未读项，供前端铃铛角标即时更新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 生成并投递一条通知。
     *
     * @param userId     接收者（null 或 == actorId 时静默跳过）
     * @param type       LIKE / COMMENT / REPLY / MENTION / POST_APPROVED / POST_REJECTED / SYSTEM
     * @param sourceType POST / COMMENT / USER
     * @param sourceId   来源对象 ID
     * @param actorId   触发者 ID（null 表示系统）
     * @param title      标题
     * @param content    内容摘要
     * @param targetUrl  前端跳转路径（如 /post/123）
     */
    @Transactional
    public void notify(Long userId, String type, String sourceType, Long sourceId,
                      Long actorId, String title, String content, String targetUrl) {
        if (userId == null || userId.equals(actorId)) {
            return; // 不通知自己
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setSourceType(sourceType);
        n.setSourceId(sourceId);
        n.setActorId(actorId);
        n.setTitle(truncate(title, 200));
        n.setContent(truncate(content, 500));
        n.setTargetUrl(targetUrl);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());

        if (actorId != null) {
            User actor = userMapper.selectById(actorId);
            if (actor != null) {
                n.setActorNickname(actor.getNickname());
                n.setActorAvatar(actor.getAvatar());
            }
        }
        notificationMapper.insert(n);

        // 实时推送（用户离线时 STOMP 投递静默失败，下次拉取列表仍可见）
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notifications",
                    toVO(n)
            );
        } catch (RuntimeException e) {
            log.debug("通知实时推送失败（用户可能离线），userId={}", userId, e);
        }
    }

    /**
     * 分页获取某用户的通知列表（按时间倒序）。
     */
    public PageResult<NotificationVO> list(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Notification> rows = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT " + size + " OFFSET " + offset)
        );
        long total = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId)
        );
        List<NotificationVO> records = rows.stream().map(this::toVO).toList();
        return new PageResult<>(records, total, page, size);
    }

    /**
     * 未读通知数。
     */
    public int unreadCount(Long userId) {
        return Math.toIntExact(notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false)
        ));
    }

    /**
     * 标记单条为已读（仅限本人）。
     */
    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !userId.equals(n.getUserId())) {
            return;
        }
        n.setIsRead(true);
        notificationMapper.updateById(n);
    }

    /**
     * 全部标记为已读。
     */
    @Transactional
    public void markAllRead(Long userId) {
        Notification n = new Notification();
        n.setIsRead(true);
        notificationMapper.update(
                n,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false)
        );
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        vo.setSourceType(n.getSourceType());
        vo.setSourceId(n.getSourceId());
        vo.setActorId(n.getActorId());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setTargetUrl(n.getTargetUrl());
        vo.setIsRead(n.getIsRead());
        vo.setCreatedAt(n.getCreatedAt());
        vo.setActorNickname(n.getActorNickname());
        vo.setActorAvatar(n.getActorAvatar());
        return vo;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
