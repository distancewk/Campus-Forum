# 私信模块 (Message) — 任务清单

> 前置依赖：common、user 模块完成

---

## 任务 1：实体与 DTO 定义

- [ ] 1.1 创建 `Message` 实体类（id/senderId/receiverId/content/isRead/createdAt/deletedBySender/deletedByReceiver）
- [ ] 1.2 创建 `WsMessage` DTO（@NotNull receiverId，@NotBlank @Size max 1000 content）
- [ ] 1.3 创建 `MessageVO`（id/senderId/senderNickname/senderAvatar/receiverId/receiverNickname/content/isRead/createdAt）
- [ ] 1.4 创建 `ConversationVO`（userId/nickname/avatar/lastMessage/lastTime/unreadCount）

---

## 任务 2：WebSocket 配置

- [ ] 2.1 创建 `WebSocketConfig`（@EnableWebSocketMessageBroker）
- [ ] 2.2 配置消息代理（enableSimpleBroker "/queue","/topic"）
- [ ] 2.3 配置应用目标前缀（"/app"）
- [ ] 2.4 配置用户目标前缀（"/user"）
- [ ] 2.5 注册 STOMP 端点（"/ws"，setAllowedOriginPatterns("*")，withSockJS）
- [ ] 2.6 创建 WebSocket 认证拦截器（ChannelInterceptor）：
  - [ ] CONNECT 帧从 header 提取 JWT Token
  - [ ] 验证 Token，设置 Principal
- [ ] 2.7 Spring Security 配置放行 `/ws` 端点

---

## 任务 3：Mapper 层

- [ ] 3.1 创建 `MessageMapper`（extends BaseMapper<Message>）
- [ ] 3.2 实现 `selectConversations(userId)` → List<ConversationVO>（按最后消息时间分组，含未读数）
- [ ] 3.3 实现 `selectChatHistory(userId, otherUserId, page, size)` → 分页查询双方消息
- [ ] 3.4 实现 `markAsRead(userId, otherUserId)` — 批量标记已读
- [ ] 3.5 实现 `selectUnreadCount(userId)` → int
- [ ] 3.6 编写 Mapper 单元测试

---

## 任务 4：Service 层

- [ ] 4.1 创建 `MessageService.saveMessage(senderId, wsMessage)` — 插入消息记录，返回 MessageVO
- [ ] 4.2 创建 `MessageService.listConversations(userId)` — 查询会话列表
- [ ] 4.3 创建 `MessageService.getChatHistory(userId, otherUserId, query)` — 分页查询聊天记录
- [ ] 4.4 创建 `MessageService.markAsRead(userId, otherUserId)` — 标记已读
- [ ] 4.5 创建 `MessageService.getUnreadCount(userId)` — 未读消息数
- [ ] 4.6 编写单元测试：消息保存、会话列表排序、已读标记

---

## 任务 5：WebSocket Controller

- [ ] 5.1 创建 `WebSocketController`（@Controller）
- [ ] 5.2 实现 `@MessageMapping("/message/send")` — 接收客户端私信：
  - [ ] 从 Principal 获取发送者 ID
  - [ ] 校验接收者存在
  - [ ] 内容校验（长度、XSS）
  - [ ] 调用 MessageService.saveMessage
  - [ ] 通过 SimpMessagingTemplate 推送给接收者（/user/{receiverId}/queue/messages）
  - [ ] 回显给发送者（确认发送成功）
- [ ] 5.3 编写集成测试：WebSocket 连接、发送消息、接收推送

---

## 任务 6：REST API Controller

- [ ] 6.1 `GET /api/messages/conversations` → listConversations()（需认证）
- [ ] 6.2 `GET /api/messages/conversations/{userId}` → getChatHistory(@PathVariable, @Valid PageQuery)（需认证）
- [ ] 6.3 `GET /api/messages/unread-count` → getUnreadCount()（需认证）
- [ ] 6.4 `PUT /api/messages/conversations/{userId}/read` → markAsRead(@PathVariable)（需认证）
- [ ] 6.5 编写集成测试：
  - [ ] 获取会话列表
  - [ ] 获取聊天记录（分页）
  - [ ] 获取未读消息数
  - [ ] 标记已读后未读数归零

---

## 依赖关系

- 任务 1 → 任务 2（配置）与任务 3（Mapper）可并行
- 任务 4 依赖任务 3
- 任务 5 依赖任务 2、4
- 任务 6 依赖任务 4
