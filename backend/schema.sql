-- 在 account 库执行一次。
-- 账号与身份放在同一张表：一个身份一个账号，登录时不需要选择身份。

CREATE TABLE IF NOT EXISTS account (
  login_name    VARCHAR(64)  NOT NULL PRIMARY KEY,
  password_hash VARCHAR(100) NOT NULL,
  role_code     VARCHAR(24)  NOT NULL,
  business_id   VARCHAR(64)  NOT NULL,
  display_name  VARCHAR(100) NOT NULL,
  enabled       TINYINT      NOT NULL DEFAULT 1,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 登录凭证表：只保存令牌的 SHA-256 摘要，不保存明文令牌。
CREATE TABLE IF NOT EXISTS auth_session (
  token_hash   CHAR(64)     NOT NULL PRIMARY KEY,
  user_id      VARCHAR(64)  NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  role_code    VARCHAR(24)  NOT NULL,
  business_id  VARCHAR(64)  NULL,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 该索引只需创建一次；重复执行会报已存在，可忽略。
CREATE INDEX idx_auth_session_expires ON auth_session (expires_at);
