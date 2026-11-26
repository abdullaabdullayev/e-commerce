-- Demo data: loaded only with the "demo" Spring profile (db/demo location). Idempotent.
-- Product IDs match the product-service demo catalog; order IDs match inventory reservations.
-- The pending order expires with its reservation, after which recovery marks it EXPIRED.
INSERT INTO customer_orders(id,customer_subject,request_fingerprint,status,amount,currency,expires_at,payment_reference) VALUES
 ('30000000-0000-0000-0000-000000000001','student@demo.local','demo-fingerprint-001','CONFIRMED',29.90,'AZN',NULL,'PAY-DEMO-001'),
 ('30000000-0000-0000-0000-000000000002','parent@demo.local','demo-fingerprint-002','PENDING_PAYMENT',49.00,'AZN',now()+interval '10 minutes',NULL) ON CONFLICT DO NOTHING;
INSERT INTO order_items(order_id,product_id,sku,name,unit_price,quantity) VALUES
 ('30000000-0000-0000-0000-000000000001','prod-demo-001','EDU-KIT-001','Riyaziyyat başlanğıc dəsti',29.90,1),
 ('30000000-0000-0000-0000-000000000002','prod-demo-002','EDU-KIT-002','Elm təcrübələri qutusu',24.50,2) ON CONFLICT DO NOTHING;
-- Earlier seeds left this order pending with no deadline, so it could never expire.
UPDATE customer_orders SET expires_at = now()
WHERE id = '30000000-0000-0000-0000-000000000002' AND status = 'PENDING_PAYMENT' AND expires_at IS NULL;
