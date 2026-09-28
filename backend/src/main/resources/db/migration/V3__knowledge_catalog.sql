CREATE TABLE knowledge_track (
    id VARCHAR(64) PRIMARY KEY,
    direction_query VARCHAR(120) NOT NULL,
    track_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
