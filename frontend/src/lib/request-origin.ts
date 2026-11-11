import type { NextRequest } from "next/server";

export function isSameOrigin(request: NextRequest): boolean {
  const origin = request.headers.get("origin");
  const host = request.headers.get("host");
  if (!origin || !host) return false;
  try {
    // Next dev may normalize nextUrl to localhost even when opened at 127.0.0.1.
    return new URL(origin).origin === new URL(`${request.nextUrl.protocol}//${host}`).origin;
  } catch { return false; }
}
