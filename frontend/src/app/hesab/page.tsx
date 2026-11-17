import type { Metadata } from "next";
import { customerOrders } from "@/lib/server";
import { adminSession } from "@/lib/admin-session";
import Link from "next/link";
import { AuthForm, OrderList } from "@/components/account";
export const metadata: Metadata = { title: "Hesabım" };
export const dynamic = "force-dynamic";
export default async function Account({ searchParams }: { searchParams: Promise<{ next?: string; page?: string }> }) {
  const query = await searchParams;
  const page = query.page && /^\d{1,6}$/.test(query.page) ? Math.min(100000, Number(query.page)) : 0;
  const [session, admin] = await Promise.all([customerOrders(page), adminSession()]);
  if (!session.loggedIn && admin.status !== 200) return <AuthForm next={query.next === "/admin" ? "/admin" : query.next === "/sifaris" ? "/sifaris" : "/hesab"} />;
  return <>{admin.status === 200 && <div className="shell"><Link className="text-link" href="/admin">Admin panelinə keç ↗</Link></div>}<OrderList orders={session.orders} unavailable={session.unavailable} page={page} /></>;
}
