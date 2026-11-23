import Link from "next/link";
import type { Product } from "@/lib/types";
import { money } from "@/lib/content";
import { AddToCart } from "./store";
export function ProductCard({ product, index = 0 }: { product: Product; index?: number }) {
  return <article className="product-card"><Link className={`product-photo product-photo-${index % 4}`} href={`/magaza/${encodeURIComponent(product.id)}`}><img src={product.images[0] || "/brand/logo.jpg"} alt={product.name} loading="lazy" /><span className="photo-view">Dəstə bax</span></Link><div className="product-copy"><div><h3><Link href={`/magaza/${encodeURIComponent(product.id)}`}>{product.name}</Link></h3><p>{money(product.price, product.currency)}</p></div>{product.source === "live" ? <Link className="journal-link" href={`/magaza/${encodeURIComponent(product.id)}`} aria-label={product.name + " dəstinə bax"}>Dəstə bax</Link> : <AddToCart product={product} compact />}</div></article>;
}
