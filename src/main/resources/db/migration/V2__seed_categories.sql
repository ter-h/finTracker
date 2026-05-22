-- System default categories (user_id is NULL = available to all users)
-- Make sure that the user can see categories instead of no categories
INSERT INTO categories (id, user_id, name, icon, color, is_system) VALUES
    (gen_random_uuid(), NULL, 'Housing',        'home',          '#6366f1', TRUE),
    (gen_random_uuid(), NULL, 'Food & Dining',  'fork-knife',    '#f59e0b', TRUE),
    (gen_random_uuid(), NULL, 'Transport',      'car',           '#3b82f6', TRUE),
    (gen_random_uuid(), NULL, 'Shopping',       'shopping-bag',  '#ec4899', TRUE),
    (gen_random_uuid(), NULL, 'Health',         'heart',         '#ef4444', TRUE),
    (gen_random_uuid(), NULL, 'Entertainment',  'music',         '#8b5cf6', TRUE),
    (gen_random_uuid(), NULL, 'Utilities',      'zap',           '#14b8a6', TRUE),
    (gen_random_uuid(), NULL, 'Education',      'book',          '#0ea5e9', TRUE),
    (gen_random_uuid(), NULL, 'Travel',         'plane',         '#f97316', TRUE),
    (gen_random_uuid(), NULL, 'Personal Care',  'smile',         '#a855f7', TRUE),
    (gen_random_uuid(), NULL, 'Subscriptions',  'repeat',        '#06b6d4', TRUE),
    (gen_random_uuid(), NULL, 'Investments',    'trending-up',   '#10b981', TRUE),
    (gen_random_uuid(), NULL, 'Insurance',      'shield',        '#64748b', TRUE),
    (gen_random_uuid(), NULL, 'Gifts',          'gift',          '#f43f5e', TRUE),
    (gen_random_uuid(), NULL, 'Salary',         'briefcase',     '#22c55e', TRUE),
    (gen_random_uuid(), NULL, 'Freelance',      'code',          '#84cc16', TRUE),
    (gen_random_uuid(), NULL, 'Other Income',   'plus-circle',   '#a3e635', TRUE),
    (gen_random_uuid(), NULL, 'Other Expense',  'minus-circle',  '#94a3b8', TRUE);