-- Reference/demo seed data. Keeps freshly provisioned environments
-- (local dev, preview/staging) non-empty for manual QA and dashboard screenshots.
-- ON CONFLICT DO NOTHING makes this migration safe to design against re-seeding
-- scenarios and keeps it from colliding with data created afterwards by users.

INSERT INTO products (name, sku, description, category, price, quantity, min_stock, status)
VALUES
    ('Laptop Demo 14"', 'SEED-LAP-001', 'Producto semilla para ambiente demo', 'Electronics', 899.99, 25, 5, 'ACTIVE'),
    ('Mouse Inalambrico', 'SEED-MOU-001', 'Producto semilla para ambiente demo', 'Electronics', 19.99, 150, 20, 'ACTIVE'),
    ('Silla Ergonomica', 'SEED-CHR-001', 'Producto semilla para ambiente demo', 'Furniture', 249.50, 3, 5, 'ACTIVE')
ON CONFLICT (sku) DO NOTHING;
