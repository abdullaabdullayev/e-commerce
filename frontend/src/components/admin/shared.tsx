"use client";
import { useEffect, useState, type FormEvent, type ReactNode } from "react";

export type Course = { id: string; title: string; description: string; minAge: number; maxAge: number; level: string; status: string };
export type Account = { accountId: number; email?: string; username?: string; roles: string[]; active: boolean };
export type Page<T> = { content: T[]; hasNext: boolean };
export type Lesson = { id: string; title: string; objective: string; durationMinutes: number; teacherNotes: string; archived: boolean };
export type Material = { id: string; title: string; kind: string; url?: string };
export type Syllabus = { course: Course; teacherAccountIds: number[]; lessons: { lesson: Lesson; materials: Material[] }[] };
export const labels: Record<string,string> = { ACTIVE:"Aktiv", COMPLETED:"Tamamlayıb", LEFT:"Ayrılıb", DRAFT:"Qaralama", PUBLISHED:"Yayımda", ARCHIVED:"Arxiv", TEACHER:"Müəllim", STUDENT:"Tələbə", ADMIN:"Admin", CUSTOMER:"Müştəri" };
export async function api<T>(path: string, method = "GET", body?: object, signal?: AbortSignal): Promise<T> {
  const file = body instanceof FormData;
  const res = await fetch("/api/admin/"+path, { method, cache:"no-store", signal,
    headers: body && !file ? {"Content-Type":"application/json"} : undefined,
    body: file ? body : body ? JSON.stringify(body) : undefined });
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw new Error(res.status === 401 ? "Sessiya bitib. Hesaba yenidən daxil olun." : problem.detail || "Əməliyyat alınmadı ("+res.status+").");
  }
  return res.status === 204 ? undefined as T : await res.json();
}
export function useData<T>(path: string, revision: number): {data?:T;error?:string} {
  const key = path+"#"+revision;
  const [state, set] = useState<{key:string; data?:T; error?:string}>();
  useEffect(() => {
    const controller = new AbortController();
    api<T>(path,"GET",undefined,controller.signal).then(data => set({key,data})).catch(e => {
      if (!controller.signal.aborted) set({key,error:e.message});
    });
    return () => controller.abort();
  }, [path,key]);
  return state?.key === key ? state : {};
}
export function Pending({error}:{error?:string}) { return <p role="status">{error || "Yüklənir…"}</p>; }
export function Pager({page,next,set}:{page:number;next:boolean;set:(v:number)=>void}) {
  if (!page && !next) return null;
  return <nav className="ap-pager" aria-label="Səhifələr"><button disabled={!page} onClick={()=>set(page-1)}>← Əvvəlki</button><span>{page+1}</span><button disabled={!next} onClick={()=>set(page+1)}>Sonrakı →</button></nav>;
}
export function Action({children, submit, button, done, confirm, reset=false}:{children?:ReactNode;submit:(f:FormData)=>Promise<unknown>;button:string;done:()=>void;confirm?:string;reset?:boolean}) {
  const [busy,setBusy]=useState(false), [error,setError]=useState("");
  async function run(e:FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (busy || (confirm && !window.confirm(confirm))) return;
    const form=e.currentTarget, data=new FormData(form);
    setBusy(true); setError("");
    try { await submit(data); if(reset) form.reset(); done(); }
    catch(err) {setError(err instanceof Error ? err.message : "Sorğu alınmadı.");}
    finally {setBusy(false);}
  }
  return <form onSubmit={run} className="ap-form"><fieldset disabled={busy}>{children}<button className="ap-submit" type="submit">{busy?"Gözləyin…":button}</button></fieldset>{error && <p role="alert">{error}</p>}</form>;
}
export const str=(f:FormData,k:string)=>String(f.get(k)??"").trim();
export function Field({label,name,value,type="text",min,max,limit,optional=false}:{label:string;name:string;value?:string|number;type?:string;min?:number;max?:number;limit?:number;optional?:boolean}) {
  return <label>{label}<input name={name} type={type} defaultValue={value} required={!optional} min={min} max={max} maxLength={limit} autoComplete={type==="password"?"new-password":undefined}/></label>;
}
export function Notes({label,name,value,limit}:{label:string;name:string;value?:string;limit:number}) {return <label>{label}<textarea name={name} defaultValue={value} maxLength={limit} rows={2}/></label>;}
