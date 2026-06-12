-- ============================================================
-- User table
-- ============================================================
CREATE TABLE "user" (
    id            BIGSERIAL PRIMARY KEY,
    student_no    VARCHAR(20)  NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    password      VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    avatar        VARCHAR(255) DEFAULT NULL,
    bio           VARCHAR(200) DEFAULT '',
    role          VARCHAR(10)  NOT NULL DEFAULT 'STUDENT',
    status        SMALLINT     NOT NULL DEFAULT 1,
    login_fail    SMALLINT     NOT NULL DEFAULT 0,
    locked_until  TIMESTAMP    DEFAULT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_user_student_no ON "user"(student_no) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_user_email ON "user"(email) WHERE deleted = 0;

-- ============================================================
-- Board table
-- ============================================================
CREATE TABLE board (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200) DEFAULT '',
    icon        VARCHAR(100) DEFAULT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      SMALLINT     NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Post table
-- ============================================================
CREATE TABLE post (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(100) NOT NULL,
    content       TEXT         NOT NULL,
    author_id     BIGINT       NOT NULL,
    board_id      BIGINT       NOT NULL,
    view_count    INT          NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    fav_count     INT          NOT NULL DEFAULT 0,
    hot_score     DOUBLE PRECISION NOT NULL DEFAULT 0,
    is_pinned     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_featured   BOOLEAN      NOT NULL DEFAULT FALSE,
    status        SMALLINT     NOT NULL DEFAULT 1,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES "user"(id),
    CONSTRAINT fk_post_board  FOREIGN KEY (board_id)  REFERENCES board(id)
);

CREATE INDEX idx_post_board_id    ON post(board_id);
CREATE INDEX idx_post_author_id   ON post(author_id);
CREATE INDEX idx_post_created_at  ON post(created_at DESC);
CREATE INDEX idx_post_hot_score   ON post(hot_score DESC);
CREATE INDEX idx_post_board_pinned ON post(board_id, is_pinned DESC, hot_score DESC);

-- ============================================================
-- Comment table
-- ============================================================
CREATE TABLE comment (
    id          BIGSERIAL PRIMARY KEY,
    content     TEXT     NOT NULL,
    author_id   BIGINT   NOT NULL,
    post_id     BIGINT   NOT NULL,
    parent_id   BIGINT   DEFAULT NULL,
    reply_to_user_id BIGINT DEFAULT NULL,
    like_count  INT      NOT NULL DEFAULT 0,
    status      SMALLINT NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted     SMALLINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES "user"(id),
    CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)   REFERENCES post(id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment(id)
);

CREATE INDEX idx_comment_post_id   ON comment(post_id);
CREATE INDEX idx_comment_parent_id ON comment(parent_id);

-- ============================================================
-- Like table (polymorphic: supports post and comment)
-- ============================================================
CREATE TABLE "like" (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    target_id   BIGINT      NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT uk_like_user_target UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_like_target ON "like"(target_type, target_id);

-- ============================================================
-- Favorite table
-- ============================================================
CREATE TABLE favorite (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    post_id    BIGINT   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT fk_fav_post FOREIGN KEY (post_id) REFERENCES post(id),
    CONSTRAINT uk_fav_user_post UNIQUE (user_id, post_id)
);

-- ============================================================
-- Message table
-- ============================================================
CREATE TABLE message (
    id          BIGSERIAL PRIMARY KEY,
    sender_id   BIGINT   NOT NULL,
    receiver_id BIGINT   NOT NULL,
    content     TEXT     NOT NULL,
    is_read     BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_by_sender   SMALLINT NOT NULL DEFAULT 0,
    deleted_by_receiver SMALLINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_msg_sender   FOREIGN KEY (sender_id)   REFERENCES "user"(id),
    CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_id) REFERENCES "user"(id)
);

CREATE INDEX idx_msg_sender    ON message(sender_id, created_at DESC);
CREATE INDEX idx_msg_receiver  ON message(receiver_id, is_read, created_at DESC);

-- ============================================================
-- Verify code table
-- ============================================================
CREATE TABLE verify_code (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(100) NOT NULL,
    code       VARCHAR(6)   NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_verify_email ON verify_code(email, type, used);
