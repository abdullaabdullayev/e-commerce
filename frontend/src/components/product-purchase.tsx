"use client";

import { useEffect, useState } from "react";
import type { Product, Stock } from "@/lib/types";
import { AddToCart } from "./store";

export function ProductPurchase({ product }: { product: Product }) {
  const [stock, setStock] = useState<Stock | null>(null);
  const [unavailable, setUnavailable] = useState(false);
  useEffect(() => {
    if (product.source !== "live") return;
    const controller = new AbortController();
    async function refresh() {
      try {
        const response = await fetch(`/api/backend/stock/${encodeURIComponent(product.sku)}`, { signal: controller.signal, cache: "no-store" });
        if (!response.ok) throw new Error("Stock unavailable");
        const data: Stock = await response.json();
        if (!controller.signal.aborted) { setStock(data); setUnavailable(false); }
      } catch {
        if (!controller.signal.aborted) setUnavailable(true);
      }
    }
    setStock(null); setUnavailable(false);
    void refresh();
    const interval = setInterval(() => { if (document.visibilityState === "visible") void refresh(); }, 15000);
    return () => { controller.abort(); clearInterval(interval); };
  }, [product.sku, product.source]);

  if (product.source === "showcase") return <><p className="stock-note">Qiymət, dəstin tərkibi və mövcudluq üçün bizimlə danış.</p><AddToCart product={product} /></>;
  const disabled = unavailable || !stock || stock.available <= 0;
  return <><p className="stock-note" role="status">{unavailable ? "Stok məlumatını hazırda yoxlamaq mümkün deyil." : !stock ? "Mövcudluq yoxlanılır…" : stock.available > 0 ? `${stock.available} ədəd satışa açıqdır` : "Hazırda stokda yoxdur"}</p><AddToCart product={product} disabled={disabled} label={stock?.available === 0 ? "Stokda yoxdur" : "Səbətə əlavə et"} /></>;
}
