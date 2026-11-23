import type { Metadata } from "next";
import Link from "next/link";
import { courses } from "@/lib/content";
import { gateway } from "@/lib/server";
import { pageNumber, type Slice } from "@/lib/learning";
import { PageLinks, type CourseSummary } from "@/components/learning-journal";

export const metadata: Metadata = { title: "Kurslar və emalatxanalar" };
export const dynamic = "force-dynamic";

async function published(page: number): Promise<Slice<CourseSummary> | null> {
  try {
    const res = await fetch(gateway() + "/api/v1/courses?page=" + page, { cache: "no-store", signal: AbortSignal.timeout(5000) });
    return res.ok ? await res.json() : null;
  } catch { return null; }
}

export default async function Courses({ searchParams }: { searchParams: Promise<{ page?: string }> }) {
  const page = pageNumber((await searchParams).page);
  const result = await published(page);
  return <div className="shell content-bottom">
    <header className="page-heading"><h1>Kurslar və emalatxanalar</h1><p>Kursu uşağın yaşına, maraqlarına və təcrübəsinə uyğun seçin. Qrup və dərs vaxtını emalatxana ilə dəqiqləşdirə bilərsiniz.</p><nav className="program-links" aria-label="Kurs səhifəsinin bölmələri"><a href="#kurs-siyahisi">Kurs kataloqu</a><a href="#istiqametler">Tədris istiqamətləri</a></nav></header>
    <section className="published-courses" id="kurs-siyahisi"><h2>Kurs kataloqu</h2>
      {result ? <>{result.content.map(c => <article key={c.id} className="journal-course">
        <small>{c.minAge}–{c.maxAge} yaş · {c.level}</small><h3>{c.title}</h3><p>{c.description}</p>
        <Link className="journal-link" href={`/elaqe?proqram=${encodeURIComponent(c.title)}#muraciet`}>Kurs haqqında soruş</Link>
      </article>)}{!result.content.length && <p className="journal-empty">Bu səhifədə yayımlanmış kurs yoxdur. <a href="#istiqametler">Tədris istiqamətlərinə baxın.</a></p>}
      <PageLinks page={page} next={result.hasNext} href={n => "/kurslar?page=" + n + "#kurs-siyahisi"}/></> :
      <p className="journal-empty">Kurs siyahısı hazırda əlçatan deyil. Qruplar barədə <Link href="/elaqe">emalatxana ilə əlaqə saxlayın.</Link></p>}
    </section>
    <h2 className="program-heading" id="istiqametler">Tədris istiqamətləri</h2>
    {courses.map(course => <section id={course.id} key={course.id} className="course-feature">
      <div className="course-feature-image"><img className={course.imageClass} src={course.image} alt={course.title} loading="lazy"/></div>
      <div><h3>{course.title}</h3><p>{course.description}</p><ul className="topic-list">{course.topics.map(t => <li key={t}>{t}</li>)}</ul>
        <Link href={`/elaqe?kurs=${course.id}#muraciet`} className="journal-link">Dərslər barədə soruş</Link></div>
    </section>)}</div>;
}
