CREATE INDEX idx_lecture_published_query
    ON lecture (generation_id, is_published, deleted_at, week, id);
