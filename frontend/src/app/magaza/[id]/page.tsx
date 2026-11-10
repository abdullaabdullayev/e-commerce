import Link from "next/link";
import { notFound } from "next/navigation";
import { getProduct } from "@/lib/server";
import { money } from "@/lib/content";
import { AddToCart } from "@/components/store";
import { Availability } from "@/components/catalog";
export const dynamic = "force-dynamic";
export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params, result = await getProduct(id);
  if (result.unavailable) return <div className="shell section"><div className="empty-state"><h2>Bu dəstə hazırda baxmaq mümkün deyil.</h2><p>Kataloq xidməti ilə əlaqə qurulmadı. Bir az sonra yenidən yoxla.</p><Link className="text-link" href="/magaza">Mağazaya qayıt →</Link></div></div>;
  if (!result.product) notFound();
  const product = result.product;
  return <div className="shell"><div className="page-heading"><div className="breadcrumbs"><Link href="/">Ana səhifə</Link><span>/</span><Link href="/magaza">Mağaza</Link><span>/</span><span>{product.name}</span></div></div><div className="product-detail"><div className="detail-photo"><img src={product.images[0] || "/brand/logo.jpg"} alt={product.name} /></div><div className="detail-info"><span className="eyebrow">KODARIUN / QURMAQ + KƏŞF ETMƏK</span><h1>{product.name}</h1><div className="detail-price">{money(product.price, product.currency)}</div><p className="detail-description">{product.description}</p>{product.source === "live" ? <Availability sku={product.sku} /> : <p className="stock-note">Qiymət, dəstin tərkibi və mövcudluq üçün bizimlə danış.</p>}<AddToCart product={product} /><p className="text-xs text-ink/60">Səbətə əlavə etmək məhsulu rezerv etmir. Kiçik detallar və elektronika ilə işləyərkən böyüklərin iştirakı vacibdir.</p><dl className="specs">{Object.entries(product.attributes).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{typeof value === "object" ? JSON.stringify(value) : String(value)}</dd></div>)}</dl><Link href="/elaqe" className="text-link mt-5">Bu dəst haqqında sual ver ↗</Link></div></div></div>;
}
