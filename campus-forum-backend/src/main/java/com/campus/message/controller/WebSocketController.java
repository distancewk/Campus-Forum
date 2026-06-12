package com.campus.message.controller;

import com.campus.message.dto.MessageVO;
import com.campus.message.dto.WsMessage;
import com.campus.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 客户端发送私信
     * SEND /app/message/send
     */
    @MessageMapping("/message/send")
    public void handleMessage(@Valid WsMessage wsMessage, Principal principal) {
        // 从 Principal 获取发送者 ID
        Long senderId = getUserIdFromPrincipal(principal);

        // 保存消息并获取 VO
        MessageVO saved = messageService.saveMessage(senderId, wsMessage);

        // 推送给接收者（/user/{receiverId}/queue/messages）
        messagingTemplate.convertAndSendToUser(
                String.valueOf(wsMessage.getReceiverId()),
                "/queue/messages",
                saved
        );

        // 回显给发送者（确认发送成功）
        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderId),
                "/queue/messages",
                saved
        );
    }

    /**
     * 从 Principal 中提取用户 ID
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object principalObj = token.getPrincipal();
            if (principalObj instanceof Long userId) {
                return userId;
            }
        }
        throw new RuntimeException("无法获取用户信息");
    }
}
