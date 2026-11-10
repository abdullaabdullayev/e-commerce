import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { gateway, isShowcase } from "@/lib/server";

export const dynamic = "force-dynamic";
async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const segments = (await context.params).path;
  const path = segments.join("/");
  const read = request.method === "GET" && /^(products(?:\/[^/]+)?|categories|stock\/[^/]+|orders(?:\/[a-f0-9-]+)?)$/.test(path);
  const write = request.method === "POST" && /^(orders|orders\/[a-f0-9-]+\/cancel)$/.test(path);
  if (!read && !write) return NextResponse.json({ detail: "Endpoint mövcud deyil." }, { status: 404 });
  if (write && request.headers.get("origin") !== request.nextUrl.origin) return NextResponse.json({ detail: "Sorğu mənbəyi qəbul edilmir." }, { status: 403 });
  if (write && isShowcase()) return NextResponse.json({ detail: "Nümunə kataloqda real sifariş yaradılmır." }, { status: 409 });
  const jar = await cookies();
  const token = jar.get("kodariun_access")?.value;
  if (path.startsWith("orders") && !token) return NextResponse.json({ detail: "Davam etmək üçün hesabınıza daxil olun." }, { status: 401 });
  try {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (token) headers.Authorization = `Bearer ${token}`;
    const idempotency = request.headers.get("idempotency-key");
    if (idempotency) headers["Idempotency-Key"] = idempotency;
    const response = await fetch(`${gateway()}/api/v1/${segments.map(encodeURIComponent).join("/")}${request.nextUrl.search}`, {
      method: request.method, headers, body: write ? await request.text() : undefined,
      cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    const text = await response.text();
    if (response.status === 401) jar.delete("kodariun_access");
    return new NextResponse(text || null, { status: response.status, headers: { "Content-Type": response.headers.get("content-type") || "application/json", "Cache-Control": "no-store" } });
  } catch { return NextResponse.json({ detail: "Hazırda serverə qoşulmaq mümkün olmadı. Bir az sonra yenidən yoxlayın." }, { status: 503 }); }
}
export const GET = proxy;
export const POST = proxy;
