CREATE TABLE IF NOT EXISTS reminders (
    id SERIAL PRIMARY KEY,
    message VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    timestamp BIGINT NOT NULL,
    repeat BOOLEAN NOT NULL
);