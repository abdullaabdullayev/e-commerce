import "server-only";
import { cookies } from "next/headers";
import { gateway } from "./server";

export async function accountSession() {
  const token = (await cookies()).get("kodariun_access")?.value;
  if (!token) return { status: 401 } as const;
  try {
    const response = await fetch(`${gateway()}/api/v1/auth/me`, {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(5000),
    });
    if ([401, 403].includes(response.status)) return { status: 401 } as const;
    if (!response.ok) return { status: 503 } as const;
    const account = await response.json();
    if (!account.active || !Array.isArray(account.roles)) return { status: 403 } as const;
    return { status: 200, token, roles: account.roles as string[], label: String(account.email || account.username) } as const;
  } catch { return { status: 503 } as const; }
}

export async function adminSession() {
  const session = await accountSession();
  if (session.status === 200 && !session.roles.includes("ADMIN")) return { status: 403 } as const;
  return session;
}
