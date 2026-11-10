import type { Metadata } from "next";
import { customerOrders } from "@/lib/server";
import { AuthForm, OrderList } from "@/components/account";
export const metadata: Metadata = { title: "Hesabım" };
export const dynamic = "force-dynamic";
export default async function Account({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const session = await customerOrders(), query = await searchParams;
  if (!session.loggedIn) return <AuthForm next={query.next === "/sifaris" ? "/sifaris" : "/hesab"} />;
  return <OrderList orders={session.orders} unavailable={session.unavailable} />;
}
