import { Suspense } from "react";
import Link from "next/link";
import { Icon } from "@/components/icons";
import { ProductCard } from "@/components/product-card";
import { courses } from "@/lib/content";
import { catalog } from "@/lib/server";

export const dynamic = "force-dynamic";

export default function Home() {
  return <>
    <section className="shell entry">
      <header className="entry-heading"><h1>Uşaqlar üçün ixtira emalatxanası</h1>
        <p>Emalatxanada robototexnika öyrənin və ya evdə yığmaq üçün bir dəst seçin.</p></header>
      <div className="entry-choices">
        <Link className="entry-choice entry-course" href="/kurslar">
          <img src="/brand/classroom.jpg" alt="" fetchPriority="high"/>
          <div><h2>Kurslar və emalatxanalar</h2><p>Yaşa və təcrübəyə uyğun dərslər</p><span>Kurs seç</span></div>
        </Link>
        <Link className="entry-choice entry-shop" href="/magaza">
          <img src="/brand/kit-box.jpg" alt="" />
          <div><h2>Özün qur dəstləri</h2><p>Evdə yığmaq üçün modellər</p><span>Dəst seç</span></div>
        </Link>
      </div>
      <nav className="entry-return" aria-label="Hesab əməliyyatları">
        <Link href="/hesab"><Icon name="user"/><span><strong>Kabinetimə daxil ol</strong><small>Tələbə, valideyn və əməkdaş hesabları</small></span></Link>
        <Link href="/hesab?tab=orders"><Icon name="bag"/><span><strong>Sifarişlərimi izlə</strong><small>Rezervasiyalar və sifariş tarixçəsi</small></span></Link>
      </nav>
    </section>
    <section className="shell studio-section"><header><h2>Tədris istiqamətləri</h2><Link className="journal-link" href="/kurslar">Bütün kurslar</Link></header>
      <div className="studio-programs">{courses.map(c => <Link key={c.id} href={`/kurslar#${c.id}`}>
        <h3>{c.title}</h3><p>{c.topics.slice(0, 2).join(" · ")}</p>
      </Link>)}</div>
    </section>
    <Suspense fallback={<p className="shell" role="status">Dəstlər yüklənir…</p>}><FeaturedKits/></Suspense>
    <section className="shell faq-section studio-section"><h2>Valideynlərin sualları</h2><div className="faq-list">{[
      ["Kursa başlamaq üçün əvvəlcədən bilik lazımdır?", "Başlanğıc qrupunu seçərkən uşağın yaşı, maraqları və əvvəlki təcrübəsi nəzərə alınır."],
      ["Kurs və masterklass arasında nə fərq var?", "Kurs bir istiqamət üzrə ardıcıl dərslərdən ibarətdir. Masterklassda uşaqlar bir layihə üzərində işləyirlər."],
      ["Dərs hesabatlarını haradan oxuya bilərəm?", "Mərkəzin göndərdiyi dəvət keçidi ilə valideyn hesabınızı açın. Uşağınız hesaba bağlandıqdan sonra yayımlanan hesabatları valideyn kabinetində oxuya bilərsiniz."],
      ["Qruplar və dərs vaxtları barədə kimə müraciət edim?", "Emalatxana ilə əlaqə saxlayın. Uşağınızın yaşı və maraqlarına uyğun qrupları və dərs vaxtlarını birlikdə dəqiqləşdirə bilərsiniz."],
    ].map(([q, a]) => <details key={q}><summary>{q}<span aria-hidden="true">+</span></summary><p>{a}</p></details>)}</div></section>
  </>;
}

async function FeaturedKits() {
  const data = await catalog();
  return <section className="shell studio-section"><header><h2>Özün qur dəstləri</h2><Link className="journal-link" href="/magaza">Mağazaya bax</Link></header>
      {data.mode === "showcase" && <p className="catalog-note">Qiymət və mövcudluğu emalatxanadan öyrənə bilərsən.</p>}
      {data.unavailable ? <p className="notice">Mağazanın kataloqu hazırda əlçatan deyil. Dəstlər haqqında <Link href="/elaqe">bizimlə əlaqə saxla.</Link></p> :
      data.products.length ? <div className="product-grid">{data.products.slice(0, 4).map((p, i) => <ProductCard key={p.id} product={p} index={i}/>)}</div> : <p className="notice">Kataloqa hələ dəst əlavə edilməyib.</p>}
    </section>;
}
