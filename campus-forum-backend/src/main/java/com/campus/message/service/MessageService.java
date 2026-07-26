package com.campus.message.service;

import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageResult;
import com.campus.message.dto.ConversationVO;
import com.campus.message.dto.MessageVO;
import com.campus.message.dto.WsMessage;
import com.campus.message.entity.Message;
import com.campus.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    /**
     * 保存消息（WebSocket 调用）
     */
    @Transactional
    public MessageVO saveMessage(Long senderId, WsMessage wsMessage) {
        // 校验接收者存在
        User receiver = userMapper.selectById(wsMessage.getReceiverId());
        if (receiver == null || receiver.getDeleted() != 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "接收者不存在");
        }

        // 不能给自己发消息
        if (senderId.equals(wsMessage.getReceiverId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能给自己发消息");
        }

        // XSS 清理
        String cleanContent = Jsoup.clean(wsMessage.getContent(), Safelist.none());

        // 创建消息实体
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(wsMessage.getReceiverId());
        message.setContent(cleanContent);
        message.setIsRead(false);
        message.setDeletedBySender(0);
        message.setDeletedByReceiver(0);

        messageMapper.insert(message);

        // 预填收发件人展示字段，避免 convertToVO 再次查询数据库
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            message.setSenderNickname(sender.getNickname());
            message.setSenderAvatar(sender.getAvatar());
        }
        if (receiver != null) {
            message.setReceiverNickname(receiver.getNickname());
        }

        // 返回 VO
        return convertToVO(message);
    }

    /**
     * 获取会话列表
     */
    public List<ConversationVO> listConversations(Long userId) {
        return messageMapper.selectConversations(userId);
    }

    /**
     * 获取聊天记录
     */
    public PageResult<MessageVO> getChatHistory(Long userId, Long otherUserId, int page, int size) {
        int offset = (page - 1) * size;

        List<Message> messages = messageMapper.selectChatHistory(userId, otherUserId, offset, size);
        List<MessageVO> records = messages.stream().map(this::convertToVO).toList();
        long total = messageMapper.countChatHistory(userId, otherUserId);

        return new PageResult<>(records, total, page, size);
    }

    /**
     * 标记已读
     */
    @Transactional
    public void markAsRead(Long userId, Long otherUserId) {
        messageMapper.markAsRead(userId, otherUserId);
    }

    /**
     * 获取未读消息数
     */
    public int getUnreadCount(Long userId) {
        return messageMapper.selectUnreadCount(userId);
    }

    /**
     * 转换为 VO。收发件人展示字段（senderNickname/senderAvatar/receiverNickname）已由
     * selectChatHistory 联表查询回填，或 saveMessage 预填，无需再次查询数据库。
     */
    private MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreatedAt(message.getCreatedAt());

        vo.setSenderNickname(message.getSenderNickname());
        vo.setSenderAvatar(message.getSenderAvatar());
        vo.setReceiverNickname(message.getReceiverNickname());

        return vo;
    }
}
