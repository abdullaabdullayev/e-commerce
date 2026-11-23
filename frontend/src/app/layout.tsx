import type { Metadata } from "next";
import { Header, Footer } from "@/components/chrome";
import { StoreProvider } from "@/components/store";
import { isShowcase } from "@/lib/server";
import "./globals.css";
import "./studio.css";
export const metadata: Metadata = {
  title: { default: "Kodariun — Uşaqlar üçün ixtira emalatxanası", template: "%s | Kodariun" },
  description: "Kodariun: uşaqlar üçün STEAM emalatxanaları, LEGO və Arduino robototexnika dərsləri, yerli özün qur dəstləri.",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="az" data-scroll-behavior="smooth"><body><a href="#main" className="skip-link">Məzmuna keç</a><StoreProvider mode={isShowcase() ? "showcase" : "live"}><Header /><main id="main">{children}</main><Footer /></StoreProvider></body></html>;
}
