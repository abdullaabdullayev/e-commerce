# Kodariun frontend

Next.js App Router, TypeScript və Tailwind CSS ilə lokal frontend.

## İşə salmaq

Node.js 20.9 və ya daha yeni versiya tələb olunur. Repository-də paketlərin
dəqiq versiyaları `pnpm-lock.yaml` ilə saxlanılır.

```powershell
cd frontend
pnpm install
npm run dev
```

Paketlər artıq yüklənibsə yalnız `npm run dev` kifayətdir. Ünvan:
`http://localhost:3000`. Next dev işləyərkən fayl dəyişiklikləri avtomatik yenilənir.

## Səhifələr

| Yol | Məzmun |
|---|---|
| `/` | Ana səhifə |
| `/kurslar` | LEGO, STEAM və Arduino istiqamətləri |
| `/magaza` | Axtarış, kateqoriya filtri və məhsullar |
| `/magaza/[id]` | Məhsul detalı |
| `/haqqimizda` | Kodariun haqqında |
| `/elaqe` | Əlaqə və istifadəçinin Instagram-da paylaşacağı müraciət mətni |
| `/hesab` | Login, qeydiyyat və şəxsi sifarişlər |
| `/sifaris` | Səbət, məlumat sorğusu və canlı rejimdə müvəqqəti rezervasiya |

## Kataloq rejimləri

`.env.example`-dan `frontend/.env.local` yarat. Backend-in kökündəki `.env`
avtomatik olaraq Next.js-ə tətbiq edilmir.

Standart rejim `DEMO_CATALOG=false`-dur: məhsullar, qiymətlər, kateqoriyalar və stok
real backend-dən gəlir. Boş baza boş kataloq kimi göstərilir; nümunə məhsul əlavə edilmir.

İstəyə bağlı `DEMO_CATALOG=true` rejimində Instagram-dan real model şəkilləri və
məzmun göstərilir. Qiymətlər uydurulmur; mövcudluq və qiymət üçün əlaqə təklif edilir.
Bu rejimdə səbət lokal saxlanır, real sifariş yaradılmır.

Backend kataloqu üçün:

```dotenv
GATEWAY_URL=http://127.0.0.1:8080
DEMO_CATALOG=false
```

Environment dəyişəndən sonra dev prosesini yenidən başlat. Canlı rejimdə yalnız
Gateway-ə sorğu gedir; frontend ayrıca Product, Inventory və Order portlarını çağırmır.
Mağaza AZN kataloqunu backend-in `content` + `page` cavabına uyğun səhifələmə ilə oxuyur.
Backend əlçatan deyilsə real xətanı
göstərir, nümunə məhsullara səssiz keçid etmir.

Login və qeydiyyat da Gateway üzərindən Auth Service-ə bağlıdır. Access token
HttpOnly, SameSite=Lax cookie-də saxlanır, JavaScript və localStorage-a çıxarılmır.
Token bitəndə yenidən login lazımdır; backend refresh endpoint-i hələ inteqrasiya olunmayıb.
Çıxış həmin brauzerdə cookie-ni silir; backend tokenini serverdə revoke etmir.
Production HTTPS reverse proxy arxasında `COOKIE_SECURE=true` təyin et.

Frontend `/api/backend/` proxy-si yalnız kataloq, stok və şəxsi sifariş yollarına
icazə verir. `/internal/` və admin açarları brauzerə ötürülmür. İdarəetmə paneli bu
frontend mərhələsinə daxil deyil. Məhsul detalında stok hər 15 saniyədən bir,
açıq sifarişlər isə hesab səhifəsində hər 10 saniyədən bir yenilənir.
Sifarişlər backend-in 20 elementlik səhifələrinə uyğun göstərilir.
Nümunə və canlı kataloqun səbətləri ayrıca saxlanır.

## Backend əlaqələri

| Ekran / əməliyyat | Gateway API |
|---|---|
| Qeydiyyat, giriş | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` |
| Mağaza, axtarış, sıralama | `GET /api/v1/products?q=&categoryId=&page=&size=&sort=&currency=AZN` |
| Kateqoriyalar | `GET /api/v1/categories` |
| Məhsul detalı | `GET /api/v1/products/{id}` |
| Satışa açıq say | `GET /api/v1/stock/{sku}` → `available` |
| Rezervasiya | `POST /api/v1/orders` + `Idempotency-Key` |
| Şəxsi sifarişlər | `GET /api/v1/orders?page=0` → 20 elementlik siyahı |
| Rezervasiyanı ləğv etmək | `POST /api/v1/orders/{id}/cancel` |

Sifarişə yalnız məhsul ID-si və say göndərilir. Qiyməti və yekun məbləği Order
servisi müəyyən edir. Eyni sorğunun şəbəkə xətasından sonra təkrarı eyni idempotency
açarını istifadə edir. Cavab alındıqdan sonra növbəti alış yeni açarla yaradılır.
Qeydiyyat/login backend-də yoxlanılır: yanlış giriş `401`, mövcud email `409`,
yanlış məlumat `400` qaytarır. UI bu statusları Azərbaycan dilində göstərir.

## Ödəniş sərhədi

Onlayn ödəniş provayderi hələ backend-ə əlavə edilməyib. Canlı rejimdə düymə
yalnız **5 dəqiqəlik rezervasiya** yaradır və bunu açıq göstərir. Kart məlumatı
toplanmır, pul çəkilmir və sifariş “ödənilib” kimi göstərilmir. Çatdırılma haqqı,
kupon və ayrıca vergi hesablanması UI-da təqdim edilmir.

Əlaqə forması müraciət mətnini istifadəçi üçün hazırlayır/kopyalayır. Avtomatik
email, Instagram mesajı və ya kurs qeydiyyatı göndərmir. Kursların cədvəli və
qiymətləri dəqiq məlumat gələnədək dərc edilmir.

## Brend materialları

Şəkillər Kodariun-un açıq Instagram səhifələrindən götürülüb. Mənbə və
əlaqə məlumatlarının hansı posterdən oxunduğu `public/brand/SOURCES.md` faylındadır.
Köhnə `fire-truck.jpg` fayl adı mənbə nüsxəsində saxlanıb, amma təsvir olunan
məhsul düzgün olaraq “Yarış maşını” adı ilə göstərilir.

## Yoxlama

`pnpm typecheck` yalnız TypeScript yoxlaması edir. Bu mərhələdə backend-in
avtomatik testləri işlədilməyib. Mövcud dev prosesində əsas səhifələrin HTTP
cavabları və desktop/mobil görünüşü yoxlanılıb; production build ayrıca mərhələdir.
