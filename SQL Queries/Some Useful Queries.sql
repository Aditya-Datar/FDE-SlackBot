-- ============================================
-- Useful Queries for FDE Slackbot
-- ============================================

-- 1. Get all tickets with message count
SELECT
    t.id,
    t.title,
    t.category,
    t.status,
    COUNT(m.id) as message_count,
    t.created_at,
    t.updated_at
FROM slack_tickets t
LEFT JOIN slack_messages m ON t.id = m.ticket_id
GROUP BY t.id
ORDER BY t.updated_at DESC;

-- 2. Get ticket with all its messages
SELECT
    t.id as ticket_id,
    t.title,
    t.category,
    m.id as message_id,
    m.slackText,
    m.slackUser,
    m.slack_message_time
FROM slack_tickets t
LEFT JOIN slack_messages m ON t.id = m.ticket_id
WHERE t.id = 1
ORDER BY m.slack_message_time ASC;

-- 3. Get all open tickets
SELECT * FROM slack_tickets
WHERE status = 'OPEN'
ORDER BY updated_at DESC;

-- 4. Get tickets by category
SELECT * FROM slack_tickets
WHERE category = 'BUG'
ORDER BY created_at DESC;

-- 5. Check for duplicate messages
SELECT slack_timestamp, COUNT(*)
FROM slack_messages
GROUP BY slack_timestamp
HAVING COUNT(*) > 1;

-- 6. Get recent messages with embeddings (for grouping)
SELECT
    m.id,
    m.slackText,
    m.ticket_id,
    t.category,
    m.slack_message_time
FROM slack_messages m
JOIN slack_tickets t ON m.ticket_id = t.id
WHERE m.slack_message_time >= NOW() - INTERVAL '24 hours'
  AND m.embedding IS NOT NULL
ORDER BY m.slack_message_time DESC;

-- 7. Get messages by thread
SELECT * FROM slack_messages
WHERE thread_ts = '1234567890.123456'
ORDER BY slack_message_time ASC;

-- 8. Statistics dashboard query
SELECT
    COUNT(*) as total_tickets,
    COUNT(CASE WHEN status = 'OPEN' THEN 1 END) as open_tickets,
    COUNT(CASE WHEN status = 'CLOSED' THEN 1 END) as closed_tickets,
    COUNT(CASE WHEN category = 'BUG' THEN 1 END) as bugs,
    COUNT(CASE WHEN category = 'FEATURE_REQUEST' THEN 1 END) as feature_requests,
    COUNT(CASE WHEN category = 'SUPPORT' THEN 1 END) as support_questions,
    COUNT(CASE WHEN category = 'QUESTION' THEN 1 END) as general_questions
FROM slack_tickets;

-- 9. Get most active channels
SELECT
    channel,
    COUNT(*) as message_count
FROM slack_messages
GROUP BY channel
ORDER BY message_count DESC;

-- 10. Get tickets created today
SELECT * FROM slack_tickets
WHERE DATE(created_at) = CURRENT_DATE
ORDER BY created_at DESC;

-- 11. Search tickets by keyword
SELECT DISTINCT t.*
FROM slack_tickets t
JOIN slack_messages m ON t.id = m.ticket_id
WHERE t.title ILIKE '%login%'
   OR m.text ILIKE '%login%'
ORDER BY t.updated_at DESC;

-- 12. Get user activity
SELECT
    user,
    COUNT(*) as message_count,
    MIN(slack_message_time) as first_message,
    MAX(slack_message_time) as last_message
FROM slack_messages
GROUP BY user
ORDER BY message_count DESC;

-- ============================================
-- Maintenance Queries
-- ============================================

-- Delete old closed tickets (older than 30 days)
DELETE FROM slack_tickets
WHERE status = 'CLOSED'
  AND updated_at < NOW() - INTERVAL '30 days';

-- Vacuum and analyze tables for performance
VACUUM ANALYZE slack_tickets;
VACUUM ANALYZE slack_messages;

-- Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- ============================================
-- Index Performance Check
-- ============================================

-- Check if indexes are being used
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;