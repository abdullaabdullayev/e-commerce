# Xarici giriş yalnız Gateway vasitəsilə

Əsas deployment `docker-compose.yaml` ilə idarə olunur. Gateway-dən başqa heç bir
konteynerin portu hosta publish edilmir. Bu məhdudiyyət Docker şəbəkəsində tətbiq olunur.
Header yoxlaması şəbəkə izolyasiyasını əvəz etmir.

```text
Brauzer / Postman → localhost:8080 → Gateway → daxili servislər
                                          → Auth, User, Product, Inventory, Order
```

## Şəbəkə

- `edge`: yalnız Gateway qoşulur; host portu standart olaraq `127.0.0.1:8080`-dir.
- `backend`: `internal: true`; bütün tətbiqlər, Eureka, bazalar və RabbitMQ buradadır.
- Gateway hər iki şəbəkəyə qoşulur. Başqa servislərdə `ports:` yoxdur.
- `8081`, `8083`, `8084`, `8085`, `8086`, `8761`, PostgreSQL, MongoDB və RabbitMQ
  portları Compose tərəfindən hosta açılmır.
- Servislər Docker DNS adları ilə əlaqə saxlayır: məsələn,
  `http://inventory-service:8084`, `http://discovery-service:8761/eureka/`.
- Backend servislərin internetə birbaşa çıxışı da bağlıdır. Gələcək ödəniş
  inteqrasiyası üçün yalnız ehtiyacı olan servisə nəzarətli çıxış əlavə edilməlidir.

`/internal/**` üçün Gateway marşrutu yoxdur. Inventory/Order daxili açarları və
kataloq admin açarı əlavə giriş nəzarəti kimi saxlanır. Admin əməliyyatları üçün
ictimai kataloq yolları Gateway üzərindən müvafiq admin açarı ilə çağırıla bilər.

## Başlatma

`.env` faylında `.env.example`-da göstərilən dəyişənləri konfiqurasiya et.
İlk dəfə faylı yaratmaq üçün mövcud `.env`-ni əvəz etmədən nümunəni kopyala və
boş secret sahələrini ayrıca təsadüfi dəyərlərlə doldur. SECRET_KEY ən azı 32 bayt olsun.
Əvvəlki PostgreSQL/RabbitMQ volume-ları varsa, giriş məlumatları onlara uyğun qalmalıdır;
environment dəyişmək mövcud bazanın parolunu dəyişmir.

```powershell
docker compose config --quiet
docker compose up -d --build
```

Dockerfile bütün tətbiqlərin bootJar fayllarını JDK 21 ilə hazırlayır; bu build
əmri test task-larını çağırmır. İlk build dependency və image endirmələri tələb edə bilər.
Servislər Eureka-da qeydiyyatdan keçənədək Gateway ilk sorğularda `503` qaytara bilər.

İstifadə edilən ünvanlar:

```text
POST http://localhost:8080/api/v1/auth/register
POST http://localhost:8080/api/v1/auth/login
GET  http://localhost:8080/api/v1/products
GET  http://localhost:8080/api/v1/categories
GET  http://localhost:8080/api/v1/stock/ARDUINO-KIT-01
GET  http://localhost:8080/api/v1/orders       (Bearer token tələb edir)
```

LAN/server girişini açmaq lazım olduqda `.env` daxilində yalnız
`GATEWAY_BIND_ADDRESS=0.0.0.0` dəyişdirilir. Digər servislərin portları açılmır.

## Tətbiq sərhədi

Bu qayda Compose ilə başlayan konteynerlər üçündür. Əvvəldən IDE/bootRun ilə
hostda işləyən tətbiqlər ayrıca proseslərdir; Docker konfiqurasiyası onların
listener-lərini bağlamır. Bu deployment-ə keçərkən həmin prosesləri dayandır.
Yeni konfiqurasiya mövcud konteynerlərə `docker compose up -d --build` ilə tətbiq olunur.
Docker administratoru və ya backend şəbəkəsinə qoşulma səlahiyyəti olan şəxs
daxili servislərə daxil ola bilər; bu, host administratorunu məhdudlaşdıran mexanizm deyil.

Volume-ların adları saxlanılıb. Keçid üçün `docker compose down -v` işlətmə;
bu, bazaların məlumatlarını silər. İnfrastrukturun idarə edilməsi üçün
`docker compose exec` istifadə edilə bilər; DB və idarəetmə portlarını publish etmək tələb olunmur.

Şəbəkə və publish qaydaları: [Docker Compose networking](https://docs.docker.com/compose/how-tos/networking/).
