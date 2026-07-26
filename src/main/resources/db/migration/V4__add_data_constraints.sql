-- Explicit CHECK constraints for invariants that today only live in
-- Jakarta Bean Validation (@Positive, @PositiveOrZero, @NotBlank on the entities).
-- This makes the database itself the last line of defense against invalid data,
-- independent of the application layer.

ALTER TABLE products ADD CONSTRAINT chk_products_price_positive CHECK (price > 0);
ALTER TABLE products ADD CONSTRAINT chk_products_quantity_non_negative CHECK (quantity >= 0);
ALTER TABLE products ADD CONSTRAINT chk_products_min_stock_non_negative CHECK (min_stock >= 0);
ALTER TABLE products ADD CONSTRAINT chk_products_sku_not_blank CHECK (btrim(sku) <> '');
ALTER TABLE products ADD CONSTRAINT chk_products_name_not_blank CHECK (btrim(name) <> '');
ALTER TABLE products ADD CONSTRAINT chk_products_status_valid CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE stock_movements ADD CONSTRAINT chk_stock_movements_quantity_positive CHECK (quantity > 0);
ALTER TABLE stock_movements ADD CONSTRAINT chk_stock_movements_type_valid CHECK (movement_type IN ('IN', 'OUT', 'ADJUSTMENT'));
ALTER TABLE stock_movements ADD CONSTRAINT chk_stock_movements_user_id_not_blank CHECK (btrim(user_id) <> '');
