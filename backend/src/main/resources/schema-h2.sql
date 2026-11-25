DROP TABLE IF EXISTS auth_session;
DROP TABLE IF EXISTS account;

CREATE TABLE account (
  login_name    VARCHAR(64)  NOT NULL PRIMARY KEY,
  password_hash VARCHAR(100) NOT NULL,
  role_code     VARCHAR(24)  NOT NULL,
  business_id   VARCHAR(64)  NOT NULL,
  display_name  VARCHAR(100) NOT NULL,
  enabled       TINYINT      NOT NULL DEFAULT 1,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_session (
  token_hash   CHAR(64)     NOT NULL PRIMARY KEY,
  user_id      VARCHAR(64)  NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  role_code    VARCHAR(24)  NOT NULL,
  business_id  VARCHAR(64)  NULL,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
