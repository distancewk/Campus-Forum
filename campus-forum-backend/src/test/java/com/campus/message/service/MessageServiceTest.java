package com.campus.message.service;

import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.common.response.PageResult;
import com.campus.message.dto.MessageVO;
import com.campus.message.dto.WsMessage;
import com.campus.message.entity.Message;
import com.campus.message.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private UserMapper userMapper;

    @Test
    void saveMessageStoresXssSafeMessageAndReturnsVo() {
        Long senderId = 1L;
        WsMessage wsMessage = new WsMessage();
        wsMessage.setReceiverId(2L);
        wsMessage.setContent("<script>alert(1)</script>你好");

        User receiver = new User();
        receiver.setId(2L);
        receiver.setDeleted(0);
        receiver.setNickname("接收者");
        User sender = new User();
        sender.setId(1L);
        sender.setNickname("发送者");

        when(userMapper.selectById(2L)).thenReturn(receiver);
        when(userMapper.selectById(1L)).thenReturn(sender);

        MessageService service = new MessageService(messageMapper, userMapper);
        MessageVO vo = service.saveMessage(senderId, wsMessage);

        ArgumentCaptor<com.campus.message.entity.Message> captor =
                ArgumentCaptor.forClass(com.campus.message.entity.Message.class);
        verify(messageMapper).insert(captor.capture());

        // XSS 被清理
        assertThat(captor.getValue().getContent()).doesNotContain("<script>");
        assertThat(captor.getValue().getContent()).contains("你好");
        assertThat(vo.getReceiverNickname()).isEqualTo("接收者");
    }

    @Test
    void saveMessageRejectsSelfMessaging() {
        WsMessage wsMessage = new WsMessage();
        wsMessage.setReceiverId(1L);
        wsMessage.setContent("hi");

        User receiver = new User();
        receiver.setId(1L);
        receiver.setDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(receiver);

        MessageService service = new MessageService(messageMapper, userMapper);
        assertThatThrownBy(() -> service.saveMessage(1L, wsMessage))
                .hasMessageContaining("不能给自己发消息");
    }

    @Test
    void getChatHistoryReturnsPagedResult() {
        when(messageMapper.selectChatHistory(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(new Message()));
        when(messageMapper.countChatHistory(anyLong(), anyLong())).thenReturn(1L);

        MessageService service = new MessageService(messageMapper, userMapper);
        PageResult<MessageVO> result = service.getChatHistory(1L, 2L, 1, 20);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
    }
}
