package com.campus.notification.controller;

import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import com.campus.common.util.SecurityUtil;
import com.campus.notification.dto.NotificationVO;
import com.campus.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取我的通知列表（分页，时间倒序）
     * GET /api/notifications?page=1&size=20
     */
    @GetMapping("/notifications")
    public R<PageResult<NotificationVO>> list(@Valid PageQuery query) {
        Long userId = SecurityUtil.requireCurrentUserId();
        PageResult<NotificationVO> result =
                notificationService.list(userId, query.getPage(), query.getSize());
        return R.ok(result);
    }

    /**
     * 未读通知数
     * GET /api/notifications/unread-count
     */
    @GetMapping("/notifications/unread-count")
    public R<Integer> unreadCount() {
        Long userId = SecurityUtil.requireCurrentUserId();
        return R.ok(notificationService.unreadCount(userId));
    }

    /**
     * 标记单条已读
     * POST /api/notifications/{id}/read
     */
    @PostMapping("/notifications/{id}/read")
    public R<Void> markRead(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.requireCurrentUserId();
        notificationService.markRead(userId, id);
        return R.ok();
    }

    /**
     * 全部标记为已读
     * POST /api/notifications/read-all
     */
    @PostMapping("/notifications/read-all")
    public R<Void> markAllRead() {
        Long userId = SecurityUtil.requireCurrentUserId();
        notificationService.markAllRead(userId);
        return R.ok();
    }
}
