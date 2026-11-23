"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Icon } from "./icons";
import { useStore } from "./store";
const links = [["/kurslar", "Kurslar"], ["/magaza", "Dəst mağazası"], ["/elaqe", "Əlaqə"]];
export function Header() {
  const path = usePathname();
  const { lines, open } = useStore();
  const privateArea = ["/telebe", "/muellim", "/valideyn"].includes(path);
  const navigation = privateArea ? [[path, "Kabinetim"], ["/hesab?tab=orders", "Sifarişlərim"], ["/elaqe", "Əlaqə"]] : links;
  const count = lines.reduce((sum, line) => sum + line.quantity, 0);
  return <header className="site-header"><div className="shell header-inner">
    <Link href="/" className="wordmark" aria-label="Kodariun — ana səhifə"><img src="/brand/logo.jpg" width="39" height="39" alt=""/><span>kodariun<span className="logo-dot">.</span></span></Link>
    <nav className="primary-nav" aria-label="Əsas menyu">{navigation.map(([href, label]) =>
      <Link key={href} href={href} aria-current={path.startsWith(href) ? "page" : undefined}>{label}</Link>
    )}</nav>
    <div className="header-actions"><Link href={privateArea ? "/hesab?tab=overview" : "/hesab"} className="header-account">
      <Icon name="user" width="18"/>{privateArea ? "Hesabım" : "Kabinetim"}</Link>
      {!privateArea && <button className="bag-button" aria-label={count ? "Səbəti aç, " + count + " ədəd" : "Səbəti aç"} onClick={open}><Icon name="bag"/><span className="bag-label">Səbət</span>{count > 0 && <span className="bag-count">{count}</span>}</button>}
    </div></div></header>;
}
export function Footer() {
  return <footer className="site-footer"><div className="shell"><div className="footer-top"><div><Link className="footer-wordmark" href="/">kodariun.</Link><p>Robototexnika və STEAM emalatxanası</p></div><div><span className="eyebrow">Bölmələr</span><Link href="/kurslar">Kurslar və emalatxanalar</Link><Link href="/magaza">Özün qur dəstləri</Link><Link href="/haqqimizda">Kodariun haqqında</Link></div><div><span className="eyebrow">Əlaqə</span><a href="tel:+994556612423">+994 55 661 24 23</a><Link href="/elaqe">Metropark TM, 7-ci mərtəbə<br />Master Klass AZ daxilində</Link></div><div><span className="eyebrow">Instagram</span><a href="https://www.instagram.com/kodariun/" target="_blank" rel="noreferrer">@kodariun <span>↗</span></a><a href="https://www.instagram.com/kodariun.az/" target="_blank" rel="noreferrer">@kodariun.az <span>↗</span></a></div></div><div className="footer-bottom"><span>© 2026 Kodariun</span><Link href="/elaqe">Əlaqə məlumatları</Link></div></div></footer>;
}
