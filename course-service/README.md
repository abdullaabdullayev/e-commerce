# Kodariun course-service — MVP

Java 21, Spring Boot, Spring Data JPA, PostgreSQL/Flyway və məxfi S3 saxlancı.

## Biznes qaydaları

| İstifadəçi | Səlahiyyət |
|---|---|
| ADMIN | Müəllim/tələbə hesabı yaratmaq, kurs yaratmaq, müəllim təyin etmək, tələbə əlavə etmək, sillabus və materialları idarə etmək, xal vermək və düzəltmək |
| TEACHER | Yalnız təyin olunduğu kursların sillabusunu/materiallarını və tələbə siyahısını görmək, həmin kurslarda aktiv tələbələrə xal vermək |
| STUDENT | Öz kurslarını, ümumi xalını və xal tarixçəsini görmək |
| Valideyn | Tələbənin qeydiyyat emaili və şifrəsi ilə STUDENT ekranından istifadə etmək; ayrıca PARENT rolu yoxdur |

Sillabus və materiallar tələbə/valideyn üçün bağlıdır. Kursun yayımlanması bunu
dəyişmir. Müəllim sillabus, dərs və material əlavə edə, redaktə edə və silə bilməz.
Kursla tələbənin əlaqəsi birbaşa Enrollment-dır; MVP-də qrup/release API-si yoxdur.

## Layihə quruluşu

- controller — HTTP, sorğu validasiyası, DTO cavabları.
- dto — giriş/çıxış record-ları və entity → DTO mapper.
- domain — Course, CourseTeacher, Lesson, Enrollment, Material, StudentPoint JPA entity-ləri.
- repository — Spring Data JPA repository-ləri; SQL biznes servislərində yazılmır.
- service — icazələr, tranzaksiyalar, kurs/sillabus, qeydiyyat, xal və material axınları.
- integration — Auth və User servisləri ilə HTTP əlaqəsi.
- security / config — JWT accountId/roles yoxlaması.
- storage — S3 klienti; brauzerə storage açarı və birbaşa URL verilmir.
- exception — sabit HTTP statusları və ProblemDetail cavabları.

Entity-lər controller-dən qaytarılmır. JPA əlaqələri LAZY-dir, open-in-view bağlıdır.
Hibernate ddl-auto=validate istifadə edir; sxemi yalnız Flyway dəyişir.
Başqa servisin istifadəçisi JPA əlaqəsi ilə deyil, sabit accountId ilə istinad edilir.

Kurs üzrə dəyişikliklər PESSIMISTIC_WRITE kilidi altında aparılır. Bu, müəllim
təyinatının ləğvi, qeydiyyat, dərs sırası və eyni vaxtda verilən xalların bir-birinə
zidd yazılmasını önləyir. Sillabusun sıralanmasında V1-dəki PostgreSQL deferred
unique constraint xüsusi repository sorğusu ilə istifadə olunur.

Auth/Profile HTTP sorğuları və S3 fayl ötürülməsi DB tranzaksiyasından kənardadır.
MembershipTransactions və MaterialTransactions bu sərhədi ayrıca saxlayır.
Tələbə xallarının cəmi bazadakı tarixçədən hesablanır; ayrıca dəyişən balans yoxdur.
Xal tarixçəsi və cəmi bir REPEATABLE_READ snapshot-dan oxunur.

## Başlatma

Kök .env-də mövcud DB/JWT/RabbitMQ açarları ilə yanaşı bunlar olmalıdır.
Nümunə: [.env.example](.env.example).

| Dəyişən | Məqsəd |
|---|---|
| AUTH_BOOTSTRAP_KEY | İlk adminin bir dəfə təyin olunması |
| PROFILE_SERVICE_KEY | Course → User daxili sorğuları |
| COURSE_STORAGE_ACCESS_KEY | S3 istifadəçi adı |
| COURSE_STORAGE_SECRET_KEY | S3 məxfi açarı |

Hər məxfi açar üçün ayrı təsadüfi, ən azı 32 baytlıq dəyər seç.

```powershell
docker compose config --quiet
docker compose up -d --build
```

API: http://localhost:8080. Əsas Compose-da tətbiq servislərindən yalnız Gateway
host portu açır. IDE üçün compose.ide.yaml əlavə olunanda infrastruktur portları
yalnız localhost-da açılır; tətbiq servislərinin Docker portları açılmır.

```powershell
docker compose -f docker-compose.yaml -f compose.ide.yaml up -d postgres-auth postgres-user postgres-course rabbitmq course-storage
```

IDE: JDK 21, Gradle reload, CourseServiceApplication. Course DB localhost:5436,
S3 localhost:9000. Tətbiqləri IDE və Docker-dən paralel eyni Eureka qeydiyyatına
qoşma. .env kök və ya modul qovluğundan avtomatik oxunur.

## Admin panelin API-si

Gateway-də baza yolu `/api/v1/admin`-dir. Bütün sorğular `Authorization: Bearer <accessToken>` başlığı və ADMIN rolu tələb edir. Panel siyahıda qaralama, yayımlanmış və arxivlənmiş kursları görür.

| Əməliyyat | Metod və baza yolundan sonrakı ünvan |
|---|---|
| Hesabları siyahıla və ya yarat | `GET /accounts?role=TEACHER&page=0`, `POST /accounts` |
| Hesabı oxu və ya rollarını dəyiş | `GET /accounts/{id}`, `PUT /accounts/{id}/roles` |
| Kursları siyahıla və ya yarat | `GET /courses?page=0&size=20`, `POST /courses` |
| Kursu redaktə et və ya statusunu dəyiş | `PUT /courses/{id}`, `PUT /courses/{id}/status` |
| Müəllim təyin et və ya təyinatı sil | `PUT` və `DELETE /courses/{id}/teachers/{accountId}` |
| Tələbələri siyahıla, kursa yaz və ya qeydiyyat statusunu dəyiş | `GET /courses/{id}/students`, `PUT /courses/{id}/students/{accountId}` |
| Sillabusu oxu və dərs yarat | `GET /courses/{id}/syllabus`, `POST /courses/{id}/lessons` |
| Dərsi redaktə et və ya arxivlə | `PUT` və `DELETE /courses/{id}/lessons/{lessonId}` |
| Dərslərin sırasını dəyiş | `PUT /courses/{id}/lesson-order` |
| Fayl və ya keçid əlavə et | `POST /course-materials/lessons/{lessonId}/files`, `POST /course-materials/lessons/{lessonId}/links` |
| Materialı oxu, arxivlə və ya faylı endir | `GET` və `DELETE /course-materials/{id}`, `GET /course-materials/{id}/download` |
| Xal tarixçəsinə bax və ya xal ver | `GET` və `POST /courses/{id}/students/{studentId}/points` |
| Verilmiş xalı düzəlt | `POST /courses/{id}/students/{studentId}/points/{awardId}/corrections` |

## Admin və hesablar

1. Sahib hesabını POST /api/v1/auth/register ilə yarat və login et.
2. GET /api/v1/auth/me cavabından accountId-ni al.
3. POST /api/v1/auth/bootstrap-admin sorğusunda X-Admin-Bootstrap-Key başlığı və
   {"accountId":1} göndər. Bu yalnız heç bir admin olmadıqda mümkündür.
4. Yenidən login et; yeni accessToken ADMIN rolunu daşıyacaq.
5. İlk admin hazır olduqdan sonra bootstrap açarını silib Auth-u yenidən başlatmaq olar.

Admin POST /api/v1/auth/accounts ilə hesab açır:

```json
{
  "email": "teacher@example.com",
  "password": "<fərdi güclü şifrə>",
  "firstName": "Ad",
  "lastName": "Soyad",
  "roles": ["TEACHER"]
}
```

Tələbə üçün STUDENT rolu verilir. Valideyn həmin tələbənin emaili ilə daxil olur.
Login: POST /api/v1/auth/login, {"email":"...","password":"..."}.
Açıq qeydiyyat yalnız CUSTOMER rolunu verir; istifadəçi özünə müəllim/admin seçmir.
Mövcud hesab rolları PUT /api/v1/auth/accounts/{id}/roles ilə əvəz edilir.
Son admini silmək mümkün deyil. Rol dəyişdikdə yenidən login lazımdır;
əvvəlki JWT digər servislərdə vaxtı bitənədək keçərlidir (standart 15 dəqiqə).

## Kurs və sillabus API-si

Bütün qorunan sorğularda Authorization: Bearer <accessToken> istifadə edilir.

| Method | Yol | İcazə / məzmun |
|---|---|---|
| GET | /api/v1/courses | Açıq yayımlanmış kataloq; page/size |
| GET | /api/v1/courses/{id} | Açıq; yalnız PUBLISHED kurs |
| GET | /api/v1/courses/mine | Admin: bütün kurslar; müəllim: öz kursları |
| POST | /api/v1/courses | Admin; title, description, level, minAge, maxAge |
| PUT | /api/v1/courses/{id} | Admin; eyni kurs sahələri |
| PUT | /api/v1/courses/{id}/status | Admin; {"status":"PUBLISHED"} |
| PUT | /api/v1/courses/{id}/teachers/{accountId} | Admin; body yoxdur, aktiv TEACHER hesabı tələb olunur |
| DELETE | /api/v1/courses/{id}/teachers/{accountId} | Admin; təyinatı deaktiv edir |
| PUT | /api/v1/courses/{id}/students/{accountId} | Admin; {"status":"ACTIVE"}, aktiv STUDENT hesabı tələb olunur |
| GET | /api/v1/courses/{id}/students | Admin / kurs müəllimi; profillərlə 50-lik səhifə |
| GET | /api/v1/courses/{id}/syllabus | Admin / kurs müəllimi |
| POST | /api/v1/courses/{id}/lessons | Yalnız admin |
| PUT | /api/v1/courses/{id}/lessons/{lessonId} | Yalnız admin |
| PUT | /api/v1/courses/{id}/lesson-order | Admin; {"lessonIds":["..."]} |
| DELETE | /api/v1/courses/{id}/lessons/{lessonId} | Admin; arxivləyir |

Dərs sorğusu: title, objective, durationMinutes (1–600), teacherNotes.
Sıralama bütün dərsləri, arxivlənmişlər daxil, bir dəfə əhatə etməlidir.
Kurs statusları DRAFT/PUBLISHED/ARCHIVED, qeydiyyat ACTIVE/COMPLETED/LEFT-dir.
Kurs arxivlənəndə yeni məzmun və xal verilməsi bağlanır; tarixçə saxlanılır.
İlk qeydiyyat ACTIVE olmalıdır; sonradan status dəyişdirilə bilər.
Müəllim təyinatı silinəndə növbəti sorğudan kursa giriş dayanır.

## Tələbə/valideyn və xal API-si

GET /api/v1/courses/my-learning tələbənin öz PUBLISHED/ARCHIVED kurslarını,
qeydiyyat statusunu, müəllim accountId-lərini və kurs üzrə ümumi xalını verir.
Sillabus, teacherNotes və material bu cavaba daxil edilmir.
COMPLETED/LEFT qeydiyyatın xal tarixçəsi saxlanır; yeni xal verilmir.

| Method | Yol | İcazə |
|---|---|---|
| GET | /api/v1/courses/{courseId}/students/me/points | Öz xal tarixçəsi |
| GET | /api/v1/courses/{courseId}/students/{studentId}/points | Admin / təyin edilmiş müəllim / həmin tələbə |
| POST | /api/v1/courses/{courseId}/students/{studentId}/points | Kurs müəllimi və ya admin |
| POST | /api/v1/courses/{courseId}/students/{studentId}/points/{awardId}/corrections | Yalnız admin |

Xal vermək:

```json
{
  "requestId": "3e2e1372-415d-4c59-86c4-38db28b95e68",
  "points": 10,
  "reason": "LED tapşırığını tamamladı"
}
```

Bir mükafat 1–1000 xal ola bilər. Kurs PUBLISHED, qeydiyyat ACTIVE olmalıdır.
İstifadəçi əməliyyatı üçün UUID requestId yarat; şəbəkə xətasında eyni requestId və
eyni body ilə təkrarla. İkinci xal qeydi yaranmır. Eyni requestId-ni fərqli payload,
əməliyyat və ya müəllimlə istifadə etmək 409 qaytarır. Açarda unikal sərhəd Enrollment-dır.

Admin düzəlişi mövcud mükafatın yeni yekun dəyərini bildirir:

```json
{
  "requestId": "2864b2cb-9362-41c0-a371-9d0b910b40ae",
  "correctedPoints": 6,
  "reason": "Əvvəlki qiymətləndirmədə 4 xal artıq yazılıb"
}
```

10 → 6 düzəlişi tarixçəyə -4 yazır. Sonra 6 → 8 düzəlişi +2 yazır.
correctedPoints 0–1000-dir; 0 həmin mükafatı ləğv edir. Hər düzəliş orijinal
AWARD qeydinə istinad edir; əvvəlki tarixçə silinmir. Dəyişikliksiz düzəliş 409 verir.
Admin arxivlənmiş kurs və qeyri-aktiv qeydiyyatda da tarixçəni düzəldə bilər.
Xal reason sahəsi tələbəyə görünür; ora şəxsi müəllim qeydləri yazılmamalıdır.

Xal tarixçəsi {courseId,studentAccountId,totalPoints,entries} qaytarır;
entries və digər səhifələr {content,page,size,hasNext} formatındadır.
size maksimum 100-dür. Profil hələ RabbitMQ vasitəsilə yaranmayıbsa roster
profilePending, User əlçatmazdırsa profilesUnavailable bildirir.

## Materiallar

- POST /api/v1/course-materials/lessons/{lessonId}/files: admin, multipart title + file.
- POST /api/v1/course-materials/lessons/{lessonId}/links: admin, {"title":"...","url":"https://..."}.
- GET /api/v1/course-materials/{id}: admin / kurs müəllimi.
- GET /api/v1/course-materials/{id}/download: admin / kurs müəllimi; Gateway faylı ötürür.
- DELETE /api/v1/course-materials/{id}: admin, arxivləmə.

PDF/PNG/JPEG/PPTX maksimum 25 MB, UTF-8 INO maksimum 1 MB.
PPTX genişlənmiş ölçüsü 100 MB/2000 entry ilə məhdudlaşdırılır. Format yoxlaması
antivirus skanı deyil. Materiallar yalnız müəllim/admin üçündür; visibility API-si yoxdur.
READY fayl arxivləndikdə tarixçə üçün saxlanır. Natamam yükləmələr DB-də qeyd olunur
və lease ilə idarə edilən cleanup işi tərəfindən təmizlənir. Lokal S3 serveri SeaweedFS-dir.

## V1-dən keçid

V1 migration dəyişdirilməyib. V2 yeni course_enrollments və student_points cədvəllərini
yaradır. Qrup qeydiyyatları course + student üzrə birləşdirilir: aktiv qrupda ACTIVE
üstündür, sonra COMPLETED, qalanlar LEFT; ən erkən enrolledAt saxlanır.
Köhnə qrup cədvəlləri tarixçə olaraq qalır, yeni API onlardan istifadə etmir.
Müəllim təyinatları qorunur, əvvəlki EDIT dəyəri artıq redaktə icazəsi vermir.
Bütün materiallar staff-only olur; köhnə STUDENT_VISIBLE dəyərləri çevrilir.
Köhnə /course-groups, /groups, /release və /visibility yolları çıxarılıb.

Frontend müəllim/tələbə ekranları, ayrıca valideyn hesabı, imtahan, sertifikat,
davamiyyət və kurs ödənişi bu backend mərhələsinə daxil deyil.
