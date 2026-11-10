import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { gateway } from "@/lib/server";

export async function POST(request: NextRequest, context: { params: Promise<{ action: string }> }) {
  if (request.headers.get("origin") !== request.nextUrl.origin) return NextResponse.json({ detail: "Sorğu mənbəyi qəbul edilmir." }, { status: 403 });
  const { action } = await context.params;
  const jar = await cookies();
  if (action === "logout") { jar.delete("kodariun_access"); return NextResponse.json({ ok: true }); }
  if (!["login", "register"].includes(action)) return NextResponse.json({ detail: "Endpoint mövcud deyil." }, { status: 404 });
  try {
    const response = await fetch(`${gateway()}/api/v1/auth/${action}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: await request.text(), cache: "no-store", signal: AbortSignal.timeout(10000) });
    if (!response.ok) return NextResponse.json({ detail: action === "login" ? "Email və ya şifrə düzgün deyil, yaxud xidmət hazırda əlçatan deyil." : "Qeydiyyat tamamlanmadı. Emailin istifadə edilmədiyini yoxlayın." }, { status: response.status });
    const data = await response.json();
    if (action === "login") {
      if (typeof data.accessToken !== "string") throw new Error("missing access token");
      const payload = JSON.parse(Buffer.from(data.accessToken.split(".")[1], "base64url").toString());
      const maxAge = Math.max(0, Math.min(86400, Number(payload.exp) - Math.floor(Date.now() / 1000)));
      if (!Number.isFinite(maxAge) || maxAge === 0) throw new Error("expired token");
      jar.set("kodariun_access", data.accessToken, { httpOnly: true, sameSite: "lax", secure: process.env.COOKIE_SECURE === "true" || request.nextUrl.protocol === "https:", path: "/", maxAge });
    }
    return NextResponse.json({ ok: true }, { status: action === "register" ? 201 : 200 });
  } catch { return NextResponse.json({ detail: "Hazırda hesab xidmətinə qoşulmaq mümkün deyil. Yenidən cəhd edin." }, { status: 503 }); }
}
