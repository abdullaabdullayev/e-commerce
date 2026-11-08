CREATE TABLE stock (
    sku varchar(80) PRIMARY KEY,
    on_hand bigint NOT NULL DEFAULT 0 CHECK (on_hand BETWEEN 0 AND 1000000000000),
    reserved bigint NOT NULL DEFAULT 0 CHECK (reserved >= 0 AND reserved <= on_hand)
);
CREATE TABLE stock_receipts (
    id uuid PRIMARY KEY,
    sku varchar(80) NOT NULL REFERENCES stock(sku),
    quantity integer NOT NULL CHECK (quantity > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE reservations (
    id uuid PRIMARY KEY,
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE','COMMITTED','RELEASED','EXPIRED')),
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reservations_expiry ON reservations(expires_at) WHERE status = 'ACTIVE';
CREATE TABLE reservation_items (
    reservation_id uuid NOT NULL REFERENCES reservations(id),
    sku varchar(80) NOT NULL REFERENCES stock(sku),
    quantity integer NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (reservation_id, sku)
);
