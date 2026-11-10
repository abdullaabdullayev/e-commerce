import type { Metadata } from "next";
import Link from "next/link";
import { catalog } from "@/lib/server";
import { CatalogExplorer } from "@/components/catalog";
export const metadata: Metadata = { title: "Özün qur dəstləri" };
export const dynamic = "force-dynamic";
export default async function Shop() {
  const data = await catalog();
  return <div className="shell content-bottom"><div className="page-heading"><div className="breadcrumbs"><Link href="/">Ana səhifə</Link><span>/</span><span>Mağaza</span></div><span className="eyebrow">KODARIUN / ÖZÜN QUR DƏSTLƏRİ</span><h1>Bir qutu.<br />Saysız kəşf.</h1><p>Detalları birləşdir, mexanizmi anla, öz rəngini əlavə et. Hər dəst yeni bir “bunu mən qurdum!” anıdır.</p></div>{data.mode === "showcase" && <p className="catalog-note">Instagram səhifəmizdən model seçimi göstərilir. Bu kataloqda qiymət və stok məlumatı hələ dərc edilməyib.</p>}<CatalogExplorer initial={data} /></div>;
}
