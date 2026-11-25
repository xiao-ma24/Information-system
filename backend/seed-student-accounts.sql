-- 学生演示账号：从 student_status_mgmt.student 生成。
-- 初始密码统一为 Campus@123（BCrypt 哈希已固定，所有学生相同）。
-- 可重复执行：已存在的登录名会跳过。

INSERT IGNORE INTO account.account
  (login_name, password_hash, role_code, business_id, display_name, enabled)
SELECT
  s.student_id,
  '$2a$10$Q.0D34mrn.JsL436Kg44A.ALkLw.BCXnK1hIwpg/FML1MVypYiRDK',
  'STUDENT',
  s.student_id,
  s.student_name,
  1
FROM student_status_mgmt.student s;
