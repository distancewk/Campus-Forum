package com.campus.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.message.dto.ConversationVO;
import com.campus.message.dto.MessageVO;
import com.campus.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询会话列表（最近联系人），结果由 MessageMapper.xml 定义
     */
    List<ConversationVO> selectConversations(@Param("userId") Long userId);

    /**
     * 查询与某用户的聊天记录（分页），结果由 MessageMapper.xml 定义。
     * 联表带回收发件人展示字段，由 MessageService.convertToVO 映射到 MessageVO，避免 N+1 查询。
     */
    List<Message> selectChatHistory(@Param("userId") Long userId,
                                    @Param("otherUserId") Long otherUserId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 查询聊天记录总数
     */
    long countChatHistory(@Param("userId") Long userId,
                          @Param("otherUserId") Long otherUserId);

    /**
     * 标记消息为已读
     */
    int markAsRead(@Param("userId") Long userId,
                   @Param("otherUserId") Long otherUserId);

    /**
     * 查询未读消息数
     */
    int selectUnreadCount(@Param("userId") Long userId);
}
