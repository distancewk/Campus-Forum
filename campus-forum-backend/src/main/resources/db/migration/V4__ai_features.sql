CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE comment
    ADD COLUMN IF NOT EXISTS is_featured BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_comment_status_deleted
    ON comment(status, deleted, created_at DESC);

CREATE TABLE ai_knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT,
    file_url VARCHAR(500),
    file_type VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'INDEXING',
    created_by BIGINT,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT,
    chunk_index INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(1536),
    token_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_chunk_document FOREIGN KEY (document_id) REFERENCES ai_knowledge_document(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_chunk_doc_index
    ON ai_knowledge_chunk(document_id, chunk_index);

CREATE INDEX IF NOT EXISTS idx_ai_chunk_source
    ON ai_knowledge_chunk(source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_ai_chunk_content_trgm
    ON ai_knowledge_chunk USING gin (content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_ai_chunk_embedding_hnsw
    ON ai_knowledge_chunk USING hnsw (embedding vector_cosine_ops);

CREATE TABLE ai_qa_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    answer_status VARCHAR(30) NOT NULL,
    latency_ms INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_qa_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_ai_qa_user_created
    ON ai_qa_session(user_id, created_at DESC);

CREATE TABLE ai_qa_citation (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    chunk_id BIGINT,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT,
    title VARCHAR(200) NOT NULL,
    snippet TEXT NOT NULL,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_citation_session FOREIGN KEY (session_id) REFERENCES ai_qa_session(id),
    CONSTRAINT fk_ai_citation_chunk FOREIGN KEY (chunk_id) REFERENCES ai_knowledge_chunk(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_citation_session
    ON ai_qa_citation(session_id);

CREATE TABLE ai_moderation_result (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 0,
    reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggested_action VARCHAR(20) NOT NULL,
    model_name VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_moderation_target
    ON ai_moderation_result(target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_ai_moderation_status
    ON ai_moderation_result(status, risk_level, created_at DESC);
