-- Demo data: loaded only with the "demo" Spring profile (db/demo location). Idempotent.
INSERT INTO courses(id,title,description,level,min_age,max_age,status) VALUES
 ('40000000-0000-0000-0000-000000000001','Rəqəmsal yaradıcılıq','Uşaqlar üçün dizayn və kodlaşdırmaya giriş.','BEGINNER',8,12,'PUBLISHED'),
 ('40000000-0000-0000-0000-000000000002','Kiçik alimlər','Gündəlik həyatdan maraqlı elm təcrübələri.','BEGINNER',7,11,'DRAFT') ON CONFLICT DO NOTHING;
INSERT INTO course_teachers(course_id,account_id,permission) VALUES
 ('40000000-0000-0000-0000-000000000001',2,'EDIT'),('40000000-0000-0000-0000-000000000002',2,'EDIT') ON CONFLICT DO NOTHING;
-- lessons(course_id, position) is DEFERRABLE, which ON CONFLICT cannot use as an arbiter: target the primary key.
INSERT INTO lessons(id,course_id,position,title,objective,duration_minutes,teacher_notes) VALUES
 ('41000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001',1,'Rənglər və formalar','Əsas vizual elementləri tanımaq',45,'Nümunə materiallardan istifadə et.'),
 ('41000000-0000-0000-0000-000000000002','40000000-0000-0000-0000-000000000001',2,'İlk animasiya','Sadə hərəkətli səhnə hazırlamaq',60,'Şagirdləri sərbəst sınağa həvəsləndir.'),
 ('41000000-0000-0000-0000-000000000003','40000000-0000-0000-0000-000000000002',1,'Suyun dövranı','Buxarlanma və kondensasiyanı anlamaq',40,'Təhlükəsizlik qaydalarını xatırlat.') ON CONFLICT (id) DO NOTHING;
INSERT INTO course_groups(id,course_id,teacher_account_id,name) VALUES
 ('42000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001',2,'Bazar ertəsi qrupu'),
 ('42000000-0000-0000-0000-000000000002','40000000-0000-0000-0000-000000000002',2,'Elm qrupu') ON CONFLICT DO NOTHING;
INSERT INTO enrollments(group_id,student_account_id,status) VALUES
 ('42000000-0000-0000-0000-000000000001',3,'ACTIVE'),('42000000-0000-0000-0000-000000000002',3,'ACTIVE') ON CONFLICT DO NOTHING;
INSERT INTO group_lessons(group_id,lesson_id,course_id,opened,opened_at) VALUES
 ('42000000-0000-0000-0000-000000000001','41000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001',true,now()),
 ('42000000-0000-0000-0000-000000000001','41000000-0000-0000-0000-000000000002','40000000-0000-0000-0000-000000000001',false,NULL),
 ('42000000-0000-0000-0000-000000000002','41000000-0000-0000-0000-000000000003','40000000-0000-0000-0000-000000000002',false,NULL) ON CONFLICT DO NOTHING;
-- Links, not files: demo files would not exist in object storage and downloads would fail.
INSERT INTO materials(id,lesson_id,uploader_account_id,title,kind,state,url) VALUES
 ('43000000-0000-0000-0000-000000000001','41000000-0000-0000-0000-000000000001',2,'Rəng palitrası','LINK','READY','https://example.org/demo/palette'),
 ('43000000-0000-0000-0000-000000000002','41000000-0000-0000-0000-000000000003',2,'Təcrübə vərəqi','LINK','READY','https://example.org/demo/science') ON CONFLICT DO NOTHING;
INSERT INTO course_enrollments(id,course_id,student_account_id,status,enrolled_at) VALUES
 ('44000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001',3,'ACTIVE',now()-interval '30 days'),
 ('44000000-0000-0000-0000-000000000002','40000000-0000-0000-0000-000000000002',3,'ACTIVE',now()-interval '30 days') ON CONFLICT DO NOTHING;
INSERT INTO student_points(id,enrollment_id,request_id,type,points,requested_points,reason,awarded_by_account_id,created_at) VALUES
 ('45000000-0000-0000-0000-000000000001','44000000-0000-0000-0000-000000000001','46000000-0000-0000-0000-000000000001','AWARD',10,10,'Dərsdə fəal iştirak',2,now()) ON CONFLICT DO NOTHING;
-- Course ...0002 is a DRAFT: no points, sessions or reports, as the API itself would refuse them.
INSERT INTO lesson_sessions(id,lesson_id,teacher_account_id,held_at,topic) VALUES
 ('47000000-0000-0000-0000-000000000001','41000000-0000-0000-0000-000000000001',2,now()-interval '2 days','Rəng nəzəriyyəsi') ON CONFLICT DO NOTHING;
INSERT INTO student_lesson_reports(id,session_id,enrollment_id,attended,learned,participation,recommendation,published_at) VALUES
 ('48000000-0000-0000-0000-000000000001','47000000-0000-0000-0000-000000000001','44000000-0000-0000-0000-000000000001',true,'Rəng çarxını öyrəndi','Qrup işində fəal idi','Kompozisiya məşqlərini davam etdirsin',now()) ON CONFLICT DO NOTHING;
