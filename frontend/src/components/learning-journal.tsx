import Link from "next/link";
import { learningRequest, lessonDate, pageNumber, uuid, type Slice } from "@/lib/learning";

export type CourseSummary = {
  id: string; title: string; level: string; minAge: number; maxAge: number;
  description: string; status: string;
};
type Enrollment = { course: CourseSummary; enrollmentStatus: string; totalPoints: number };
type History = { totalPoints: number; entries: Slice<{
  id: string; type: string; points: number; reason: string; createdAt: string;
}> };
export type JournalQuery = { course?: string; page?: string; historyPage?: string };
const enrollmentLabels: Record<string, string> = { ACTIVE: "Davam edir", COMPLETED: "Tamamlanıb", LEFT: "Kursdan ayrılıb" };

export function PageLinks({ page, next, href }: { page: number; next: boolean; href: (n: number) => string }) {
  if (!page && !next) return null;
  return <nav className="journal-pages" aria-label="Səhifələr">
    {page > 0 && <Link href={href(page - 1)}>Əvvəlki</Link>}
    <span>Səhifə {page + 1}</span>
    {next && page < 100000 && <Link href={href(page + 1)}>Növbəti</Link>}
  </nav>;
}

export function CabinetHeading({ title, description }: { title: string; description?: string }) {
  return <header className="journal-heading">
    <Link href="/hesab?tab=overview" className="journal-back">Hesabım</Link>
    <h1>{title}</h1>{description && <p>{description}</p>}
  </header>;
}

async function Points({ token, endpoint, url, page }: { token: string; endpoint: string; url: string; page: number }) {
  const history = await learningRequest<History>(token, endpoint + "?page=" + page).catch(() => null);
  if (!history) return <p className="journal-empty" role="status">Xal tarixçəsini açmaq mümkün olmadı. <a href={url + "&historyPage=" + page}>Yenidən aç</a></p>;
  return <section className="point-history" aria-label="Xal tarixçəsi">
    <h4>Xal tarixçəsi</h4>
    {history.entries.content.map(entry => <div className="point-entry" key={entry.id}>
      <strong>{entry.points > 0 ? "+" : ""}{entry.points} xal</strong>
      <div><p>{entry.reason}</p><small>{lessonDate(entry.createdAt)} · {entry.type === "CORRECTION" ? "Xal düzəlişi" : "Verilən xal"}</small></div>
    </div>)}
    {!history.entries.content.length && <p>Bu səhifədə xal qeydi yoxdur.</p>}
    <PageLinks page={page} next={history.entries.hasNext} href={n => url + "&historyPage=" + n}/>
  </section>;
}

export async function CourseJournal({ token, childId, query }: { token: string; childId?: number; query: JournalQuery }) {
  const page = pageNumber(query.page);
  const endpoint = childId ? `parent/children/${childId}/courses` : "courses/my-learning";
  const base = childId ? `/valideyn?child=${childId}&tab=courses&` : "/telebe?";
  const listUrl = base + "page=" + page;
  const data = await learningRequest<Slice<Enrollment>>(token, endpoint + "?page=" + page).catch(() => null);
  if (!data) return <p className="journal-empty">Kurs siyahısını yükləmək mümkün olmadı. <a href={listUrl}>Yenidən aç</a></p>;
  const selected = uuid.test(query.course ?? "") ? query.course : undefined;
  return <><div className="journal-list">{data.content.map(({ course, enrollmentStatus, totalPoints }) => {
    const open = selected === course.id;
    const url = listUrl + "&course=" + course.id;
    return <article className="journal-course" key={course.id}>
      <header><div><small>{course.status === "ARCHIVED" ? "Arxiv · " : ""}{enrollmentLabels[enrollmentStatus] ?? enrollmentStatus} · {course.level}</small>
        <h3>{course.title}</h3></div><strong className="score">{totalPoints} <span>xal</span></strong></header>
      {course.description && <p>{course.description}</p>}
      <Link className="journal-link" aria-expanded={open} href={open ? listUrl : url} scroll={false}>{open ? "Xal tarixçəsini bağla" : "Xal tarixçəsinə bax"}</Link>
      {open && <Points token={token} endpoint={childId ? endpoint + "/" + course.id + "/points" : "courses/" + course.id + "/students/me/points"} url={url} page={pageNumber(query.historyPage)}/>}
    </article>;
  })}</div>
    {!data.content.length && <p className="journal-empty">{page ? "Bu səhifədə kurs yoxdur." : "Kursa qeydiyyat tamamlandıqda kurs və qazanılan xallar burada görünəcək."} {!page && <Link href="/kurslar">Kurslara bax</Link>}</p>}
    <PageLinks page={page} next={data.hasNext} href={n => base + "page=" + n}/>
  </>;
}
