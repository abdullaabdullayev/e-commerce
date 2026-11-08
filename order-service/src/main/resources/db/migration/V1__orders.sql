CREATE TABLE customer_orders (
    id uuid PRIMARY KEY,
    customer_subject varchar(320) NOT NULL,
    request_fingerprint varchar(64) NOT NULL,
    status varchar(32) NOT NULL CHECK (status IN ('RESERVING','PENDING_PAYMENT','CONFIRMING','CONFIRMED','CANCELLING','CANCELLED','EXPIRED','REJECTED','PAYMENT_REVIEW')),
    amount numeric(24,4) NOT NULL CHECK (amount > 0),
    currency varchar(3) NOT NULL,
    expires_at timestamptz,
    payment_reference varchar(200) UNIQUE,
    cancel_as varchar(20),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_orders_customer ON customer_orders(customer_subject, created_at DESC);
CREATE INDEX idx_orders_work ON customer_orders(status, updated_at);
CREATE TABLE order_items (
    order_id uuid NOT NULL REFERENCES customer_orders(id),
    product_id varchar(100) NOT NULL,
    sku varchar(80) NOT NULL,
    name varchar(200) NOT NULL,
    unit_price numeric(16,4) NOT NULL CHECK (unit_price > 0),
    quantity integer NOT NULL CHECK (quantity BETWEEN 1 AND 1000),
    PRIMARY KEY (order_id, product_id)
);
