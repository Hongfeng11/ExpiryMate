CREATE TABLE shared_lists (
    id CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_shared_lists_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX ix_shared_lists_creator (created_by, created_at)
) ENGINE=InnoDB;

CREATE TABLE shared_list_members (
    id CHAR(36) NOT NULL PRIMARY KEY,
    list_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_shared_list_member UNIQUE (list_id, user_id),
    CONSTRAINT fk_shared_list_members_list FOREIGN KEY (list_id) REFERENCES shared_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_shared_list_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_shared_list_member_role CHECK (role IN ('OWNER','EDITOR','VIEWER')),
    INDEX ix_shared_list_members_user (user_id, list_id)
) ENGINE=InnoDB;

CREATE TABLE list_invitations (
    id CHAR(36) NOT NULL PRIMARY KEY,
    list_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_by CHAR(36) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    accepted_by CHAR(36) NULL,
    accepted_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_list_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_list_invitations_list FOREIGN KEY (list_id) REFERENCES shared_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_list_invitations_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_list_invitations_acceptor FOREIGN KEY (accepted_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_list_invitations_role CHECK (role IN ('EDITOR','VIEWER')),
    INDEX ix_list_invitations_expiry (expires_at, accepted_at)
) ENGINE=InnoDB;

INSERT INTO shared_lists (id, name, created_by)
SELECT UUID(), '我的清单', u.id
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM shared_lists existing
    WHERE existing.created_by = u.id AND existing.name = '我的清单'
);

INSERT INTO shared_list_members (id, list_id, user_id, role)
SELECT UUID(), l.id, l.created_by, 'OWNER'
FROM shared_lists l
WHERE NOT EXISTS (
    SELECT 1 FROM shared_list_members m
    WHERE m.list_id = l.id AND m.user_id = l.created_by
);

ALTER TABLE items ADD COLUMN list_id CHAR(36) NULL AFTER user_id;

UPDATE items i
JOIN shared_lists l ON l.created_by = i.user_id AND l.name = '我的清单'
SET i.list_id = l.id
WHERE i.list_id IS NULL;

ALTER TABLE items
    MODIFY COLUMN list_id CHAR(36) NOT NULL,
    ADD CONSTRAINT fk_items_shared_list FOREIGN KEY (list_id) REFERENCES shared_lists(id) ON DELETE CASCADE,
    ADD INDEX ix_items_list_expiry (list_id, status, deleted_at, expires_on, id);
