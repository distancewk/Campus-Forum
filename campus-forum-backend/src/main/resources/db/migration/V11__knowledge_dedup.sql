-- P2：AI 知识库去重与检索质量提升
-- 1) 清理同一来源（source_type + source_id）的历史重复文档，仅保留最新一次索引（id 最大者）。
--    先删孤儿分块，再删文档，规避外键限制（fk_ai_chunk_document 无 ON DELETE 级联）。
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT a.id AS dup_id
        FROM ai_knowledge_document a
        WHERE a.source_id IS NOT NULL
          AND a.id <> (
              SELECT MAX(b.id)
              FROM ai_knowledge_document b
              WHERE b.source_type = a.source_type
                AND b.source_id = a.source_id
          )
    LOOP
        DELETE FROM ai_knowledge_chunk WHERE document_id = r.dup_id;
        DELETE FROM ai_knowledge_document WHERE id = r.dup_id;
    END LOOP;
END $$;

-- 2) 防止未来重复索引：同一来源只保留一个文档。
--    使用部分唯一索引（WHERE source_id IS NOT NULL），上传资料（source_id 为 NULL）不受影响，可多条共存。
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_document_source
    ON ai_knowledge_document(source_type, source_id)
    WHERE source_id IS NOT NULL;
