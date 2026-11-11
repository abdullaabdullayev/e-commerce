import type { Metadata } from "next";
import { Header, Footer } from "@/components/chrome";
import { StoreProvider } from "@/components/store";
import { isShowcase } from "@/lib/server";
import "./globals.css";
export const metadata: Metadata = {
  title: { default: "Kodariun — Özün qur. Özün yarat.", template: "%s | Kodariun" },
  description: "Kodariun: uşaqlar üçün STEAM emalatxanaları, LEGO və Arduino robototexnika dərsləri, yerli özün qur dəstləri.",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="az"><body><a href="#main" className="skip-link">Məzmuna keç</a><StoreProvider mode={isShowcase() ? "showcase" : "live"}><Header /><main id="main">{children}</main><Footer /></StoreProvider></body></html>;
}
