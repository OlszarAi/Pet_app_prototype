-- V1__init_schema.sql
-- Pelny schemat bazy danych PetsApp
-- Wersja: 1
-- UWAGA: tej migracji nie modyfikujemy — nowe zmiany w kolejnych wersjach V2, V3 itd.

-- =====================
-- TYPY
-- =====================

CREATE TYPE friendship_status AS ENUM ('pending', 'accepted', 'blocked');

-- =====================
-- TABELE GLOWNE
-- =====================

CREATE TABLE "user" (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username         VARCHAR(30) NOT NULL UNIQUE,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255),                        -- nullable jesli OAuth
    avatar_url       VARCHAR(500),
    bio              VARCHAR(150),
    total_catches    INTEGER     NOT NULL DEFAULT 0,
    unique_breeds    INTEGER     NOT NULL DEFAULT 0,
    email_verified   BOOLEAN     NOT NULL DEFAULT false,
    is_private       BOOLEAN     NOT NULL DEFAULT false,
    oauth_provider   VARCHAR(20),                         -- 'google' | 'apple' | null
    oauth_id         VARCHAR(255),
    deleted_at       TIMESTAMP,                           -- soft delete
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE breed (
    id               SERIAL      PRIMARY KEY,
    name             VARCHAR(100) NOT NULL UNIQUE,
    name_pl          VARCHAR(100) NOT NULL,
    "group"          VARCHAR(50),
    size_category    VARCHAR(20),                         -- 'small' | 'medium' | 'large'
    description      TEXT,
    silhouette_url   VARCHAR(500),
    rarity_score     SMALLINT    NOT NULL DEFAULT 1 CHECK (rarity_score BETWEEN 1 AND 5),
    is_active        BOOLEAN     NOT NULL DEFAULT true
);

CREATE TABLE dog_catch (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID        NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    breed_id              INTEGER     NOT NULL REFERENCES breed(id),
    photo_url             VARCHAR(500) NOT NULL,
    thumbnail_url         VARCHAR(500) NOT NULL,
    caption               VARCHAR(300),
    latitude              DOUBLE PRECISION,
    longitude             DOUBLE PRECISION,
    location_name         VARCHAR(255),
    is_public             BOOLEAN     NOT NULL DEFAULT true,
    like_count            INTEGER     NOT NULL DEFAULT 0,
    comment_count         INTEGER     NOT NULL DEFAULT 0,
    feed_score            DOUBLE PRECISION NOT NULL DEFAULT 0,
    ai_suggested_breed_id INTEGER     REFERENCES breed(id),  -- przyszlosc
    ai_confidence         DOUBLE PRECISION,
    deleted_at            TIMESTAMP,                          -- soft delete
    caught_at             TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE friendship (
    id            UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id  UUID              NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    addressee_id  UUID              NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    status        friendship_status NOT NULL DEFAULT 'pending',
    created_at    TIMESTAMP         NOT NULL DEFAULT now(),
    CONSTRAINT no_self_friendship CHECK (requester_id != addressee_id),
    CONSTRAINT unique_friendship UNIQUE (requester_id, addressee_id)
);

CREATE TABLE "like" (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    catch_id   UUID      NOT NULL REFERENCES dog_catch(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT unique_like UNIQUE (user_id, catch_id)
);

CREATE TABLE comment (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    catch_id   UUID      NOT NULL REFERENCES dog_catch(id) ON DELETE CASCADE,
    content    VARCHAR(500) NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- =====================
-- TABELE AUTH I SYSTEMOWE
-- =====================

CREATE TABLE refresh_token (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    expires_at  TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE email_verification (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    code       VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE password_reset (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP   NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE device_token (
    id         UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID       NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    platform   VARCHAR(10) NOT NULL CHECK (platform IN ('ios', 'android')),
    created_at TIMESTAMP  NOT NULL DEFAULT now()
);

CREATE TABLE notification (
    id         UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID       NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       VARCHAR(500),
    data_json  JSONB,
    is_read    BOOLEAN    NOT NULL DEFAULT false,
    created_at TIMESTAMP  NOT NULL DEFAULT now()
);

CREATE TABLE user_settings (
    user_id               UUID    PRIMARY KEY REFERENCES "user"(id) ON DELETE CASCADE,
    push_likes            BOOLEAN NOT NULL DEFAULT true,
    push_comments         BOOLEAN NOT NULL DEFAULT true,
    push_friend_requests  BOOLEAN NOT NULL DEFAULT true,
    push_achievements     BOOLEAN NOT NULL DEFAULT true,
    language              VARCHAR(5)  NOT NULL DEFAULT 'pl',
    dark_mode             BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE report (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID        NOT NULL REFERENCES "user"(id),
    catch_id    UUID        REFERENCES dog_catch(id),
    user_id     UUID        REFERENCES "user"(id),
    reason      VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE achievement (
    id               INTEGER     PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    code             VARCHAR(50) NOT NULL UNIQUE,
    name             VARCHAR(100) NOT NULL,
    name_pl          VARCHAR(100) NOT NULL,
    description      VARCHAR(255),
    icon_url         VARCHAR(500),
    condition_type   VARCHAR(50) NOT NULL,
    condition_value  INTEGER     NOT NULL
);

CREATE TABLE achievement_unlock (
    id             UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID      NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    achievement_id INTEGER   NOT NULL REFERENCES achievement(id),
    unlocked_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT unique_achievement_unlock UNIQUE (user_id, achievement_id)
);

-- =====================
-- INDEKSY
-- =====================

CREATE INDEX idx_catch_user        ON dog_catch (user_id, caught_at DESC);
CREATE INDEX idx_catch_breed       ON dog_catch (breed_id);
CREATE INDEX idx_catch_feed        ON dog_catch (is_public, caught_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_catch_score       ON dog_catch (feed_score DESC) WHERE deleted_at IS NULL AND is_public = true;

CREATE INDEX idx_friendship        ON friendship (requester_id, addressee_id, status);
CREATE INDEX idx_friendship_reverse ON friendship (addressee_id, requester_id, status);

CREATE INDEX idx_like_catch        ON "like" (catch_id);

CREATE INDEX idx_notification_user ON notification (user_id, is_read, created_at DESC);

CREATE INDEX idx_refresh_token     ON refresh_token (token_hash);

CREATE INDEX idx_device_token_user ON device_token (user_id);

CREATE INDEX idx_report_status     ON report (status, created_at DESC);

CREATE INDEX idx_user_username     ON "user" (username) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_email        ON "user" (email) WHERE deleted_at IS NULL;
