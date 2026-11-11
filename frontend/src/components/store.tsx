"use client";
import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import type { CartLine, Product } from "@/lib/types";
import { money } from "@/lib/content";
import { Icon } from "./icons";

type Store = { lines: CartLine[]; add: (product: Product, quantity?: number) => void; quantity: (id: string, value: number) => void; clear: () => void; open: () => void };
const Context = createContext<Store | null>(null);
export const useStore = () => { const store = useContext(Context); if (!store) throw new Error("Missing store"); return store; };
export function StoreProvider({ children, mode }: { children: ReactNode; mode: Product["source"] }) {
  const [lines, setLines] = useState<CartLine[]>([]), [ready, setReady] = useState(false), [opened, setOpened] = useState(false), [notice, setNotice] = useState("");
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    try {
      const saved: unknown = JSON.parse(localStorage.getItem(`kodariun-cart-${mode}-v1`) || "[]");
      if (Array.isArray(saved)) setLines(saved.filter((line: CartLine) => line?.product?.id && typeof line.product.name === "string" && Array.isArray(line.product.images) && ["showcase", "live"].includes(line.product.source) && (line.product.price === null || (typeof line.product.price === "number" && Number.isFinite(line.product.price) && line.product.price > 0)) && Number.isInteger(line.quantity) && line.quantity > 0 && line.quantity <= 99).slice(0, 50));
    } catch { /* An invalid local cart can be replaced. */ }
    setReady(true);
  }, [mode]);
  useEffect(() => { if (ready) { try { localStorage.setItem(`kodariun-cart-${mode}-v1`, JSON.stringify(lines)); } catch { /* Private mode may disallow persistence. */ } } }, [lines, ready, mode]);
  useEffect(() => {
    if (opened) dialog.current?.showModal(); else dialog.current?.close();
    if (!opened) return;
    const before = document.body.style.overflow; document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = before; };
  }, [opened]);
  useEffect(() => { if (!notice) return; const id = setTimeout(() => setNotice(""), 3500); return () => clearTimeout(id); }, [notice]);
  const add = (product: Product, amount = 1) => {
    setLines(current => {
      const existing = current.find(line => line.product.id === product.id);
      return existing ? current.map(line => line.product.id === product.id ? { ...line, quantity: Math.min(99, line.quantity + amount) } : line) : [...current, { product, quantity: amount }];
    });
    setNotice(`${product.name} səbətə əlavə edildi`);
  };
  const quantity = (id: string, value: number) => setLines(current => current.map(line => line.product.id === id ? { ...line, quantity: Math.min(99, value) } : line).filter(line => line.quantity > 0));
  const known = lines.length > 0 && lines.every(line => line.product.price != null && line.product.currency === lines[0].product.currency);
  const total = lines.reduce((sum, line) => sum + (line.product.price || 0) * line.quantity, 0);
  return <Context.Provider value={{ lines, add, quantity, clear: () => setLines([]), open: () => setOpened(true) }}>
    {children}
    {notice && <div className="toast" role="status"><Icon name="check" /><span>{notice}</span><button onClick={() => setOpened(true)}>Səbətə bax</button></div>}
    <dialog ref={dialog} className="cart-dialog" aria-labelledby="cart-title" onCancel={() => setOpened(false)}>
      <div className="cart-top"><div><span className="eyebrow">SEÇDİYİN KƏŞFLƏR</span><h2 id="cart-title">Səbətin <span>({lines.reduce((n, l) => n + l.quantity, 0)})</span></h2></div><button className="icon-button" aria-label="Səbəti bağla" onClick={() => setOpened(false)}><Icon name="close" /></button></div>
      {lines.length === 0 ? <div className="empty-cart"><Icon name="bag" width="48" height="48" /><h3>İlk kəşfini seç.</h3><p>Özün qur dəstlərimizə bax, marağını oyadan modeli səbətinə əlavə et.</p><Link className="button" href="/magaza" onClick={() => setOpened(false)}>Mağazaya keç <Icon name="arrow" /></Link></div> : <>
        <div className="cart-lines">{lines.map(({ product, quantity: count }) => <div className="cart-line" key={product.id}><img src={product.images[0] || "/brand/logo.jpg"} alt="" /><div><Link href={`/magaza/${encodeURIComponent(product.id)}`} onClick={() => setOpened(false)}>{product.name}</Link><small>{money(product.price, product.currency)}</small><div className="quantity"><button aria-label={`${product.name}: sayını azalt`} onClick={() => quantity(product.id, count - 1)}><Icon name="minus" width="14" /></button><span>{count}</span><button aria-label={`${product.name}: sayını artır`} disabled={count >= 99} onClick={() => quantity(product.id, count + 1)}><Icon name="plus" width="14" /></button></div></div><button className="remove-button" onClick={() => quantity(product.id, 0)} aria-label={`${product.name}: səbətdən sil`}><Icon name="close" width="16" /></button></div>)}</div>
        <div className="cart-bottom"><div className="total-row"><span>Məhsulların cəmi</span><strong>{known ? money(total, lines[0].product.currency) : "Dəqiqləşdiriləcək"}</strong></div><p>Səbətə əlavə etmək stoku rezerv etmir.</p><Link href="/sifaris" className="button wide" onClick={() => setOpened(false)}>Davam et <Icon name="arrow" /></Link></div>
      </>}
    </dialog>
  </Context.Provider>;
}
export function AddToCart({ product, label = "Səbətə əlavə et", compact = false, disabled = false }: { product: Product; label?: string; compact?: boolean; disabled?: boolean }) {
  const { add } = useStore();
  return <button className={compact ? "product-add" : "button"} disabled={disabled} aria-label={`${product.name} — səbətə əlavə et`} onClick={() => add(product)}>{!compact && label}<Icon name={compact ? "plus" : "bag"} /></button>;
}
