CREATE TABLE reminder_preferences (
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
