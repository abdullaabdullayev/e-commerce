import type { Category, Product } from "./types";

// Spring Data's VIA_DTO response puts pagination inside `page`.
export type CatalogPage<T> = {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
};

export type CatalogProduct = Omit<Product, "source" | "price"> & { price: number | string };

export function liveProduct(product: CatalogProduct): Product {
  const price = Number(product.price);
  if (!Number.isFinite(price) || price <= 0) throw new Error("Invalid catalog price");
  return { ...product, price, source: "live", images: product.images || [], attributes: product.attributes || {} };
}

export function liveCatalog(products: CatalogPage<CatalogProduct>, categories: CatalogPage<Category>) {
  return {
    mode: "live" as const,
    products: products.content.map(liveProduct),
    categories: categories.content,
    totalElements: products.page.totalElements,
    totalPages: products.page.totalPages,
  };
}
