import type { Metadata } from "next";
import { isShowcase } from "@/lib/server";
import { CheckoutView } from "@/components/checkout";
export const metadata: Metadata = { title: "Səbət və rezervasiya" };
export const dynamic = "force-dynamic";
export default function CheckoutPage() { return <CheckoutView showcaseMode={isShowcase()} />; }
