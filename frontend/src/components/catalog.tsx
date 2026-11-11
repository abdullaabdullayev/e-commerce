"use client";
import { useEffect, useMemo, useState } from "react";
import type { Catalog } from "@/lib/types";
import { liveProduct, type CatalogPage, type CatalogProduct } from "@/lib/catalog-data";
import { ProductCard } from "./product-card";
import { Icon } from "./icons";

export function CatalogExplorer({ initial }: { initial: Catalog }) {
  const [q, setQ] = useState(""), [category, setCategory] = useState(""), [sort, setSort] = useState("createdAt,desc"), [page, setPage] = useState(0);
  const [products, setProducts] = useState(initial.products), [total, setTotal] = useState(initial.totalElements ?? initial.products.length), [pages, setPages] = useState(initial.totalPages ?? 0);
  const [busy, setBusy] = useState(false), [error, setError] = useState(initial.unavailable || false);
  useEffect(() => {
    if (initial.mode !== "live") return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setBusy(true); setError(false);
      try {
        const params = new URLSearchParams({ page: String(page), size: "12", currency: "AZN", sort });
        if (q.trim()) params.set("q", q.trim());
        if (category) params.set("categoryId", category);
        const response = await fetch(`/api/backend/products?${params}`, { signal: controller.signal });
        if (!response.ok) throw new Error("unavailable");
        const data: CatalogPage<CatalogProduct> = await response.json();
        if (controller.signal.aborted) return;
        setProducts(data.content.map(liveProduct));
        setTotal(data.page.totalElements); setPages(data.page.totalPages);
      } catch { if (!controller.signal.aborted) setError(true); }
      finally { if (!controller.signal.aborted) setBusy(false); }
    }, 250);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [q, category, sort, page, initial.mode]);
  const displayed = useMemo(() => initial.mode === "live" ? products : initial.products.filter(p => (!category || p.categoryId === category) && `${p.name} ${p.description}`.toLocaleLowerCase("az").includes(q.toLocaleLowerCase("az"))), [initial, products, q, category]);
  return <><div className="shop-tools"><div className="filter-buttons"><button aria-pressed={!category} onClick={() => { setCategory(""); setPage(0); }}>Bütün dəstlər</button>{initial.categories.map(c => <button key={c.id} aria-pressed={category === c.id} onClick={() => { setCategory(c.id); setPage(0); }}>{c.name}</button>)}</div><label className="search-field"><Icon name="search" width="17" /><input aria-label="Dəst axtar" placeholder="Bir kəşf axtar…" value={q} maxLength={100} onChange={e => { setQ(e.target.value); setPage(0); }} /></label></div><div className="result-note"><span>{busy ? "Axtarılır…" : `${initial.mode === "live" ? total : displayed.length} dəst`}</span>{initial.mode === "live" ? <select aria-label="Məhsulları sırala" className="sort-select" value={sort} onChange={e => { setSort(e.target.value); setPage(0); }}><option value="createdAt,desc">Ən yenilər</option><option value="price,asc">Qiymət: aşağıdan yuxarı</option><option value="price,desc">Qiymət: yuxarıdan aşağı</option></select> : <span>Qiymət və mövcudluq əlaqə zamanı dəqiqləşdirilir.</span>}</div>{error ? <div className="notice" role="alert">Kataloqa qoşulmaq mümkün olmadı. Bir az sonra səhifəni yenilə və ya <a href="/elaqe">bizimlə əlaqə saxla.</a></div> : displayed.length === 0 ? <div className="empty-state"><h2>{q || category ? "Uyğun dəst tapılmadı." : "Yeni dəstlər yoldadır."}</h2><p>{q || category ? "Başqa söz və ya kateqoriya ilə yenidən yoxla." : "Məhsullar kataloqa əlavə edildikcə burada görünəcək."}</p><button className="secondary-button mt-5" onClick={() => { setQ(""); setCategory(""); setPage(0); }}>Filtrləri təmizlə</button></div> : <div className="product-grid" aria-busy={busy} style={{ opacity: busy ? .5 : 1 }}>{displayed.map((product, index) => <ProductCard key={product.id} product={product} index={index} />)}</div>}{initial.mode === "live" && pages > 1 && <div className="flex items-center justify-center gap-6 mt-12"><button className="secondary-button" disabled={page === 0 || busy} onClick={() => setPage(p => p - 1)}>← Əvvəlki</button><span className="eyebrow">{page + 1} / {pages}</span><button className="secondary-button" disabled={page + 1 >= pages || busy} onClick={() => setPage(p => p + 1)}>Növbəti →</button></div>}</>;
}
