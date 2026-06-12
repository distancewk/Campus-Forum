package com.campus.message.controller;

import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import com.campus.common.util.SecurityUtil;
import com.campus.message.dto.ConversationVO;
import com.campus.message.dto.MessageVO;
import com.campus.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public R<List<ConversationVO>> listConversations() {
        Long userId = SecurityUtil.requireCurrentUserId();
        return R.ok(messageService.listConversations(userId));
    }

    /**
     * 获取与某用户的聊天记录
     */
    @GetMapping("/conversations/{userId}")
    public R<PageResult<MessageVO>> getChatHistory(@PathVariable Long userId,
                                                    @Valid PageQuery query) {
        Long currentUserId = SecurityUtil.requireCurrentUserId();
        return R.ok(messageService.getChatHistory(currentUserId, userId, query.getPage(), query.getSize()));
    }

    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread-count")
    public R<Map<String, Integer>> getUnreadCount() {
        Long userId = SecurityUtil.requireCurrentUserId();
        int count = messageService.getUnreadCount(userId);
        return R.ok(Map.of("count", count));
    }

    /**
     * 标记某会话已读
     */
    @PutMapping("/conversations/{userId}/read")
    public R<Void> markAsRead(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.requireCurrentUserId();
        messageService.markAsRead(currentUserId, userId);
        return R.ok();
    }
}
