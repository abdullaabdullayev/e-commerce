import Link from "next/link";
import { redirect } from "next/navigation";
import { accountSession } from "@/lib/admin-session";
import { learningRequest, lessonDate, pageNumber, type LessonReport, type Slice } from "@/lib/learning";
import { CabinetHeading, CourseJournal, PageLinks, type JournalQuery } from "@/components/learning-journal";

export const metadata = { title: "Valideyn kabineti", robots: { index: false } };
type Child = { accountId: number; firstName: string; lastName: string };
type Query = JournalQuery & { child?: string; tab?: string };
const childName = (c: Child) => [c.firstName, c.lastName].filter(Boolean).join(" ") || "Tələbə #" + c.accountId;

async function Reports({ token, child, page }: { token: string; child: Child; page: number }) {
  const url = `/valideyn?child=${child.accountId}&tab=reports&page=`;
  const reports = await learningRequest<Slice<LessonReport>>(token, `parent/children/${child.accountId}/reports?page=${page}`).catch(() => null);
  if (!reports) return <p className="journal-empty">Hesabatları yükləmək mümkün olmadı. <a href={url + page}>Yenidən aç</a></p>;
  return <>{reports.content.map((r, i) => <details className="lesson-report" key={r.id} open={i === 0}>
    <summary>{r.topic}<small><time dateTime={r.heldAt}>{lessonDate(r.heldAt)}</time> · {r.courseTitle}</small></summary>
    <dl><dt>Davamiyyət</dt><dd>{r.attended ? "Dərsdə iştirak edib" : "Dərsdə iştirak etməyib"}</dd>
      {r.attended && <><dt>Nə öyrəndi?</dt><dd>{r.learned}</dd><dt>Dərsdə iştirakı</dt><dd>{r.participation}</dd></>}
      {r.recommendation && <><dt>Evdə məşq</dt><dd>{r.recommendation}</dd></>}
    </dl>
  </details>)}
    {!reports.content.length && <p className="journal-empty">{page ? "Bu səhifədə hesabat yoxdur." : "Müəllim dərs hesabatını yayımladıqda onu burada oxuya biləcəksiniz."}</p>}
    <PageLinks page={page} next={reports.hasNext} href={n => url + n}/>
  </>;
}

export default async function ParentPage({ searchParams }: { searchParams: Promise<Query> }) {
  const session = await accountSession();
  if (session.status === 401) redirect("/hesab?next=/valideyn");
  const heading = <CabinetHeading title="Valideyn kabineti"/>;
  if (session.status !== 200 || !session.roles.includes("PARENT")) return <div className="shell journal">{heading}
    <p className="journal-empty">{session.status === 503 ? "Hesab xidməti hazırda əlçatan deyil." : "Bu bölməyə valideyn hesabı ilə daxil olun."}</p></div>;
  const query = await searchParams;
  const children = await learningRequest<Child[]>(session.token, "parent/children").catch(() => null);
  if (!children) return <div className="shell journal">{heading}<p className="journal-empty">Uşaqların siyahısını yükləmək mümkün olmadı. <a href="/valideyn">Yenidən aç</a></p></div>;
  const child = query.child ? children.find(c => String(c.accountId) === query.child) : children[0];
  const courses = query.tab === "courses";
  return <div className="shell journal">{heading}<div className={children.length > 1 || !child && children.length ? "journal-layout" : undefined}>
    {(children.length > 1 || (!child && children.length > 0)) && <aside><h2>Uşaq seçimi</h2><nav className="journal-nav" aria-label="Uşaq seçimi">
      {children.map(c => <Link key={c.accountId} aria-current={child === c ? "page" : undefined} href={`/valideyn?child=${c.accountId}&tab=${courses ? "courses" : "reports"}`}>{childName(c)}</Link>)}
    </nav></aside>}
    <section className="journal-body">{child ? <><h2>{childName(child)}</h2>
      <nav className="journal-tabs" aria-label="Uşağın tədris məlumatları">
        <Link aria-current={!courses ? "page" : undefined} href={`/valideyn?child=${child.accountId}&tab=reports`}>Dərs hesabatları</Link>
        <Link aria-current={courses ? "page" : undefined} href={`/valideyn?child=${child.accountId}&tab=courses`}>Kurslar və xallar</Link>
      </nav>{courses ? <CourseJournal token={session.token} childId={child.accountId} query={query}/> : <Reports token={session.token} child={child} page={pageNumber(query.page)}/>}</> :
      <p className="journal-empty">{children.length ? "Bu tələbənin məlumatlarına girişiniz yoxdur. Siyahıdan uşağı seçin." : "Hesabınıza hələ uşaq bağlanmayıb. Tədris mərkəzindən uşağınızın hesabını əlavə etməsini xahiş edin."} {!children.length && <Link href="/elaqe">Mərkəzlə əlaqə</Link>}</p>}
    </section></div></div>;
}
