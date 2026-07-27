-- 互动通知体系：点赞 / 评论 / 回复@ / 帖子过审 / 帖子驳回 / 系统通知
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,        -- 接收通知的用户
    type        VARCHAR(30)   NOT NULL,        -- LIKE / COMMENT / REPLY / MENTION / POST_APPROVED / POST_REJECTED / SYSTEM
    source_type VARCHAR(30),                     -- POST / COMMENT / USER
    source_id   BIGINT,
    actor_id    BIGINT,                           -- 触发者（谁点的赞 / 谁评论的）
    title       VARCHAR(200)  NOT NULL,
    content     VARCHAR(500)  NOT NULL,
    target_url  VARCHAR(500),                      -- 前端跳转路径，如 /post/123
    is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_user_read
    ON notification (user_id, is_read);

CREATE INDEX IF NOT EXISTS idx_notification_user_created
    ON notification (user_id, created_at DESC);
