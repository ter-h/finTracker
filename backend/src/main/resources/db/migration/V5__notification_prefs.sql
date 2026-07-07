-- Notification preferences (already in V1 schema)
-- This migration adds an alert tracking table
CREATE TABLE IF NOT EXISTS budget_alert_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    budget_id       UUID        NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    threshold       INTEGER     NOT NULL,   -- 80 or 100
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    month           DATE        NOT NULL,
    UNIQUE(budget_id, threshold, month)     -- one alert per budget per threshold per month
);

-- Insert default notification prefs for any existing users who don't have them
INSERT INTO notification_prefs (user_id, budget_alert_80, budget_alert_100, weekly_summary)
SELECT id, TRUE, TRUE, FALSE
FROM users
WHERE id NOT IN (SELECT user_id FROM notification_prefs);