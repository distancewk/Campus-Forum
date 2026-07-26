-- 移除随仓库分发的硬编码默认管理员（V2 种子），改由 AdminInitializer 从环境变量注入强密码。
-- 覆盖占位邮箱与真实种子邮箱两种变体，确保默认凭据被清除，避免默认管理员被利用。
DELETE FROM "user"
WHERE student_no = 'admin001'
  AND role = 'ADMIN'
  AND (email = 'admin@your-school.edu.cn' OR email = 'admin@xxx.edu.cn');
