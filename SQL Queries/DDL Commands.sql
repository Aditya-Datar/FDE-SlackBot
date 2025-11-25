-- ============================================
-- FDE Slackbot Database Schema for Neon DB
-- ============================================

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS slack_messages CASCADE;
DROP TABLE IF EXISTS slack_tickets CASCADE;

-- ============================================
-- Table: slack_tickets
-- Stores grouped issues/requests from customers
-- ============================================
CREATE TABLE slack_tickets (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Table: slack_messages
-- Stores individual Slack messages
-- ============================================
CREATE TABLE slack_messages (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    slack_text VARCHAR(4000) NOT NULL,
    slack_user VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    slack_timestamp VARCHAR(50),
    thread_ts VARCHAR(50),
    channel_type VARCHAR(50),
    embedding TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    slack_message_time TIMESTAMP NOT NULL,

    -- Foreign key constraint
    CONSTRAINT fk_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES slack_tickets(id)
        ON DELETE CASCADE
);

-- ============================================
-- Indexes for Performance
-- ============================================

-- Index on slack_timestamp for fast duplicate checking
CREATE INDEX idx_slack_ts ON slack_messages(slack_timestamp);

-- Index on thread_ts for fast thread-based grouping
CREATE INDEX idx_thread_ts ON slack_messages(thread_ts);

-- Index on channel for filtering by channel
CREATE INDEX idx_channel ON slack_messages(channel);

-- Index on ticket_id for fast message retrieval
CREATE INDEX idx_ticket_id ON slack_messages(ticket_id);

-- Index on slack_message_time for time-based queries
CREATE INDEX idx_message_time ON slack_messages(slack_message_time);

-- Index on category for filtering tickets
CREATE INDEX idx_category ON slack_tickets(category);

-- Index on status for filtering tickets
CREATE INDEX idx_status ON slack_tickets(status);

-- Index on updated_at for sorting by recent activity
CREATE INDEX idx_updated_at ON slack_tickets(updated_at DESC);

-- ============================================
-- Trigger: Update updated_at on slack_tickets
-- Automatically update timestamp when ticket changes
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_slack_tickets_updated_at
    BEFORE UPDATE ON slack_tickets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Comments for Documentation
-- ============================================
COMMENT ON TABLE slack_tickets IS 'Stores grouped customer issues/requests detected by the FDE Slackbot';
COMMENT ON TABLE slack_messages IS 'Stores individual Slack messages with embeddings for similarity matching';

COMMENT ON COLUMN slack_tickets.category IS 'BUG, FEATURE_REQUEST, SUPPORT, QUESTION, or NONE';
COMMENT ON COLUMN slack_tickets.status IS 'OPEN or CLOSED';
COMMENT ON COLUMN slack_messages.slack_timestamp IS 'Unique Slack message timestamp (prevents duplicates)';
COMMENT ON COLUMN slack_messages.thread_ts IS 'Slack thread timestamp for grouping threaded messages';
COMMENT ON COLUMN slack_messages.embedding IS 'JSON array of embedding vectors for semantic similarity';