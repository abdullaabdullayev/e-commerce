import "server-only";
import { cookies } from "next/headers";
import { showcase } from "./content";
import type { Catalog, CustomerOrder, Product } from "./types";

export const gateway = () => (process.env.GATEWAY_URL || "http://127.0.0.1:8080").replace(/\/$/, "");
export const isShowcase = () => process.env.DEMO_CATALOG !== "false";

export async function catalog(): Promise<Catalog> {
  if (isShowcase()) return showcase;
  try {
    const options = { cache: "no-store" as const, signal: AbortSignal.timeout(5000) };
    const [products, categories] = await Promise.all([
      fetch(`${gateway()}/api/v1/products?size=12&currency=AZN`, options),
      fetch(`${gateway()}/api/v1/categories?size=100`, options),
    ]);
    if (!products.ok || !categories.ok) throw new Error("catalog unavailable");
    const p = await products.json(), c = await categories.json();
    return { mode: "live", products: (p.content || []).map((item: Product) => ({ ...item, source: "live", price: Number(item.price), images: item.images || [], attributes: item.attributes || {} })), categories: c.content || [], totalElements: p.totalElements || 0, totalPages: p.totalPages || 0 };
  } catch {
    return { mode: "live", products: [], categories: [], unavailable: true };
  }
}

export async function getProduct(id: string): Promise<{ product?: Product; unavailable?: boolean }> {
  if (isShowcase()) return { product: showcase.products.find(p => p.id === id) };
  try {
    const response = await fetch(`${gateway()}/api/v1/products/${encodeURIComponent(id)}`, { cache: "no-store", signal: AbortSignal.timeout(5000) });
    if (response.status === 404) return {};
    if (!response.ok) throw new Error("product unavailable");
    const p = await response.json();
    return { product: { ...p, price: Number(p.price), images: p.images || [], attributes: p.attributes || {}, source: "live" } };
  } catch { return { unavailable: true }; }
}

export async function customerOrders(): Promise<{ orders: CustomerOrder[]; loggedIn: boolean; unavailable?: boolean }> {
  const token = (await cookies()).get("kodariun_access")?.value;
  if (!token) return { orders: [], loggedIn: false };
  try {
    const response = await fetch(`${gateway()}/api/v1/orders`, { headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(5000) });
    if (response.status === 401 || response.status === 403) return { orders: [], loggedIn: false };
    if (!response.ok) throw new Error("orders unavailable");
    return { orders: await response.json(), loggedIn: true };
  } catch { return { orders: [], loggedIn: true, unavailable: true }; }
}
