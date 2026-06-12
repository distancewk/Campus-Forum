# Campus AI Features Design

## Goal

为校园论坛增加一组可落地的 AI 能力：

- 登录用户可以基于论坛历史内容、精华评论和管理员上传资料进行校园智能问答。
- 回答必须基于可引用来源生成，不能在无可靠来源时编造校园信息。
- 发帖和评论接入 AI 审核辅助，识别广告、辱骂、诈骗、联系方式引流、敏感信息泄露和恶意灌水。
- AI 审核只给建议，不直接封禁用户；最终处置由管理员完成。

第一版采用方案 2：关键词检索 + `pgvector` 向量语义检索 + 大模型整理答案的混合检索 RAG。

## Confirmed Decisions

- AI 问答仅登录用户可用。
- 第一版数据源包括历史帖子、精华评论、管理员上传资料。
- 校园通知预留数据源类型，第一版不接外部校园官网。
- 管理员上传资料支持 `PDF`、`DOCX`、`TXT`、`Markdown`。
- 大模型接口使用 OpenAI-compatible 配置，避免绑定单一供应商。
- PostgreSQL 允许启用 `pgvector` 扩展。
- 问答没有可靠引用来源时拒绝编造，只返回相关搜索建议或提问建议。
- 审核流程采用“低风险直接发布，中高风险进入人工待审”。

## Non-Goals

- 第一版不做 OCR，不处理扫描版 PDF 或图片里的文字。
- 第一版不支持 Excel、PPT、音视频资料入库。
- 第一版不自动爬取学校官网、教务系统或企业微信通知。
- 第一版不做自动封禁、自动禁言、自动扣信誉分。
- 第一版不训练自有大模型，只调用可配置的大模型和 embedding 服务。
- 第一版不做实时流式回答；先使用普通请求 + 加载状态，后续可升级 SSE 或 WebSocket 流式输出。

## User Experience

### Campus AI Q&A

新增“AI 问答”入口，登录用户进入后可以直接提问，例如：

- 学校附近哪里可以打印论文？
- 计算机学院研究生选课有什么经验？
- 宿舍报修后一般多久有人处理？
- 有没有往年 Java 校招面经？

页面包含：

- 提问输入框。
- 回答区域。
- 引用来源列表。
- 相关帖子建议。
- 历史问答记录入口。

回答展示规则：

- 每段核心结论尽量对应至少一个引用来源。
- 引用可以是帖子、精华评论或资料片段。
- 引用展示标题、来源类型、摘要片段、发布时间或上传时间。
- 帖子和评论引用可跳转到原帖子；资料引用可跳转到资料详情或下载页。
- 来源不足时，回答固定为“暂未找到足够可靠的校园论坛资料”，并展示相关搜索建议。

### AI Moderation Assistant

用户发帖或评论后，后端先执行审核辅助流程：

- 低风险：直接发布。
- 中风险：进入人工待审，不公开展示。
- 高风险：进入人工待审，不公开展示，并在后台突出显示。

管理员后台内容审核页升级为：

- 待审帖子列表。
- 待审评论列表。
- AI 风险类型标签。
- 风险等级和置信度。
- 命中原因和疑似片段。
- 管理员操作：通过、拒绝、删除。

模型不能直接执行封禁、禁言或账号限制。后台可以展示“建议关注该用户”，但所有账号处置必须由管理员手动确认。

## Architecture

新增 `ai` 后端模块，包含以下边界清晰的服务：

- `AiProviderClient`：封装 OpenAI-compatible chat 和 embedding 调用。
- `KnowledgeIngestionService`：负责帖子、评论、资料的文本抽取、清洗、分段和入库。
- `KnowledgeRetrievalService`：负责关键词检索、向量检索、混合排序和来源过滤。
- `AiQuestionAnswerService`：负责问题接收、召回、提示词构造、回答生成和引用绑定。
- `AiModerationService`：负责内容风险识别、等级判定、审核建议记录。
- `AdminKnowledgeService`：负责管理员资料上传、列表、删除、重建索引。

前端新增：

- `src/views/ai/AiAsk.vue`：用户 AI 问答页。
- `src/api/ai.js`：问答接口。
- `src/api/adminAi.js`：资料管理和审核建议接口。
- 后台增加 AI 资料管理页。
- 后台内容审核页增加 AI 建议、评论审核和风险筛选。

## Data Model

### Knowledge Source

新增知识资料表：

- `ai_knowledge_document`
  - `id`
  - `title`
  - `source_type`: `POST`, `COMMENT`, `DOCUMENT`, `NOTICE_RESERVED`
  - `source_id`
  - `file_url`
  - `file_type`
  - `status`: `ACTIVE`, `INDEXING`, `FAILED`, `DELETED`
  - `created_by`
  - `created_at`
  - `updated_at`

新增知识片段表：

- `ai_knowledge_chunk`
  - `id`
  - `document_id`
  - `source_type`
  - `source_id`
  - `chunk_index`
  - `title`
  - `content`
  - `content_hash`
  - `embedding vector`
  - `token_count`
  - `created_at`
  - `updated_at`

索引要求：

- PostgreSQL 启用 `vector` 扩展。
- `ai_knowledge_chunk.embedding` 建立向量检索索引。
- `title`、`content` 保留关键词检索索引。
- `source_type + source_id` 建立唯一或普通索引，便于重建来源索引。

### Featured Comments

现有帖子已有 `is_featured`。评论第一版需要补充精华能力：

- `comment.is_featured boolean default false`
- 管理员可标记或取消精华评论。
- 只有已发布、未删除、被标记为精华的评论进入问答知识库。

### Q&A Records

新增问答记录表：

- `ai_qa_session`
  - `id`
  - `user_id`
  - `question`
  - `answer`
  - `answer_status`: `ANSWERED`, `INSUFFICIENT_SOURCES`, `FAILED`
  - `latency_ms`
  - `created_at`

新增引用记录表：

- `ai_qa_citation`
  - `id`
  - `session_id`
  - `chunk_id`
  - `source_type`
  - `source_id`
  - `title`
  - `snippet`
  - `score`
  - `created_at`

### Moderation Records

新增审核建议表：

- `ai_moderation_result`
  - `id`
  - `target_type`: `POST`, `COMMENT`
  - `target_id`
  - `author_id`
  - `risk_level`: `LOW`, `MEDIUM`, `HIGH`
  - `risk_types`: JSON array
  - `confidence`
  - `reasons`: JSON array
  - `suggested_action`: `ALLOW`, `REVIEW`, `REJECT`
  - `model_name`
  - `status`: `PENDING_ADMIN`, `ADMIN_APPROVED`, `ADMIN_REJECTED`, `ADMIN_DELETED`, `AUTO_ALLOWED`
  - `created_at`
  - `reviewed_by`
  - `reviewed_at`

现有 `post.status` 可继续表达待审状态：

- `1`: 已发布
- `0`: 待审核
- `-1`: 已拒绝

现有 `comment.status` 也按同样语义使用；待审评论不计入帖子评论数，不在前台评论列表展示。

## Retrieval Flow

1. 用户提交问题。
2. 后端校验登录态和用户级限流。
3. 对问题生成 embedding。
4. 同时执行关键词检索和向量检索。
5. 过滤掉删除、待审、拒绝、不可见来源。
6. 合并排序，优先保留高相关、较新、精华或管理员资料来源。
7. 如果最高分和有效来源数量低于阈值，返回 `INSUFFICIENT_SOURCES`。
8. 如果来源足够，将来源片段和问题交给大模型生成回答。
9. 保存问答记录和引用记录。
10. 前端展示回答和引用。

默认召回策略：

- 向量召回 top 12。
- 关键词召回 top 12。
- 合并后送入模型的来源不超过 6 条。
- 每个来源片段控制长度，避免超出模型上下文。
- 具体阈值通过配置项管理，便于调优。

## Moderation Flow

### Post Create

1. 用户提交帖子。
2. 后端完成现有参数校验和 HTML 清洗。
3. 调用 AI 审核辅助。
4. 风险为 `LOW`：保存为 `status=1`，记录 `AUTO_ALLOWED`。
5. 风险为 `MEDIUM` 或 `HIGH`：保存为 `status=0`，记录 `PENDING_ADMIN`。
6. 前端提示用户：
   - 直接发布：发布成功。
   - 进入审核：内容已提交，等待管理员审核。

### Comment Create

1. 用户提交评论。
2. 后端完成帖子存在性、父评论关系和内容长度校验。
3. 调用 AI 审核辅助。
4. 风险为 `LOW`：保存为 `status=1`，帖子评论数加 1。
5. 风险为 `MEDIUM` 或 `HIGH`：保存为 `status=0`，不增加前台可见评论数。
6. 管理员通过评论后，再增加帖子评论数。

### AI Unavailable

AI 服务不可用时，发帖和评论不因 AI 调用失败整体报错。

系统执行本地兜底规则：

- 明显链接刷屏、联系方式堆叠、重复内容、超频提交进入待审。
- 未命中本地规则的内容允许发布。
- 记录 `AI_UNAVAILABLE` 事件，方便管理员排查。

该策略后续可通过配置切换为更严格的“AI 不可用时全部进入待审”。

## Risk Taxonomy

审核辅助第一版识别以下风险：

- `ADVERTISEMENT`: 广告推广、兼职刷单、营销引流。
- `ABUSE`: 辱骂、人身攻击、歧视性表达。
- `SCAM`: 诈骗、钓鱼链接、虚假交易。
- `CONTACT_DIVERSION`: 微信、QQ、手机号、群号等联系方式引流。
- `SENSITIVE_INFO`: 身份证、学号、手机号、住址、宿舍号等敏感信息泄露。
- `FLOODING`: 重复发帖、无意义刷屏、恶意灌水。

审核结果需要包含：

- 风险类型。
- 风险等级。
- 置信度。
- 命中原因。
- 疑似片段。
- 建议动作。

## Configuration

新增配置项建议：

- `campus.ai.enabled`
- `campus.ai.base-url`
- `campus.ai.api-key`
- `campus.ai.chat-model`
- `campus.ai.embedding-model`
- `campus.ai.embedding-dimension`
- `campus.ai.qa.max-sources`
- `campus.ai.qa.min-score`
- `campus.ai.qa.rate-limit-per-hour`
- `campus.ai.moderation.enabled`
- `campus.ai.moderation.medium-threshold`
- `campus.ai.moderation.high-threshold`
- `campus.ai.document.max-file-size`

敏感配置通过环境变量注入，不写入仓库。

## API Surface

用户接口：

- `POST /api/ai/ask`
  - 请求：`question`
  - 响应：`answerStatus`, `answer`, `citations`, `relatedPosts`
- `GET /api/ai/sessions`
  - 查询当前用户历史问答。
- `GET /api/ai/sessions/{id}`
  - 查询一次问答详情和引用。

管理员接口：

- `POST /api/admin/ai/documents`
  - 上传资料并触发入库。
- `GET /api/admin/ai/documents`
  - 查询资料列表和索引状态。
- `DELETE /api/admin/ai/documents/{id}`
  - 删除资料并下线对应知识片段。
- `POST /api/admin/ai/documents/{id}/reindex`
  - 重新抽取和向量化。
- `GET /api/admin/ai/moderation`
  - 查询 AI 审核建议，支持风险等级、类型、目标类型筛选。
- `PUT /api/admin/posts/{id}/audit`
  - 复用现有帖子审核接口，但补充审核建议状态联动。
- `PUT /api/admin/comments/{id}/audit`
  - 新增评论审核接口。
- `PUT /api/admin/comments/{id}/feature`
  - 新增评论精华切换接口。

## Security and Privacy

- 问答检索只使用已发布、未删除、允许公开的内容。
- 待审、拒绝、删除内容不能进入问答知识库。
- 管理员上传资料默认作为论坛内部知识库使用，仅登录用户可通过问答间接查询。
- 问答提示词中明确要求模型只基于提供来源回答。
- 检索片段视为不可信内容，不能让片段中的指令覆盖系统提示词。
- 问答日志保存问题、答案、引用和耗时，不保存模型完整调试提示词。
- 审核结果中的疑似敏感信息在后台展示时可做局部脱敏。
- AI API key 只允许后端持有，前端不暴露。

## Error Handling

- 问答模型调用超时：返回“AI 服务暂时不可用，请稍后再试”，记录 `FAILED`。
- embedding 生成失败：资料或内容保持未索引状态，后台展示失败原因。
- 上传资料抽取失败：资料状态为 `FAILED`，允许管理员删除或重建索引。
- 检索无结果：返回 `INSUFFICIENT_SOURCES`，不调用或不采纳生成回答。
- 审核模型调用失败：执行本地兜底规则并记录事件。

## Testing

后端测试：

- AI provider 使用 fake client 覆盖正常回答、超时、异常、空结果。
- 知识分段和文本清洗测试。
- 混合检索排序和低分拒答测试。
- 问答接口登录态和限流测试。
- 发帖审核低风险直接发布测试。
- 发帖审核中高风险待审测试。
- 评论待审不增加评论数测试。
- 管理员通过待审评论后评论数增加测试。

前端测试与验证：

- `npm run build` 通过。
- AI 问答页未登录跳转登录。
- 有引用答案、无来源拒答、模型失败三种状态正常展示。
- 后台审核页能看到 AI 风险标签和原因。
- 后台资料管理页能展示上传、失败、索引中、已完成状态。

## Acceptance Criteria

- 登录用户可以提交校园问题并得到基于引用来源的回答。
- 回答中的每条引用能跳转或定位到对应帖子、评论或资料。
- 无可靠来源时系统不会编造答案。
- 管理员可以上传 `PDF`、`DOCX`、`TXT`、`Markdown` 资料并看到索引状态。
- 发帖和评论会经过 AI 审核辅助。
- 低风险内容可直接发布。
- 中高风险内容进入人工待审，前台不可见。
- 管理员能查看 AI 审核建议，并手动通过或拒绝。
- AI 不会直接封禁、禁言或限制用户账号。
- 大模型供应商可通过配置切换。
- 后端测试和前端构建通过。
