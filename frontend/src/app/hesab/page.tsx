import type { Metadata } from "next";
import { customerOrders } from "@/lib/server";
import { AuthForm, OrderList } from "@/components/account";
export const metadata: Metadata = { title: "Hesabım" };
export const dynamic = "force-dynamic";
export default async function Account({ searchParams }: { searchParams: Promise<{ next?: string; page?: string }> }) {
  const query = await searchParams;
  const page = query.page && /^\d{1,6}$/.test(query.page) ? Math.min(100000, Number(query.page)) : 0;
  const session = await customerOrders(page);
  if (!session.loggedIn) return <AuthForm next={query.next === "/sifaris" ? "/sifaris" : "/hesab"} />;
  return <OrderList orders={session.orders} unavailable={session.unavailable} page={page} />;
}
