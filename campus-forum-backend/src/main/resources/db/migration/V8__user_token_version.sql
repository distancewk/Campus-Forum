-- 会话撤销：token_version 每次登出/改密/封禁自增，使旧 token 立即失效
ALTER TABLE "user" ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_user_token_version ON "user"(id, token_version);
