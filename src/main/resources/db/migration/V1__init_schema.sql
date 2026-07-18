-- ============================================================
-- SmartTask Enterprise - Initial Schema
-- V1__init_schema.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- ORGANIZATIONS
-- ------------------------------------------------------------
CREATE TABLE organizations (
    id            CHAR(36)     NOT NULL DEFAULT (UUID()),
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    logo          VARCHAR(500),
    website       VARCHAR(300),
    status        ENUM('ACTIVE','INACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_org_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- USERS
-- ------------------------------------------------------------
CREATE TABLE users (
    id                  CHAR(36)     NOT NULL DEFAULT (UUID()),
    first_name          VARCHAR(80)  NOT NULL,
    last_name           VARCHAR(80)  NOT NULL,
    email               VARCHAR(200) NOT NULL,
    phone               VARCHAR(20),
    password            VARCHAR(255) NOT NULL,
    role                ENUM('SUPER_ADMIN','ORGANIZATION_ADMIN','PROJECT_MANAGER',
                             'TEAM_LEAD','DEVELOPER','TESTER','VIEWER') NOT NULL DEFAULT 'DEVELOPER',
    status              ENUM('ACTIVE','INACTIVE','SUSPENDED','PENDING_VERIFICATION') NOT NULL DEFAULT 'PENDING_VERIFICATION',
    profile_picture     VARCHAR(500),
    email_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    organization_id     CHAR(36),
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_email (email),
    INDEX idx_user_org (organization_id),
    INDEX idx_user_role (role),
    INDEX idx_user_status (status),
    CONSTRAINT fk_user_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- REFRESH TOKENS
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    token       VARCHAR(500) NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    expiry_date DATETIME     NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_token (token),
    INDEX idx_rt_user (user_id),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- EMAIL VERIFICATION TOKENS
-- ------------------------------------------------------------
CREATE TABLE email_verification_tokens (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    token       VARCHAR(255) NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    expiry_date DATETIME     NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_evt_token (token),
    INDEX idx_evt_user (user_id),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- PASSWORD RESET TOKENS
-- ------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    token       VARCHAR(255) NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    expiry_date DATETIME     NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_prt_token (token),
    INDEX idx_prt_user (user_id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- TEAMS
-- ------------------------------------------------------------
CREATE TABLE teams (
    id              CHAR(36)     NOT NULL DEFAULT (UUID()),
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    organization_id CHAR(36)     NOT NULL,
    team_lead_id    CHAR(36),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_team_name_org (name, organization_id),
    INDEX idx_team_org (organization_id),
    INDEX idx_team_lead (team_lead_id),
    CONSTRAINT fk_team_org  FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_lead FOREIGN KEY (team_lead_id)    REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- TEAM MEMBERS (join table)
-- ------------------------------------------------------------
CREATE TABLE team_members (
    team_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, user_id),
    CONSTRAINT fk_tm_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- PROJECTS
-- ------------------------------------------------------------
CREATE TABLE projects (
    id              CHAR(36)     NOT NULL DEFAULT (UUID()),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    status          ENUM('PLANNING','ACTIVE','ON_HOLD','COMPLETED','ARCHIVED','CANCELLED') NOT NULL DEFAULT 'PLANNING',
    start_date      DATE,
    end_date        DATE,
    organization_id CHAR(36)     NOT NULL,
    created_by_id   CHAR(36),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME,
    PRIMARY KEY (id),
    INDEX idx_project_org    (organization_id),
    INDEX idx_project_status (status),
    INDEX idx_project_creator(created_by_id),
    CONSTRAINT fk_project_org     FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_creator FOREIGN KEY (created_by_id)   REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- PROJECT MEMBERS (join table)
-- ------------------------------------------------------------
CREATE TABLE project_members (
    project_id CHAR(36) NOT NULL,
    user_id    CHAR(36) NOT NULL,
    joined_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- TASKS
-- ------------------------------------------------------------
CREATE TABLE tasks (
    id               CHAR(36)     NOT NULL DEFAULT (UUID()),
    title            VARCHAR(300) NOT NULL,
    description      TEXT,
    priority         ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    status           ENUM('TODO','IN_PROGRESS','IN_REVIEW','DONE') NOT NULL DEFAULT 'TODO',
    due_date         DATE,
    estimated_hours  DECIMAL(6,2),
    actual_hours     DECIMAL(6,2),
    project_id       CHAR(36)     NOT NULL,
    assigned_user_id CHAR(36),
    reporter_id      CHAR(36),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       DATETIME,
    PRIMARY KEY (id),
    INDEX idx_task_project  (project_id),
    INDEX idx_task_assigned (assigned_user_id),
    INDEX idx_task_reporter (reporter_id),
    INDEX idx_task_status   (status),
    INDEX idx_task_priority (priority),
    CONSTRAINT fk_task_project  FOREIGN KEY (project_id)       REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assigned FOREIGN KEY (assigned_user_id) REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT fk_task_reporter FOREIGN KEY (reporter_id)      REFERENCES users(id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- COMMENTS
-- ------------------------------------------------------------
CREATE TABLE comments (
    id         CHAR(36) NOT NULL DEFAULT (UUID()),
    task_id    CHAR(36) NOT NULL,
    author_id  CHAR(36),
    message    TEXT     NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    PRIMARY KEY (id),
    INDEX idx_comment_task   (task_id),
    INDEX idx_comment_author (author_id),
    CONSTRAINT fk_comment_task   FOREIGN KEY (task_id)   REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- FILE ATTACHMENTS
-- ------------------------------------------------------------
CREATE TABLE file_attachments (
    id            CHAR(36)     NOT NULL DEFAULT (UUID()),
    task_id       CHAR(36)     NOT NULL,
    file_name     VARCHAR(300) NOT NULL,
    file_type     VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    uploaded_by_id CHAR(36),
    uploaded_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    DATETIME,
    PRIMARY KEY (id),
    INDEX idx_attach_task     (task_id),
    INDEX idx_attach_uploader (uploaded_by_id),
    CONSTRAINT fk_attach_task     FOREIGN KEY (task_id)        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_attach_uploader FOREIGN KEY (uploaded_by_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- NOTIFICATIONS
-- ------------------------------------------------------------
CREATE TABLE notifications (
    id           CHAR(36)     NOT NULL DEFAULT (UUID()),
    recipient_id CHAR(36)     NOT NULL,
    title        VARCHAR(300) NOT NULL,
    message      TEXT         NOT NULL,
    type         ENUM('TASK_ASSIGNED','TASK_UPDATED','TASK_COMPLETED','COMMENT_ADDED',
                      'PROJECT_UPDATED','DEADLINE_REMINDER','GENERAL') NOT NULL DEFAULT 'GENERAL',
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    reference_id CHAR(36)     COMMENT 'ID of the referenced entity',
    reference_type VARCHAR(50) COMMENT 'Type of the referenced entity',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_notif_recipient (recipient_id),
    INDEX idx_notif_read      (is_read),
    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- ACTIVITY LOGS
-- ------------------------------------------------------------
CREATE TABLE activity_logs (
    id            CHAR(36)     NOT NULL DEFAULT (UUID()),
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(80)  NOT NULL,
    entity_id     CHAR(36)     NOT NULL,
    performed_by_id CHAR(36),
    description   TEXT,
    ip_address    VARCHAR(45),
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_log_entity  (entity_type, entity_id),
    INDEX idx_log_performer (performed_by_id),
    INDEX idx_log_created (created_at),
    CONSTRAINT fk_log_performer FOREIGN KEY (performed_by_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
