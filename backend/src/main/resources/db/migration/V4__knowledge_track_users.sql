ALTER TABLE knowledge_track ADD COLUMN user_id VARCHAR(64) NOT NULL DEFAULT 'demo-user';
CREATE INDEX idx_knowledge_track_user_created ON knowledge_track (user_id, created_at, id);
