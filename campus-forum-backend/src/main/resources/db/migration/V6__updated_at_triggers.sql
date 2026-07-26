-- ============================================================
-- 统一的 updated_at 触发器：在任何带 updated_at 列的表执行 UPDATE 前自动刷新
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- "user" (V1)
CREATE TRIGGER trg_user_updated_at BEFORE UPDATE ON "user"
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- post (V1)
CREATE TRIGGER trg_post_updated_at BEFORE UPDATE ON post
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ai_knowledge_document (V4)
CREATE TRIGGER trg_ai_knowledge_document_updated_at BEFORE UPDATE ON ai_knowledge_document
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ai_knowledge_chunk (V4)
CREATE TRIGGER trg_ai_knowledge_chunk_updated_at BEFORE UPDATE ON ai_knowledge_chunk
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
