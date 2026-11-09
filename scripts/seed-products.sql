-- Invoked by seed-products.sh with a validated positive integer psql variable.
INSERT INTO products (name, description, category, price, stock_quantity)
SELECT
    'Sample Product ' || n,
    'Generated catalog item ' || n || ' for local API testing.',
    CASE (n % 5)
        WHEN 0 THEN 'shoes'
        WHEN 1 THEN 'clothing'
        WHEN 2 THEN 'accessories'
        WHEN 3 THEN 'equipment'
        ELSE 'sportswear'
    END,
    (((n * 7919) % 50000) + 100) / 100.0,
    (n * 37) % 501
FROM generate_series(1, :product_count) AS n;
