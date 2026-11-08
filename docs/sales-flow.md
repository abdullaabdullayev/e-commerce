# Satış axını: stok və sifariş

## Servislər

| Servis | Port | Baza | Məsuliyyət |
|---|---|---|---|
| product-service | 8083 | MongoDB product_db | Kataloq və məhsul qiymətlərinin əsas mənbəyi |
| inventory-service | 8084 | PostgreSQL inventory_db (daxili 5432) | Satış stoku, mədaxil, rezervasiya |
| order-service | 8086 | PostgreSQL order_db (daxili 5432) | Qiymət surətləri, sifariş və kompensasiya axını |

Portlar konteyner daxilidir; hostdan yalnız Gateway-in 8080 portu açıqdır.
Başlatma və `.env` konfiqurasiyası: [Gateway girişi](gateway-access.md).

Sifarişlər yalnız onlayn kart ödənişi modelinə görə hazırlanıb. Provayder hələ
seçilməyib; kartdan pul çəkmək, ödəniş sessiyası/redirect yaratmaq və webhook
imzasını yoxlamaq sonrakı Payment Service inteqrasiyasında əlavə olunacaq.
Müştəri üçün public "paid" endpoint yoxdur.

Bu ilk sifariş müqaviləsi məhsul məbləğlərinin cəmini saxlayır. Çatdırılma ünvanı,
çatdırılma haqqı, kupon, ayrıca vergi hesablanması, göndəriş və fiziki qaytarılma
axınları bu API-yə daxil edilməyib. Onları yekun checkout məbləğinə əlavə etmədən
production ödənişinə keçmək olmaz. Kurs alışı da bu məhsul sifarişinə hələ qoşulmayıb.

## İşə salma

`.env` dəyişənlərini konfiqurasiya etdikdən sonra bazaları mövcud volume-lara toxunmadan başlat:

```powershell
docker compose up -d postgres-inventory postgres-order
```

Mövcud `DB_USERNAME`, `DB_PASSWORD` dəyişənləri yeni bazaların Compose
konfiqurasiyasına uyğun olmalıdır. `SECRET_KEY` Auth Service ilə eyni dəyərdir.
Order Service hazırkı Auth tokeninin email olan `sub` sahəsini sahiblik üçün istifadə edir.
Email dəyişdirmə əlavə olunarkən bu identifikator account ID-yə ayrıca migration ilə keçirilməlidir.

Əlavə konfiqurasiya:

| Dəyişən | Servis | Məqsəd |
|---|---|---|
| CATALOG_ADMIN_KEY | Product | Kataloq idarəetməsi |
| INVENTORY_SERVICE_KEY | Inventory və Order | Stok üzrə daxili əməliyyatlar |
| ORDER_SERVICE_KEY | Order, gələcək Payment | Ödəniş nəticəsinin daxili təsdiqi |
| INVENTORY_DB_URL | Inventory | Opsional JDBC URL |
| ORDER_DB_URL | Order | Opsional JDBC URL |
| PRODUCT_SERVICE_URL | Order | Default http://localhost:8083 |
| INVENTORY_SERVICE_URL | Order | Default http://localhost:8084 |

Açarları environment vasitəsilə ver; frontend koduna yerləşdirmə.
Flyway yeni bazalarda cədvəlləri yaradır. Inventory və Order servisləri başqa
servisin bazasına qoşulmur. Daxili endpoint-lər Gateway-dən açılmır.

## Stok

`onHand` satış uçotundakı stokdur, `reserved` müvəqqəti ayrılmış hissədir,
`available = onHand - reserved`. Uğurlu ödənişin təsdiqində satılmış say satış
stokundan çıxılır. Fiziki anbarın göndərişə qədər ayrılmış malları ayrıca izləməsi
üçün sonradan fulfillment/allocated uçotu əlavə edilməlidir.

Mədaxil sorğusu:

```text
POST /internal/v1/inventory/receipts
X-Inventory-Service-Key: <daxili açar>

{"id":"<unikal UUID>","sku":"ARDUINO-KIT-01","quantity":10}
```

Eyni UUID və eyni məlumat təkrar mədaxil yaratmır. Eyni UUID ilə fərqli məlumat `409` verir.
Mədaxil edən operator kataloqdakı dəqiq SKU-dan istifadə etməlidir; SKU-lar
servislər arasında foreign key deyil. Stok servisi məhsul qiymətini saxlamır.

| Metod | Yol | Qayda |
|---|---|---|
| GET | /api/v1/stock/{sku} | İctimai mövcudluq; tanınmayan SKU üçün sıfır |
| POST | /internal/v1/inventory/receipts | İdempotent mədaxil |
| POST | /internal/v1/inventory/reservations | Bir səbətin bütün SKU-larını vahid transaction-da rezerv et |
| GET | /internal/v1/inventory/reservations/{id} | Rezervasiya vəziyyəti |
| POST | /internal/v1/inventory/reservations/{id}/commit | Satışa çevir; yalnız ACTIVE və vaxtı bitməmiş rezervasiya |
| POST | /internal/v1/inventory/reservations/{id}/release | Rezervasiyanı burax; təkrar çağırış təhlükəsizdir |

Rezervasiya ID-si sifariş ID-sidir. Eyni ID fərqli məhsullarla istifadə edilə bilməz.
Təkrar sorğu rezervasiya vaxtını uzatmır. Standart müddət 300 saniyədir;
`inventory.reservation-seconds` ilə 1–3600 arasında dəyişə bilər.
Expiry prosesi saniyədə bir dəfə maksimum 100 rezervasiyanı buraxır; yük altında
buraxılma vaxtı gecikə bilər. Stok çatmırsa bütün səbətin rezervasiyası geri qaytarılır.
Satılmış rezervasiya adi ləğvlə geri açıla bilməz; ayrıca qaytarılma prosesi lazımdır.

## Sifariş

Səbətə məhsul əlavə etmək üçün order endpoint çağırma. Sifariş yaratma sorğusu
yalnız müştəri ödənişə keçəndə göndərilməlidir; o zaman rezervasiya başlayır.

```text
POST /api/v1/orders
Authorization: Bearer <access token>
Idempotency-Key: <unikal UUID>

{"items":[{"productId":"<MongoDB məhsul ID-si>","quantity":2}]}
```

Qiymət və müştəri ID-si sorğu body-sindən qəbul edilmir. Məhsul qiyməti Product
Service-dən, sahiblik isə yoxlanmış JWT-dən götürülür. Ad, SKU, vahid qiymət və
valyuta sifarişə köçürülür; kataloq sonradan dəyişəndə sifariş dəyişmir.
Müxtəlif valyutalı məhsullar bir sifarişdə qəbul edilmir.

Eyni istifadəçi və Idempotency-Key üçün eyni məhsullar əvvəlki sifarişi qaytarır;
fərqli məhsullar `409` verir. Təkrar sifariş üçün yeni açar lazımdır.
`201` yalnız sifariş qeydinin yarandığını bildirir, satışın/ödənişin təsdiqi deyil.

| Metod | Yol | Məqsəd |
|---|---|---|
| POST | /api/v1/orders | Məhsul qiymətlərini götür və rezervasiya başlat |
| GET | /api/v1/orders?page=0 | Yalnız öz sifarişlərin; səhifədə 20 |
| GET | /api/v1/orders/{id} | Yalnız öz sifarişinin detalı |
| POST | /api/v1/orders/{id}/cancel | Ödənilməmiş sifarişin rezervasiyasını burax |

Vəziyyətlər:

```text
RESERVING → PENDING_PAYMENT → CONFIRMING → CONFIRMED
     └──→ REJECTED
RESERVING / PENDING_PAYMENT → CANCELLING → CANCELLED / EXPIRED
Gecikmiş və ya ziddiyyətli ödəniş → PAYMENT_REVIEW
```

Order öz bazasında niyyəti saxladıqdan sonra Inventory-yə HTTP sorğusu göndərir.
Şəbəkə sorğusu zamanı DB transaction açıq qalmır. Yarımçıq RESERVING, CONFIRMING
və CANCELLING əməliyyatları arxa planda təkrar edilir. Recovery prosesi hər
2 saniyədə 50 sifariş götürür; daha yüksək yükdə worker planlaması genişləndirilməlidir.
Ödənişə keçid və ləğv üçün şərtli status yeniləmələri istifadə olunur.

## Gələcək Payment Service müqaviləsi

```text
POST /internal/v1/orders/{id}/payment-confirmed
X-Order-Service-Key: <daxili açar>

{"reference":"<provider payment ID>","amount":150.00,"currency":"AZN"}
```

Bu sorğunu yalnız provayder imzasını və ödəniş statusunu yoxlamış etibarlı backend
göndərməlidir. Hazırda provayder inteqrasiyası yoxdur; bu endpoint kart ödənişini
özü yoxlamır və pul çəkmir. Reference unikal olmalıdır, məbləğ və valyuta sifarişlə
uyğun gəlməlidir. Vaxtı bitmiş/ləğv edilmiş sifarişdə ödəniş təsdiqlənərsə
PAYMENT_REVIEW saxlanır. Bu status ayrıca araşdırma və gələcək refund inteqrasiyası tələb edir;
avtomatik geri ödəniş hazırda yoxdur.

Bu mərhələdə biznes kodu kompilyasiya edilir; istifadəçinin göstərişinə görə
testlər işə salınmır. Yeni servislərin runtime və inteqrasiya yoxlaması ayrıca qalır.
