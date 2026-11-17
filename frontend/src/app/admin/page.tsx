import Link from "next/link";
import { redirect } from "next/navigation";
import { adminSession } from "@/lib/admin-session";
import { AdminPanel } from "@/components/admin/panel";
import "./admin.css";

export const metadata = { title: "Admin paneli", robots: { index: false } };
export const dynamic = "force-dynamic";
export default async function AdminPage() {
  const session = await adminSession();
  if (session.status === 401) redirect("/hesab?next=/admin");
  if (session.status !== 200) return <main className="shell section"><h1>{session.status === 403 ? "Admin səlahiyyəti tələb olunur" : "Hesab xidməti əlçatan deyil"}</h1><Link className="text-link" href="/hesab">Hesabıma qayıt</Link></main>;
  return <AdminPanel label={session.label} />;
}
