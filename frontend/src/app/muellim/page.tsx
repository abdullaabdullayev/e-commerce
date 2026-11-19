import Link from "next/link";
import { redirect } from "next/navigation";
import { accountSession } from "@/lib/admin-session";
import { learningRequest, lessonDate, pageNumber, uuid, type LessonReport, type Slice } from "@/lib/learning";
import { NewSession, ReportEditor } from "./forms";

export const metadata = { title: "Dərs hesabatları", robots: { index: false } };
type Query = { course?: string; session?: string; student?: string; page?: string };
type Syllabus = {course: {title: string; status: string}; lessons: {lesson: {id: string; title: string; archived: boolean}}[]};
type Session = {id: string; heldAt: string; topic: string; teacherAccountId: number};
type Entry = {enrollment: {studentAccountId: number; status: string}; profile?: {firstName: string; lastName: string}};

function Pages({base, page, next}: {base: string; page: number; next: boolean}) {
  return <nav className="flex gap-6 my-5">{page > 0 && <Link href={base + "page=" + (page-1)}>← Əvvəlki</Link>}
    {next && <Link href={base + "page=" + (page+1)}>Sonrakı →</Link>}</nav>;
}

export default async function TeacherPage({searchParams}: {searchParams: Promise<Query>}) {
  const account = await accountSession();
  if (account.status === 401) redirect("/hesab?next=/muellim");
  if (account.status !== 200 || !account.roles.includes("TEACHER")) return <main className="shell section"><p>{account.status === 503 ? "Hesab xidməti əlçatan deyil." : "Müəllim hesabı ilə daxil olun."}</p></main>;
  const query = await searchParams, page = pageNumber(query.page);
  const read = <T,>(path: string) => learningRequest<T>(account.token, path);
  let content;
  if (!query.course || !uuid.test(query.course)) {
    const courses = await read<Slice<{id: string; title: string}>>("courses/mine?page=" + page);
    content = <><h2>Kurslarım</h2>{courses.content.map(c => <p key={c.id}><Link className="text-link" href={"/muellim?course=" + c.id}>{c.title} ↗</Link></p>)}
      {!courses.content.length && <p>Bu səhifədə kurs yoxdur.</p>}<Pages base="/muellim?" page={page} next={courses.hasNext}/></>;
  } else {
    const course = query.course, base = "courses/" + course, url = "/muellim?course=" + course;
    if (!query.session || !uuid.test(query.session)) {
      const [syllabus, sessions] = await Promise.all([read<Syllabus>(base + "/syllabus"), read<Slice<Session>>(base + "/sessions?page=" + page)]);
      content = <><h2>{syllabus.course.title}</h2>{syllabus.course.status === "PUBLISHED" &&
        <NewSession course={course} lessons={syllabus.lessons.map(l => l.lesson).filter(l => !l.archived)}/>}
        <h3>Keçirilmiş dərslər</h3>{sessions.content.map(s => <p key={s.id}><Link className="text-link" href={url + "&session=" + s.id}>{s.topic} · {lessonDate(s.heldAt)}</Link></p>)}
        {!sessions.content.length && <p>Bu səhifədə dərs yoxdur.</p>}<Pages base={url+"&"} page={page} next={sessions.hasNext}/></>;
    } else {
      const session = query.session, sessionUrl = url + "&session=" + session;
      if (query.student && /^[1-9]\d*$/.test(query.student)) {
        const draft = await read<LessonReport | undefined>(base + "/sessions/" + session + "/reports/" + query.student);
        content = <><Link href={sessionUrl}>← Tələbələr</Link><h2>Tələbə #{query.student}</h2>
          <ReportEditor key={session + query.student} course={course} session={session} student={query.student} draft={draft}/></>;
      } else {
        const roster = await read<Slice<Entry>>(base + "/students?page=" + page);
        content = <><Link href={url}>← Keçirilmiş dərslər</Link><h2>Tələbəni seç</h2>{roster.content.map(({enrollment, profile}) =>
          <p key={enrollment.studentAccountId}><Link className="text-link" href={sessionUrl + "&student=" + enrollment.studentAccountId}>
            {profile ? profile.firstName + " " + profile.lastName : "Tələbə #" + enrollment.studentAccountId}</Link></p>)}
          {!roster.content.length && <p>Bu səhifədə tələbə yoxdur.</p>}<Pages base={sessionUrl+"&"} page={page} next={roster.hasNext}/></>;
      }
    }
  }
  return <main className="shell section"><Link href="/hesab">← Hesabım</Link><h1>Dərs hesabatları</h1>{content}</main>;
}
