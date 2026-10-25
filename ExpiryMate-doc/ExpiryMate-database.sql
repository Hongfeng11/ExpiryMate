-- ExpiryMate initial schema for MySQL 8.0.16+
-- Creates the database if needed, then selects it. Existing tables are left intact.
CREATE DATABASE IF NOT EXISTS expirymate
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE expirymate;

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    email_lower VARCHAR(254) GENERATED ALWAYS AS (LOWER(email)) STORED,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80),
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_email_lower UNIQUE (email_lower),
    CONSTRAINT ck_users_email_not_blank CHECK (CHAR_LENGTH(TRIM(email)) > 0),
    CONSTRAINT ck_users_timezone_not_blank CHECK (CHAR_LENGTH(TRIM(timezone)) > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS items (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(60) NOT NULL DEFAULT '其他',
    location VARCHAR(120),
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit VARCHAR(30) NOT NULL DEFAULT '件',
    manufactured_on DATE,
    purchased_on DATE,
    shelf_life_value INT,
    shelf_life_unit VARCHAR(10),
    expires_on DATE NOT NULL,
    package_expires_on DATE,
    opened_on DATE,
    opened_shelf_life_value INT,
    opened_shelf_life_unit VARCHAR(10),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_items_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_items_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_items_quantity_nonnegative CHECK (quantity >= 0 AND (quantity > 0 OR status <> 'active')),
    CONSTRAINT ck_items_shelf_life_positive CHECK (shelf_life_value IS NULL OR shelf_life_value > 0),
    CONSTRAINT ck_items_shelf_life_unit CHECK (shelf_life_unit IS NULL OR shelf_life_unit IN ('days','months','years')),
    CONSTRAINT ck_items_shelf_life_pair CHECK ((shelf_life_value IS NULL AND shelf_life_unit IS NULL) OR (shelf_life_value IS NOT NULL AND shelf_life_unit IS NOT NULL)),
    CONSTRAINT ck_items_opened_shelf_life_positive CHECK (opened_shelf_life_value IS NULL OR opened_shelf_life_value > 0),
    CONSTRAINT ck_items_opened_shelf_life_unit CHECK (opened_shelf_life_unit IS NULL OR opened_shelf_life_unit IN ('days','months','years')),
    CONSTRAINT ck_items_opened_shelf_life_pair CHECK ((opened_shelf_life_value IS NULL AND opened_shelf_life_unit IS NULL) OR (opened_shelf_life_value IS NOT NULL AND opened_shelf_life_unit IS NOT NULL)),
    CONSTRAINT ck_items_purchase_after_manufacture CHECK (manufactured_on IS NULL OR purchased_on IS NULL OR purchased_on >= manufactured_on),
    CONSTRAINT ck_items_expiry_after_manufacture CHECK (manufactured_on IS NULL OR expires_on >= manufactured_on),
    CONSTRAINT ck_items_opened_after_purchase CHECK (opened_on IS NULL OR purchased_on IS NULL OR opened_on >= purchased_on),
    CONSTRAINT ck_items_opened_after_manufacture CHECK (opened_on IS NULL OR manufactured_on IS NULL OR opened_on >= manufactured_on),
    CONSTRAINT ck_items_status CHECK (status IN ('active','used_up','discarded','processed')),
    INDEX ix_items_user_expiry (user_id, status, deleted_at, expires_on, id),
    INDEX ix_items_user_category (user_id, status, deleted_at, category, expires_on),
    INDEX ix_items_user_location (user_id, status, deleted_at, location),
    INDEX ix_items_user_name (user_id, name)
) ENGINE=InnoDB;
-- expires_on stores the effective date: the earlier of the unopened/package expiry and opened expiry.

CREATE TABLE IF NOT EXISTS reminder_preferences (
    user_id CHAR(36) NOT NULL PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    remind_before_days JSON NOT NULL DEFAULT (JSON_ARRAY(7, 3, 1, 0)),
    preferred_time TIME NOT NULL DEFAULT '09:00:00',
    channels JSON NOT NULL DEFAULT (JSON_ARRAY('in_app')),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reminder_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_reminder_days_array CHECK (JSON_TYPE(remind_before_days) = 'ARRAY'),
    CONSTRAINT ck_reminder_channels_array CHECK (JSON_TYPE(channels) = 'ARRAY')
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    item_id CHAR(36),
    item_name_snapshot VARCHAR(120) NOT NULL,
    expires_on_snapshot DATE NOT NULL,
    reminder_days SMALLINT NOT NULL,
    rule_key VARCHAR(80) NOT NULL,
    dedupe_key VARCHAR(160) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'in_app',
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'delivered',
    scheduled_for DATETIME(6) NOT NULL,
    sent_at DATETIME(6),
    read_at DATETIME(6),
    retry_count SMALLINT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE SET NULL,
    CONSTRAINT uq_notifications_dedupe UNIQUE (user_id, dedupe_key, channel),
    CONSTRAINT ck_notifications_reminder_days CHECK (reminder_days >= 0),
    CONSTRAINT ck_notifications_retry_count CHECK (retry_count >= 0),
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('in_app','web_push')),
    CONSTRAINT ck_notifications_delivery_status CHECK (delivery_status IN ('pending','delivered','failed')),
    INDEX ix_notifications_user_inbox (user_id, channel, created_at, id),
    INDEX ix_notifications_user_unread (user_id, channel, read_at, created_at),
    INDEX ix_notifications_push_queue (channel, delivery_status, scheduled_for, id),
    INDEX ix_notifications_item (item_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    endpoint_hash BINARY(32) NOT NULL,
    endpoint_ciphertext BLOB NOT NULL,
    p256dh_ciphertext BLOB NOT NULL,
    auth_ciphertext BLOB NOT NULL,
    encryption_key_id VARCHAR(100) NOT NULL,
    expiration_time DATETIME(6),
    device_label VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_used_at DATETIME(6),
    revoked_at DATETIME(6),
    active_endpoint_hash BINARY(32) GENERATED ALWAYS AS (IF(revoked_at IS NULL, endpoint_hash, NULL)) STORED,
    CONSTRAINT fk_push_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_push_subscriptions_endpoint_active UNIQUE (active_endpoint_hash),
    INDEX ix_push_subscriptions_user_active (user_id, revoked_at, created_at)
) ENGINE=InnoDB;

-- MySQL stores timestamps without a time-zone identifier. Backend connections must use UTC.
-- Create the user's reminder_preferences row in the same transaction as account registration.
-- MySQL has no partial indexes; generated active_endpoint_hash enforces active endpoint uniqueness.
