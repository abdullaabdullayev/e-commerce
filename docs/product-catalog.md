# Məhsul satışının birinci mərhələsi: kataloq

Bu mərhələ Product Service-in kataloq məntiqini tamamlayır.
Stok rezervasiyası və sifariş emalı ayrıca Inventory və Order servislərində qurulacaq.

## Qaydalar

- Məhsul mövcud kateqoriyaya bağlanmalıdır.
- SKU unikal, registrə həssas və yaradıldıqdan sonra sabitdir.
  SKU hərf/rəqəmlə başlayır; hərf, rəqəm, nöqtə, alt xətt və tire qəbul edir.
- Qiymət müsbətdir, maksimum 12 tam rəqəm saxlayır. ISO valyuta kodu böyük hərfə
  çevrilir; valyutanın kiçik pul vahidinə uyğun olmayan qiymət yuvarlaqlaşdırılmır,
  sorğu rədd edilir. Məsələn, AZN üçün `12.50` qəbul edilir, `12.501` edilmir.
- Qiymət filtri və qiymətə görə sıralama valyuta tələb edir.
- Məhsul yeniləmə əməliyyatı SKU, yaradılma tarixi və aktivliyi dəyişmir.
  Redaktə olunan sahələr tam əvəz edilir; göndərilməyən optional sahələr təmizlənir.
- Silmə məhsulu arxivləyir (`active=false`). İctimai siyahı və detallarda görünmür.
  Arxivdən bərpa mümkündür; fiziki silinmədiyinə görə gələcək sifariş istinadları saxlanır.
- Kateqoriya slug-u unikal və kiçik ASCII hərf/rəqəm/tire formasındadır.
  Ana kateqoriya yalnız yaradılarkən təyin olunur və mövcud olmalıdır.
  Parent dəyişdirmə və kateqoriya silmə əməliyyatı bu mərhələdə təqdim edilmir.
- Şəkillər URL kimi saxlanır; yalnız host-u olan HTTP/HTTPS ünvanları qəbul edilir.
- Xüsusiyyətlər iç-içə JSON ola bilər; ölçü və dərinlik limitləri tətbiq edilir.
  MongoDB xüsusi sahə adlarının qarşısını almaq üçün nöqtə və `$` ilə başlayan açarlar qəbul edilmir.
- Axtarış ad və SKU daxilində literal, registrdən asılı olmayan uyğunluqdur.
  Kateqoriya filtri yalnız verilən kateqoriyaya aiddir, alt kateqoriyaları genişləndirmir.
- Siyahılar səhifələnir, səhifədə maksimum 100 qeyd qaytarılır.
  Sıralama sahələri: `name`, `sku`, `price`, `createdAt`, `updatedAt`, `id`.

## Giriş

Oxuma əməliyyatları ictimaidir. Məhsul və kateqoriya dəyişiklikləri üçün
Product Service başladılmadan əvvəl `CATALOG_ADMIN_KEY` environment dəyişənini təyin et.
Sorğuda həmin dəyəri `X-Catalog-Admin-Key` header-ində göndər.

Açar təyin edilməyibsə idarəetmə əməliyyatları `503`, səhv açar olduqda `401` qaytarır.
Bu açar backend/operator üçündür; ictimai frontend koduna yerləşdirilməməlidir.
Auth Service-də staff rolları hazır olanda bu giriş mexanizmi rollara bağlanacaq.

## Endpoint-lər

| Metod | Yol | Məqsəd |
|---|---|---|
| GET | `/api/v1/categories` | Səhifələnmiş kateqoriyalar |
| GET | `/api/v1/categories/{id}` | Kateqoriya detalı |
| POST | `/api/v1/categories` | Kateqoriya yarat |
| PUT | `/api/v1/categories/{id}` | Kateqoriya adı və slug-u yenilə |
| GET | `/api/v1/products` | Aktiv məhsulları axtar |
| GET | `/api/v1/products/{id}` | Aktiv məhsulun detalı |
| POST | `/api/v1/products` | Məhsul yarat |
| PUT | `/api/v1/products/{id}` | Məhsul məlumatlarını yenilə |
| DELETE | `/api/v1/products/{id}` | Məhsulu arxivlə |
| POST | `/api/v1/products/{id}/activate` | Məhsulu yenidən aktivləşdir |
| GET | `/internal/v1/catalog/products` | Aktiv və arxiv məhsullarını göstər |
| GET | `/internal/v1/catalog/products/{id}` | Arxiv daxil olmaqla məhsul detalı |

`/internal/` sorğuları da admin açarı tələb edir və Gateway vasitəsilə açılmır.
Kateqoriya və məhsulların ictimai yolları Gateway-dən yönləndirilir.

Filtr nümunəsi:

```text
GET /api/v1/products?q=arduino&currency=AZN&minPrice=10&maxPrice=100&sort=price,asc&page=0&size=20
```

Digər filtrlər: `categoryId`, `brandId`.
Xəta cavabları ProblemDetail formatındadır; DTO validasiya xətalarında `errors`
sahəsi konkret sahə və səbəbi göstərir. Təkrar SKU/slug `409`, olmayan məhsul və
kateqoriya `404`, səhv məlumat `400` qaytarır.

MongoDB-də qiymətlər üçün mövcud DECIMAL128 konfiqurasiyası istifadə edilir.
Arxivləmə və yeniləmə bir sənəd üzərində atomik əməliyyatlardır. Eyni sahəyə
paralel admin dəyişikliklərində son yazı qüvvəyə minir; bu mərhələdə versiya konflikti API-si yoxdur.
