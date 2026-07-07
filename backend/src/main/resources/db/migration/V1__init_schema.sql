-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name  VARCHAR(100) NOT NULL,
    google_id     VARCHAR(100) UNIQUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

-- Refresh tokens (for JWT auth)
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ
);

-- Categories (system defaults + user custom)
CREATE TABLE categories (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID         REFERENCES users (id) ON DELETE CASCADE,
    name      VARCHAR(100) NOT NULL,
    icon      VARCHAR(50),
    color     CHAR(7),
    parent_id UUID         REFERENCES categories (id),
    is_system BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Accounts (bank accounts, credit cards, cash, etc.)
CREATE TABLE accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    currency    CHAR(3)      NOT NULL DEFAULT 'AUD',
    balance     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    is_archived BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Transactions
CREATE TABLE transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    account_id       UUID           NOT NULL REFERENCES accounts (id),
    category_id      UUID           REFERENCES categories (id),
    type             VARCHAR(20)    NOT NULL,
    amount           DECIMAL(15, 2) NOT NULL,
    payee            VARCHAR(255),
    notes            TEXT,
    date             DATE           NOT NULL,
    transfer_pair_id UUID,
    import_hash      VARCHAR(64),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Performance indexes
CREATE INDEX idx_transactions_user_date     ON transactions (user_id, date DESC);
CREATE INDEX idx_transactions_user_cat_date ON transactions (user_id, category_id, date DESC);
CREATE INDEX idx_accounts_user             ON accounts (user_id);
CREATE INDEX idx_refresh_tokens_user       ON refresh_tokens (user_id);

-- Budgets
CREATE TABLE budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID           NOT NULL REFERENCES categories (id),
    month       DATE           NOT NULL,
    amount      DECIMAL(15, 2) NOT NULL,
    rollover    BOOLEAN        NOT NULL DEFAULT FALSE,
    UNIQUE (user_id, category_id, month)
);

-- Notification preferences
CREATE TABLE notification_prefs (
    user_id          UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    budget_alert_80  BOOLEAN      NOT NULL DEFAULT TRUE,
    budget_alert_100 BOOLEAN      NOT NULL DEFAULT TRUE,
    weekly_summary   BOOLEAN      NOT NULL DEFAULT FALSE,
    alert_email      VARCHAR(255)
);