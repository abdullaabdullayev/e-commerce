import Link from "next/link";
import { redirect } from "next/navigation";
import { accountSession } from "@/lib/admin-session";
import { CabinetHeading, CourseJournal, type JournalQuery } from "@/components/learning-journal";

export const metadata = { title: "Tələbə kabineti", robots: { index: false } };

export default async function StudentPage({ searchParams }: { searchParams: Promise<JournalQuery> }) {
  const session = await accountSession();
  if (session.status === 401) redirect("/hesab?next=/telebe");
  return <div className="shell journal">
    <CabinetHeading title="Tələbə kabineti"/>
    {session.status === 200 && session.roles.includes("STUDENT") ? <section className="journal-body">
      <h2>Kurslarım və xallarım</h2>
      <CourseJournal token={session.token} query={await searchParams}/>
    </section> : <p className="journal-empty">{session.status === 503 ? "Hesab xidməti hazırda əlçatan deyil." : "Bu bölməyə tələbə hesabı ilə daxil ol."} <Link href="/hesab">Hesabıma qayıt</Link></p>}
  </div>;
}
