import "server-only";
import { gateway } from "./server";

export async function learningRequest<T>(token: string, path: string, method = "GET", body?: unknown): Promise<T> {
  const response = await fetch(gateway() + "/api/v1/" + path, {
    method, cache: "no-store", redirect: "error", signal: AbortSignal.timeout(10000),
    headers: { Authorization: "Bearer " + token, ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    const detail = await response.json().catch(() => null);
    throw new Error(detail?.detail || "Sorğu tamamlanmadı (" + response.status + ").");
  }
  return response.status === 204 ? undefined as T : response.json();
}

export const uuid = /^[\da-f]{8}(?:-[\da-f]{4}){3}-[\da-f]{12}$/i;
export function pageNumber(value?: string) {
  return value && /^\d{1,6}$/.test(value) ? Math.min(100000, Number(value)) : 0;
}
export type Slice<T> = { content: T[]; hasNext: boolean };
export type LessonReport = {
  id: string; studentId: number; courseTitle: string; heldAt: string; topic: string;
  attended: boolean; learned: string; participation: string; recommendation: string; publishedAt?: string;
};
export function lessonDate(value: string) {
  return new Intl.DateTimeFormat("az-AZ", { timeZone: "Asia/Baku", dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
