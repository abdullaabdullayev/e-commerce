export type Product = {
  id: string; name: string; description: string; sku: string; categoryId: string;
  price: number | null; currency: string; images: string[]; active: boolean;
  attributes: Record<string, unknown>; source: "live" | "showcase";
};
export type Category = { id: string; name: string; slug: string };
export type Catalog = { products: Product[]; categories: Category[]; mode: "live" | "showcase"; unavailable?: boolean; totalElements?: number; totalPages?: number };
export type CartLine = { product: Product; quantity: number };
export type CustomerOrder = {
  id: string; status: string; amount: number; currency: string; expiresAt: string | null;
  createdAt: string; items: { productId: string; name: string; quantity: number; unitPrice: number }[];
};
