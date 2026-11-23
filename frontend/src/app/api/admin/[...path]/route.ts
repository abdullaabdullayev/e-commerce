import { NextRequest, NextResponse } from "next/server";
import { adminSession } from "@/lib/admin-session";
import { gateway } from "@/lib/server";
import { isSameOrigin } from "@/lib/request-origin";

const id = "[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";
const course = `courses/${id}`;
const student = `${course}/students/[1-9][0-9]*`;
const children = "parents/[1-9][0-9]*/children";
const child = `${children}/[1-9][0-9]*`;
const routes: Record<string, RegExp> = Object.fromEntries(Object.entries({
  GET: [children, "accounts", "accounts/[1-9][0-9]*", "courses", `${course}/syllabus`, `${course}/students`, `${student}/points`, `course-materials/${id}/download`],
  POST: ["parent-invitations", "accounts", "courses", `${course}/lessons`, `${student}/points`, `${student}/points/${id}/corrections`, `course-materials/lessons/${id}/(?:files|links)`],
  PUT: ["accounts/[1-9][0-9]*/roles", child, course, `${course}/(?:status|lesson-order)`, `${course}/lessons/${id}`, `${course}/teachers/[1-9][0-9]*`, student],
  DELETE: [child, `${course}/teachers/[1-9][0-9]*`, `${course}/lessons/${id}`, `course-materials/${id}`],
}).map(([method, paths]) => [method, new RegExp(`^(?:${paths.join("|")})$`)]));
function error(status: number, detail: string) {
  return NextResponse.json({ detail }, { status, headers: { "Cache-Control": "no-store" } });
}
async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const path = (await context.params).path.join("/");
  if (!routes[request.method]?.test(path)) return error(404, "Bu əməliyyat mövcud deyil.");
  if (request.method !== "GET" && !isSameOrigin(request)) return error(403, "Sorğu mənbəyi qəbul edilmir.");
  const session = await adminSession();
  if (session.status !== 200) return error(session.status, session.status === 401 ? "Sessiya bitib. Yenidən daxil olun." : session.status === 403 ? "Admin səlahiyyəti tələb olunur." : "Hesab xidməti cavab vermir.");
  const headers = new Headers({ Authorization: `Bearer ${session.token}` });
  const type = request.headers.get("content-type");
  if (type) headers.set("Content-Type", type);
  try {
    const options: RequestInit & { duplex: "half" } = {
      method: request.method, headers, body: request.body, duplex: "half",
      cache: "no-store", redirect: "error", signal: AbortSignal.timeout(90000),
    };
    const result = await fetch(`${gateway()}/api/v1/admin/${path}${request.nextUrl.search}`, options);
    const out = new Headers({ "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" });
    for (const key of ["content-type", "content-disposition"]) {
      const value = result.headers.get(key);
      if (value) out.set(key, value);
    }
    return new Response(result.body, { status: result.status, headers: out });
  } catch { return error(503, "Gateway ilə əlaqə qurulmadı."); }
}
export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const DELETE = proxy;
