ALTER TABLE items
    DROP CHECK ck_items_quantity_positive,
    ADD CONSTRAINT ck_items_quantity_nonnegative
        CHECK (quantity >= 0 AND (quantity > 0 OR status <> 'active'));
