-- 补齐 board 逻辑删除列，与其他核心表保持一致
ALTER TABLE board ADD COLUMN deleted SMALLINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uk_board_name_active ON board(name) WHERE deleted = 0;
