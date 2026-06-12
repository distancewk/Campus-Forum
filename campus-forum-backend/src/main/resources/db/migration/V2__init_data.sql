-- ============================================================
-- Insert preset boards
-- ============================================================
INSERT INTO board (name, description, icon, sort_order, status) VALUES
('校园公告', '学校官方通知、公告、新闻', '📢', 1, 1),
('二手交易', '闲置物品转让、求购', '🛒', 2, 1),
('失物招领', '丢失物品寻找、拾到物品招领', '🔍', 3, 1),
('表白墙', '表白、交友、情感分享', '💕', 4, 1),
('学习交流', '课程讨论、学习资料、考试经验', '📚', 5, 1),
('灌水区', '自由讨论、闲聊灌水', '💧', 6, 1);

-- ============================================================
-- Insert admin account
-- BCrypt hash for the bootstrap admin account. Rotate it immediately after first deployment.
-- ============================================================
INSERT INTO "user" (student_no, nickname, password, email, role, status) VALUES
('admin001', '系统管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@xxx.edu.cn', 'ADMIN', 1);
