ALTER TABLE items
    ADD COLUMN package_expires_on DATE NULL AFTER expires_on,
    ADD COLUMN opened_shelf_life_value INT NULL AFTER opened_on,
    ADD COLUMN opened_shelf_life_unit VARCHAR(10) NULL AFTER opened_shelf_life_value,
    ADD CONSTRAINT ck_items_opened_shelf_life_positive
        CHECK (opened_shelf_life_value IS NULL OR opened_shelf_life_value > 0),
    ADD CONSTRAINT ck_items_opened_shelf_life_unit
        CHECK (opened_shelf_life_unit IS NULL OR opened_shelf_life_unit IN ('days','months','years')),
    ADD CONSTRAINT ck_items_opened_shelf_life_pair
        CHECK ((opened_shelf_life_value IS NULL AND opened_shelf_life_unit IS NULL)
            OR (opened_shelf_life_value IS NOT NULL AND opened_shelf_life_unit IS NOT NULL)),
    ADD CONSTRAINT ck_items_opened_after_manufacture
        CHECK (opened_on IS NULL OR manufactured_on IS NULL OR opened_on >= manufactured_on);
