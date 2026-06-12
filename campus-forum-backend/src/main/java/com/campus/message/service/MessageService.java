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

        List<MessageVO> records = messageMapper.selectChatHistory(userId, otherUserId, offset, size);
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
     * 转换为 VO
     */
    private MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreatedAt(message.getCreatedAt());

        // 查询发送者信息
        User sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }

        // 查询接收者信息
        User receiver = userMapper.selectById(message.getReceiverId());
        if (receiver != null) {
            vo.setReceiverNickname(receiver.getNickname());
        }

        return vo;
    }
}
