ALTER TABLE lecture
    ADD COLUMN published_at datetime(6) NULL AFTER created_by;

UPDATE lecture
SET published_at = created_at
WHERE is_published = TRUE;
