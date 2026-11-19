import Link from "next/link";
import { redirect } from "next/navigation";
import { accountSession } from "@/lib/admin-session";
import { learningRequest, lessonDate, pageNumber, uuid, type LessonReport, type Slice } from "@/lib/learning";

export const metadata = { title: "Valideyn kabineti", robots: { index: false } };
type Child = { accountId: number; firstName: string; lastName: string };
type Learning = { course: { id: string; title: string }; totalPoints: number };
type History = { totalPoints: number; entries: Slice<{ id: string; points: number; reason: string; createdAt: string }> };
type Query = { child?: string; tab?: string; course?: string; page?: string };

async function ChildLearning({token, child, query}: {token: string; child: Child; query: Query}) {
  const base = `parent/children/${child.accountId}`;
  const tab = query.tab === "reports" ? "reports" : "courses";
  const course = tab === "courses" && uuid.test(query.course ?? "") ? query.course : undefined;
  const page = pageNumber(query.page);
  const url = `/valideyn?child=${child.accountId}&tab=${tab}` + (course ? "&course=" + course : "");
  let hasNext: boolean;
  let content;
  if (tab === "reports") {
    const result = await learningRequest<Slice<LessonReport>>(token, base + "/reports?page=" + page);
    hasNext = result.hasNext;
    content = result.content.length ? result.content.map(r => <article key={r.id} className="order-card">
      <small>{lessonDate(r.heldAt)} · {r.courseTitle}</small><h3>{r.topic}</h3>
      {r.attended ? <><p><strong>Nə öyrəndi?</strong> {r.learned}</p><p><strong>Dərsdə iştirakı</strong> {r.participation}</p></> : <p>Dərsdə iştirak etməyib.</p>}
      {r.recommendation && <p><strong>Evdə məşq</strong> {r.recommendation}</p>}
    </article>) : <p>Bu səhifədə yayımlanmış hesabat yoxdur.</p>;
  } else if (course) {
    const result = await learningRequest<History>(token, base + "/courses/" + course + "/points?page=" + page);
    hasNext = result.entries.hasNext;
    content = <><h3>Ümumi xal · {result.totalPoints}</h3>{result.entries.content.length ? result.entries.content.map(p =>
      <p key={p.id} className="order-card"><strong>{p.points > 0 ? "+" : ""}{p.points} xal</strong> · {p.reason}<br/><small>{lessonDate(p.createdAt)}</small></p>
    ) : <p>Bu səhifədə xal qeydi yoxdur.</p>}</>;
  } else {
    const result = await learningRequest<Slice<Learning>>(token, base + "/courses?page=" + page);
    hasNext = result.hasNext;
    content = result.content.length ? result.content.map(item => <article key={item.course.id} className="order-card">
      <h3>{item.course.title}</h3><Link className="text-link" href={url + "&course=" + item.course.id}>{item.totalPoints} xal · Tarixçəyə bax ↗</Link>
    </article>) : <p>Bu səhifədə kurs yoxdur.</p>;
  }
  return <><h2>{child.firstName} {child.lastName}</h2><nav className="flex gap-6 my-5">
    <Link className="text-link" href={`/valideyn?child=${child.accountId}`}>Kurslar və xallar</Link>
    <Link className="text-link" href={`/valideyn?child=${child.accountId}&tab=reports`}>Dərs hesabatları</Link>
  </nav>{content}<nav className="flex gap-6 my-5">{page > 0 && <Link href={url + "&page=" + (page - 1)}>← Əvvəlki</Link>}
    {hasNext && <Link href={url + "&page=" + (page + 1)}>Sonrakı →</Link>}</nav></>;
}

export default async function ParentPage({searchParams}: {searchParams: Promise<Query>}) {
  const session = await accountSession();
  if (session.status === 401) redirect("/hesab?next=/valideyn");
  if (session.status !== 200 || !session.roles.includes("PARENT")) return <main className="shell section"><p>{session.status === 503 ? "Hesab xidməti əlçatan deyil." : "Valideyn hesabı ilə daxil olun."}</p></main>;
  const query = await searchParams;
  const children = await learningRequest<Child[]>(session.token, "parent/children");
  const child = query.child ? children.find(c => String(c.accountId) === query.child) : children[0];
  return <main className="shell section"><Link className="text-link" href="/hesab">← Hesabım</Link><h1>Uşaqlarım</h1>
    <nav className="flex gap-6 my-5">{children.map(c => <Link key={c.accountId} href={`/valideyn?child=${c.accountId}`}>{c.firstName} {c.lastName}</Link>)}</nav>
    {child ? <ChildLearning token={session.token} child={child} query={query}/> :
      <p>{children.length ? "Bu tələbəyə girişiniz yoxdur." : "Hesabınıza hələ tələbə bağlanmayıb."}</p>}
  </main>;
}
