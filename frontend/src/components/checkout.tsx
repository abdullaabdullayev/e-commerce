"use client";
import { useRef, useState } from "react";
import Link from "next/link";
import { useStore } from "./store";
import { Icon } from "./icons";
import { money, orderLabels, social } from "@/lib/content";
import type { CustomerOrder } from "@/lib/types";

export function CheckoutView({ showcaseMode, hasSession }: { showcaseMode: boolean; hasSession: boolean }) {
  const { lines, clear, quantity, ready } = useStore(), [busy, setBusy] = useState(false), [message, setMessage] = useState(""), [needsLogin, setNeedsLogin] = useState(false), [created, setCreated] = useState<CustomerOrder | null>(null), [copied, setCopied] = useState(false);
  const key = useRef("");
  const keySignature = useRef("");
  const inquiry = showcaseMode || lines.some(line => line.product.source !== "live" || line.product.price == null);
  const currencies = new Set(lines.map(line => line.product.currency));
  const total = lines.reduce((sum, line) => sum + (line.product.price || 0) * line.quantity, 0);
  async function reserve() {
    setBusy(true); setMessage(""); setNeedsLogin(false);
    try {
      const signature = JSON.stringify(lines.map(line => [line.product.id, line.quantity]).sort());
      if (keySignature.current !== signature) { key.current = ""; keySignature.current = signature; }
      try {
        const saved = JSON.parse(sessionStorage.getItem("kodariun-checkout") || "null");
        key.current = saved?.signature === signature && saved?.key ? saved.key : crypto.randomUUID();
        sessionStorage.setItem("kodariun-checkout", JSON.stringify({ signature, key: key.current }));
      } catch { if (!key.current) key.current = crypto.randomUUID(); }
      const response = await fetch("/api/backend/orders", { method: "POST", headers: { "Content-Type": "application/json", "Idempotency-Key": key.current }, body: JSON.stringify({ items: lines.map(line => ({ productId: line.product.id, quantity: line.quantity })) }) });
      const order = await response.json();
      if (response.status === 401) { setNeedsLogin(true); throw new Error("Rezervasiya üçün hesabına daxil ol."); }
      if (!response.ok) throw new Error(order.detail || "Rezervasiya tamamlanmadı.");
      if (["REJECTED", "CANCELLED", "EXPIRED"].includes(order.status)) {
        key.current = "";
        try { sessionStorage.removeItem("kodariun-checkout"); } catch {}
        throw new Error(order.status === "REJECTED" ? "Seçilən sayda stok yoxdur. Məhsulların sayını dəyiş və yenidən yoxla." : "Əvvəlki rezervasiya artıq bağlıdır. Yeni rezervasiya üçün yenidən cəhd et.");
      }
      try { sessionStorage.removeItem("kodariun-checkout"); } catch {}
      key.current = "";
      setCreated(order); clear();
    } catch (error) { setMessage(error instanceof Error ? error.message : "Hazırda serverə qoşulmaq mümkün deyil."); }
    finally { setBusy(false); }
  }
  if (!ready) return <div className="shell journal" role="status">Səbət yüklənir…</div>;
  if (created) return <div className="shell section"><div className="empty-state"><span className="eyebrow">SİFARİŞ #{created.id.slice(0, 8)}</span><h2 className="mt-5">{orderLabels[created.status] || "Sorğun qəbul edildi."}</h2><p className="detail-price">{money(Number(created.amount), created.currency)}</p>{created.expiresAt && <p>Rezervasiyanın bitmə vaxtı: {new Date(created.expiresAt).toLocaleTimeString("az-AZ", { hour: "2-digit", minute: "2-digit" })}</p>}<p>Rezervasiyanın vəziyyətini sifariş tarixçəsindən izləyə bilərsiniz.</p><Link href="/hesab?tab=orders" className="button">Sifarişlərimə bax <Icon name="arrow" /></Link></div></div>;
  return <div className="shell"><div className="page-heading"><div className="breadcrumbs"><Link href="/magaza">Mağaza</Link><span>/</span><span>Səbət</span></div><h1>{inquiry ? "Dəstlər üçün sorğu" : "Səbət və rezervasiya"}</h1></div>{lines.length === 0 ? <div className="empty-state mb-20"><h2>Səbətin hələ boşdur.</h2><Link className="button" href="/magaza">Dəstlərə bax <Icon name="arrow" /></Link></div> : <div className="checkout-grid"><div className="checkout-summary"><h2>Seçdiyin dəstlər</h2>{lines.map(({ product, quantity: count }) => <div className="cart-line" key={product.id}><img src={product.images[0] || "/brand/logo.jpg"} alt="" /><div><Link href={`/magaza/${encodeURIComponent(product.id)}`}>{product.name}</Link><small>{money(product.price, product.currency)}</small><div className="quantity"><button aria-label={`${product.name}: sayını azalt`} disabled={busy} onClick={() => quantity(product.id, count - 1)}><Icon name="minus" width="14" /></button><span>{count}</span><button aria-label={`${product.name}: sayını artır`} disabled={busy || count >= 99} onClick={() => quantity(product.id, count + 1)}><Icon name="plus" width="14" /></button></div></div></div>)}<div className="total-row mt-6"><span>Məhsulların cəmi</span><strong>{inquiry ? "Dəqiqləşdiriləcək" : currencies.size === 1 ? money(total, lines[0].product.currency) : "Fərqli valyutalar"}</strong></div></div><div className="checkout-aside"><h2>{inquiry ? "Qiymət və mövcudluq" : "Rezervasiya"}</h2>{inquiry ? <><p>Hazırda Instagram-dan model seçimini görürsən. Qiymət, mövcudluq və dəstin tərkibini bizimlə dəqiqləşdir.</p><p>Bu mərhələdə real sifariş və ya ödəniş yaradılmır.</p><button className="button wide" onClick={async () => { try { await navigator.clipboard.writeText(`Salam, Kodariun! Bu dəstlər haqqında məlumat almaq istəyirəm:\n${lines.map(line => `${line.product.name} × ${line.quantity}`).join("\n")}`); setCopied(true); } catch { setMessage("Kopyalama mümkün olmadı. Seçimlərini Instagram-da bizə yaza bilərsən."); } }}>{copied ? "Seçimin kopyalandı" : "Seçimimi kopyala"}<Icon name={copied ? "check" : "arrow"} /></button><a className="text-link mt-5" href={social.shop} target="_blank" rel="noreferrer">Instagram-da danışaq ↗</a></> : <><p>Dəstlər stokda olduqda 5 dəqiqəlik ayrılır. Onlayn ödəniş hazırda bağlıdır.</p>
      {!hasSession || needsLogin ? <><p>Səbətiniz saxlanılıb. Rezervasiya üçün hesabınıza daxil olun.</p><Link className="button wide" href="/hesab?next=/sifaris">Daxil ol və davam et</Link></> :
      <button className="button wide" disabled={busy || currencies.size !== 1} onClick={reserve}>{busy ? "Stok yoxlanılır…" : "5 dəqiqəlik rezervasiya et"}<Icon name="arrow" /></button>}{currencies.size !== 1 && <p className="error-message mt-4">Fərqli valyutalı məhsulları ayrıca sifariş et.</p>}</>}{message && <p className="form-message error-message" role="alert">{message}</p>}</div></div>}</div>;
}
