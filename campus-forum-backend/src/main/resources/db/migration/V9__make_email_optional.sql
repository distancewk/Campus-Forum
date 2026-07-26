-- 邮箱验证功能暂未启用：邮箱改为可选字段，允许为空（无邮箱也可注册）
-- 唯一索引 uk_user_email 对 NULL 不施加约束，多个无邮箱用户不会冲突
ALTER TABLE "user" ALTER COLUMN email DROP NOT NULL;
