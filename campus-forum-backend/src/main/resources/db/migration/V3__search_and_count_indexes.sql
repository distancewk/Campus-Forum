CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_post_title_trgm
    ON post USING GIN (title gin_trgm_ops)
    WHERE deleted = 0 AND status = 1;

CREATE INDEX IF NOT EXISTS idx_post_content_trgm
    ON post USING GIN (content gin_trgm_ops)
    WHERE deleted = 0 AND status = 1;

CREATE INDEX IF NOT EXISTS idx_post_public_latest
    ON post (created_at DESC)
    WHERE deleted = 0 AND status = 1;

CREATE INDEX IF NOT EXISTS idx_comment_post_parent_created
    ON comment (post_id, parent_id, created_at)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_message_pair_created
    ON message (sender_id, receiver_id, created_at DESC);
