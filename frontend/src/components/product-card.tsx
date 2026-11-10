import Link from "next/link";
import type { Product } from "@/lib/types";
import { money } from "@/lib/content";
import { AddToCart } from "./store";
export function ProductCard({ product, index = 0 }: { product: Product; index?: number }) {
  return <article className="product-card"><Link className={`product-photo product-photo-${index % 4}`} href={`/magaza/${encodeURIComponent(product.id)}`}><span className="product-index">K / {String(index + 1).padStart(2, "0")}</span><img src={product.images[0] || "/brand/logo.jpg"} alt={product.name} loading="lazy" /><span className="photo-view">Yaxından bax ↗</span></Link><div className="product-copy"><div><span className="eyebrow">ÖZÜN QUR DƏSTİ</span><h3><Link href={`/magaza/${encodeURIComponent(product.id)}`}>{product.name}</Link></h3><p>{money(product.price, product.currency)}</p></div><AddToCart product={product} compact /></div></article>;
}
