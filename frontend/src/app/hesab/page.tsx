import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { customerOrders } from "@/lib/server";
import { accountSession } from "@/lib/admin-session";
import { pageNumber } from "@/lib/learning";
import { AuthForm, OrderList, Logout } from "@/components/account";

export const metadata: Metadata = { title: "Hesabım", robots: { index: false } };
const desks = [
  { role: "PARENT", path: "/valideyn", title: "Valideyn kabineti", detail: "Uşaqlarımın dərs hesabatları, davamiyyəti və xalları" },
  { role: "STUDENT", path: "/telebe", title: "Tələbə kabineti", detail: "Kurslarım, topladığım xallar və müəllimin xal qeydləri" },
  { role: "TEACHER", path: "/muellim", title: "Müəllim kabineti", detail: "Keçirilmiş dərslər və valideynlər üçün hesabatlar" },
  { role: "ADMIN", path: "/admin", title: "İdarəetmə", detail: "Kurslar, hesablar, valideyn dəvətləri və ailə əlaqələri" },
];

export default async function Account({ searchParams }: { searchParams: Promise<{ next?: string; tab?: string; page?: string }> }) {
  const query = await searchParams;
  const account = await accountSession();
  const wantsOrders = query.tab === "orders" || query.page !== undefined;
  const next = wantsOrders ? "/hesab?tab=orders" : [...desks.map(d => d.path), "/sifaris"].includes(query.next ?? "") ? query.next : "/hesab";
  if (account.status === 401) return <AuthForm next={next}/>;
  if (account.status !== 200) return <div className="shell journal"><h1>Hesabım</h1><p className="journal-empty">
    {account.status === 503 ? "Hesab xidməti cavab vermir. Bir az sonra yenidən cəhd et." : "Bu hesab aktiv deyil."}</p><Logout/></div>;
  const available = desks.filter(d => account.roles.includes(d.role));
  if (query.next === "/sifaris") redirect("/sifaris");
  const requested = available.find(d => d.path === next);
  if (requested) redirect(requested.path);
  if (!wantsOrders && query.tab !== "overview" && available.length === 1) redirect(available[0].path);
  const showOrders = !available.length || wantsOrders;
  const page = pageNumber(query.page);
  const orders = showOrders ? await customerOrders(page) : undefined;
  return <div className="shell journal">
    <header className="journal-heading account-heading"><div><h1>Hesabım</h1><p>{account.label}</p></div><Logout/></header>
    {!!available.length && <nav aria-label="Hesab bölmələri" className="journal-tabs">
      <Link href="/hesab?tab=overview" aria-current={!showOrders ? "page" : undefined}>Kabinetlərim</Link>
      <Link href="/hesab?tab=orders" aria-current={showOrders ? "page" : undefined}>Sifarişlərim</Link>
    </nav>}
    {orders ? orders.loggedIn ? <OrderList orders={orders.orders} unavailable={orders.unavailable} page={page}/> :
      <p className="journal-empty">Sifarişlərə baxmaq üçün hesabına yenidən daxil ol.</p> :
      <nav className="account-desks" aria-label="Kabinet seçimi">{available.map(d => <Link href={d.path} key={d.role}>
        <h2>{d.title}</h2><p>{d.detail}</p><span>Kabineti aç</span>
      </Link>)}</nav>}
  </div>;
}
