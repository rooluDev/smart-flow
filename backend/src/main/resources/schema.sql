CREATE DATABASE IF NOT EXISTS smartflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE smartflow;

CREATE TABLE IF NOT EXISTS users (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    google_id               VARCHAR(255)    NOT NULL,
    email                   VARCHAR(255)    NOT NULL,
    name                    VARCHAR(100)    NOT NULL,
    profile_image_url       VARCHAR(500)    NULL,
    google_access_token     TEXT            NULL,
    google_refresh_token    TEXT            NULL,
    google_token_expires_at DATETIME        NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_google_id (google_id),
    UNIQUE KEY uk_users_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversations (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL DEFAULT '새 대화',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_conversations_user_id      (user_id),
    KEY idx_conversations_user_updated (user_id, updated_at DESC),
    CONSTRAINT fk_conversations_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS messages (
    id              BIGINT                      NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT                      NOT NULL,
    role            ENUM('USER','ASSISTANT')    NOT NULL,
    content         LONGTEXT                    NOT NULL,
    mcp_tools_used  JSON                        NULL,
    created_at      DATETIME                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_messages_conversation_id (conversation_id),
    KEY idx_messages_conv_created    (conversation_id, created_at ASC),
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
