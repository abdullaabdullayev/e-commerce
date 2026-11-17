import "server-only";
import { cookies } from "next/headers";
import { gateway } from "./server";

export async function adminSession() {
  const token = (await cookies()).get("kodariun_access")?.value;
  if (!token) return { status: 401 } as const;
  try {
    const response = await fetch(`${gateway()}/api/v1/auth/me`, {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(5000),
    });
    if ([401, 403].includes(response.status)) return { status: 401 } as const;
    if (!response.ok) return { status: 503 } as const;
    const account = await response.json();
    if (!account.active || !account.roles?.includes("ADMIN")) return { status: 403 } as const;
    return { status: 200, token, label: String(account.email || account.username) } as const;
  } catch { return { status: 503 } as const; }
}
