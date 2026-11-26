-- Demo data: loaded only with the "demo" Spring profile (db/demo location). Idempotent.
-- Reservation IDs equal the order IDs in the order-service demo seed, as real checkouts do.
INSERT INTO stock(sku,on_hand,reserved) VALUES ('EDU-KIT-001',99,0),('EDU-KIT-002',60,0),('EDU-KIT-003',25,0) ON CONFLICT DO NOTHING;
INSERT INTO stock_receipts(id,sku,quantity) VALUES
 ('10000000-0000-0000-0000-000000000001','EDU-KIT-001',100),
 ('10000000-0000-0000-0000-000000000002','EDU-KIT-002',60),
 ('10000000-0000-0000-0000-000000000003','EDU-KIT-003',25) ON CONFLICT DO NOTHING;
-- Order ...0001 was paid (stock already deducted); order ...0002 still holds stock awaiting payment.
INSERT INTO reservations(id,status,expires_at) VALUES
 ('30000000-0000-0000-0000-000000000001','COMMITTED',now()),
 ('30000000-0000-0000-0000-000000000002','ACTIVE',now()+interval '10 minutes') ON CONFLICT DO NOTHING;
INSERT INTO reservation_items(reservation_id,sku,quantity) VALUES
 ('30000000-0000-0000-0000-000000000001','EDU-KIT-001',1),
 ('30000000-0000-0000-0000-000000000002','EDU-KIT-002',2) ON CONFLICT DO NOTHING;

-- Keep the invariant "reserved = sum of active reservations" for the demo SKUs, also in
-- databases seeded by earlier versions (whose stock rows the inserts above leave untouched).
UPDATE stock s SET reserved = COALESCE((
    SELECT sum(i.quantity) FROM reservation_items i JOIN reservations r ON r.id = i.reservation_id
    WHERE i.sku = s.sku AND r.status = 'ACTIVE'), 0)
WHERE s.sku IN ('EDU-KIT-001','EDU-KIT-002','EDU-KIT-003');
