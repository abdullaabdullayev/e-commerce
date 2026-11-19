import type { Metadata } from "next";
import { customerOrders } from "@/lib/server";
import { accountSession } from "@/lib/admin-session";
import Link from "next/link";
import { AuthForm, OrderList } from "@/components/account";
export const metadata: Metadata = { title: "Hesabım" };
export const dynamic = "force-dynamic";
export default async function Account({ searchParams }: { searchParams: Promise<{ next?: string; page?: string }> }) {
  const query = await searchParams;
  const page = query.page && /^\d{1,6}$/.test(query.page) ? Math.min(100000, Number(query.page)) : 0;
  const [session, account] = await Promise.all([customerOrders(page), accountSession()]);
  const next = ["/admin", "/sifaris", "/valideyn", "/muellim"].includes(query.next ?? "") ? query.next : "/hesab";
  if (!session.loggedIn && account.status !== 200) return <AuthForm next={next} />;
  const roles = account.status === 200 ? account.roles : [];
  return <><nav className="shell flex gap-6">
    {roles.includes("ADMIN") && <Link className="text-link" href="/admin">Admin paneli ↗</Link>}
    {roles.includes("PARENT") && <Link className="text-link" href="/valideyn">Uşaqlarım ↗</Link>}
    {roles.includes("TEACHER") && <Link className="text-link" href="/muellim">Dərs hesabatları ↗</Link>}
  </nav><OrderList orders={session.orders} unavailable={session.unavailable} page={page} /></>;

}
