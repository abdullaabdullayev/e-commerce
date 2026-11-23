"use client";
import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { money, orderLabels } from "@/lib/content";
import type { CustomerOrder } from "@/lib/types";
import { Icon } from "./icons";

export function AuthForm({ next = "/hesab" }: { next?: string }) {
  const [mode, setMode] = useState<"login" | "register">("login"), [busy, setBusy] = useState(false), [message, setMessage] = useState(""), [success, setSuccess] = useState(false);
  const roleTitle: Record<string, string> = { "/valideyn": "Valideyn kabineti", "/telebe": "Tələbə kabineti", "/muellim": "Müəllim kabineti", "/admin": "İdarəetmə" };
  const role = roleTitle[next];
  const checkout = next === "/sifaris";
  const orders = next === "/hesab?tab=orders";
  const title = role || (checkout ? "Rezervasiyaya davam et" : orders ? "Sifarişlərim" : "Kabinetimə giriş");
  const description = checkout ? "Daxil olduqdan sonra seçdiyiniz dəstlərə qayıdacaqsınız." :
    orders ? "Sifarişləri və rezervasiyaları görmək üçün alış zamanı istifadə etdiyiniz hesabla daxil olun." :
    role ? "Tədris mərkəzində istifadə etdiyiniz hesabla daxil olun." : "Daxil olduqda hesabınıza uyğun kabinet açılacaq.";
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setMessage(""); setSuccess(false);
    const form = new FormData(event.currentTarget);
    const identifier = String(form.get("identifier")).trim();
    const body = mode === "login" ? { login: identifier, password: form.get("password") } :
      { email: identifier, password: form.get("password"), firstName: form.get("firstName"), lastName: form.get("lastName") };
    try {
      if (new TextEncoder().encode(String(body.password)).length > 72) throw new Error("Şifrə ən çox 72 bayt ola bilər. Daha qısa şifrə seç.");
      const response = await fetch(`/api/auth/${mode}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
      const result = await response.json();
      if (!response.ok) throw new Error(result.detail || "Sorğu tamamlanmadı.");
      if (mode === "register") { setMode("login"); setSuccess(true); setMessage("Hesabın yaradıldı. İndi email və şifrənlə daxil ola bilərsən."); }
      else { window.location.replace(role ? "/hesab?next=" + encodeURIComponent(next) : next); }
    } catch (error) { setMessage(error instanceof Error ? error.message : "Hazırda qoşulmaq mümkün deyil."); }
    finally { setBusy(false); }
  }
  return <div className="auth-shell"><Link className="journal-back" href={checkout ? "/sifaris" : "/"}>{checkout ? "Səbətə qayıt" : "Ana səhifə"}</Link>
    <h1>{mode === "login" ? title : "Mağaza hesabı yarat"}</h1>
    <p>{mode === "login" ? description : "Dəst rezervasiya etmək və sifarişlərinizi izləmək üçün qeydiyyatdan keçin."}</p>
    {!role && <div className="auth-tabs"><button disabled={busy} aria-pressed={mode === "login"} onClick={() => { setMode("login"); setMessage(""); }}>Daxil ol</button>
      <button disabled={busy} aria-pressed={mode === "register"} onClick={() => { setMode("register"); setMessage(""); }}>Mağaza hesabı yarat</button></div>}
    <form onSubmit={submit}>{mode === "register" && <div className="grid grid-cols-2 gap-5"><label className="field"><span>Ad</span><input name="firstName" required maxLength={80} autoComplete="given-name" /></label><label className="field"><span>Soyad</span><input name="lastName" required maxLength={80} autoComplete="family-name" /></label></div>}<label className="field"><span>{mode === "login" ? "E-poçt və ya istifadəçi adı" : "E-poçt"}</span><input name="identifier" type={mode === "login" ? "text" : "email"} required maxLength={254} autoComplete={mode === "login" ? "username" : "email"} /></label><label className="field"><span>Şifrə</span><input name="password" type="password" required minLength={mode === "register" ? 8 : 1} maxLength={72} autoComplete={mode === "login" ? "current-password" : "new-password"} /></label>{mode === "register" && <p className="text-xs mb-5">Ən azı 8 simvoldan ibarət şifrə seç.</p>}{message && <p className={`form-message ${success ? "" : "error-message"}`} role="status">{message}</p>}<button className="button wide" type="submit" disabled={busy}>{busy ? "Bir az gözlə…" : mode === "login" ? "Daxil ol" : "Hesab yarat"}<Icon name="arrow" /></button></form>{!checkout && !orders && <details className="login-help"><summary>Tədris hesabını necə əldə edə bilərəm?</summary><p>Tələbə və əməkdaş hesablarını mərkəz yaradır. Valideyn hesabını mərkəzin göndərdiyi dəvət keçidi ilə aça bilərsiniz.</p><Link className="journal-link" href="/elaqe">Mərkəzlə əlaqə</Link></details>}</div>;
}

export function OrderList({ orders, unavailable, page = 0 }: { orders: CustomerOrder[]; unavailable?: boolean; page?: number }) {
  const router = useRouter(), [busy, setBusy] = useState<string | null>(null), [message, setMessage] = useState("");
  const hasPendingOrders = orders.some(order => ["RESERVING", "PENDING_PAYMENT", "CANCELLING", "CONFIRMING"].includes(order.status));
  useEffect(() => {
    if (!hasPendingOrders) return;
    const interval = setInterval(() => { if (document.visibilityState === "visible") router.refresh(); }, 10000);
    return () => clearInterval(interval);
  }, [hasPendingOrders, router]);
  async function cancel(id: string) {
    setBusy(id); setMessage("");
    try { const response = await fetch(`/api/backend/orders/${id}/cancel`, { method: "POST" }); const data = await response.json(); if (!response.ok) throw new Error(data.detail || "Ləğv etmək mümkün olmadı."); router.refresh(); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Sorğu tamamlanmadı."); }
    finally { setBusy(null); }
  }
  return <div className="orders-list"><div className="flex justify-between items-center border-b border-ink/15 pb-5 mb-5"><h2 className="!text-3xl">Sifarişlərim</h2><button className="secondary-button" onClick={() => router.refresh()}>Yenilə ↻</button></div>{message && <p role="alert" className="form-message error-message">{message}</p>}{unavailable ? <div className="notice">Sifariş xidməti hazırda əlçatan deyil. Bir az sonra yenilə.</div> : orders.length === 0 ? <div className="empty-state"><h3>{page ? "Bu səhifədə sifariş yoxdur." : "Hələ sifarişin yoxdur."}</h3><Link className="button" href="/magaza">Dəstləri kəşf et <Icon name="arrow" /></Link></div> : orders.map(order => <article key={order.id} className="order-card"><div className="order-heading"><div><strong>Sifariş #{order.id.slice(0, 8)}</strong><small>{new Intl.DateTimeFormat("az-AZ", { dateStyle: "medium" }).format(new Date(order.createdAt))}</small></div><span className="order-status">{orderLabels[order.status] || order.status}</span></div><div className="order-items">{order.items.map(item => <p key={item.productId}>{item.name} <span>× {item.quantity}</span></p>)}</div>{order.status === "PENDING_PAYMENT" && <p className="text-xs mb-4">Rezervasiya {order.expiresAt ? new Date(order.expiresAt).toLocaleTimeString("az-AZ", { hour: "2-digit", minute: "2-digit" }) : "göstərilən vaxtda"} bitir. Onlayn kart ödənişi bu mərhələdə hələ aktiv deyil.</p>}<div className="order-actions"><strong>{money(Number(order.amount), order.currency)}</strong>{["RESERVING", "PENDING_PAYMENT"].includes(order.status) && <button className="secondary-button" disabled={busy === order.id} onClick={() => cancel(order.id)}>{busy === order.id ? "Ləğv edilir…" : "Rezervasiyanı ləğv et"}</button>}</div></article>)}{!unavailable && (page > 0 || orders.length === 20) && <nav className="flex items-center justify-center gap-6 mt-10" aria-label="Sifariş səhifələri">{page > 0 && <Link className="secondary-button" href={`/hesab?tab=orders&page=${page - 1}`}>← Əvvəlki</Link>}<span>Səhifə {page + 1}</span>{orders.length === 20 && page < 100000 && <Link className="secondary-button" href={`/hesab?tab=orders&page=${page + 1}`}>Növbəti →</Link>}</nav>}</div>;
}


export function Logout() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function logout() {
    setBusy(true);
    try {
      const response = await fetch("/api/auth/logout", { method: "POST" });
      if (!response.ok) throw new Error();
      router.replace("/hesab");
      router.refresh();
    } catch { setError("Çıxış alınmadı. Yenidən cəhd et."); }
    finally { setBusy(false); }
  }
  return <div><button className="secondary-button" disabled={busy} onClick={logout}>Çıxış et</button>{error && <p role="alert">{error}</p>}</div>;
}
