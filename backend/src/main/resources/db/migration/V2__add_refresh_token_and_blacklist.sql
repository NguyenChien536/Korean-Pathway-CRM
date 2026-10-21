-- Refresh-token rotation and access-token revocation records.
CREATE TABLE refresh_token (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    token       varchar(255) NOT NULL UNIQUE,
    user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  timestamptz NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    is_revoked  boolean NOT NULL DEFAULT false
);

CREATE INDEX ix_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX ix_refresh_token_expiry ON refresh_token(expires_at);

CREATE TABLE invalidated_token (
    id          varchar(255) PRIMARY KEY,
    expiry_time timestamptz NOT NULL
);

CREATE INDEX ix_invalidated_token_expiry ON invalidated_token(expiry_time);
