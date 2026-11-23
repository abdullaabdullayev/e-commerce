"use client";
import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { Icon } from "../icons";

export type Course = { id: string; title: string; description: string; minAge: number; maxAge: number; level: string; status: string };
export type Account = { accountId: number; email?: string; username?: string; roles: string[]; active: boolean };
export type Page<T> = { content: T[]; hasNext: boolean };
export type Lesson = { id: string; title: string; objective: string; durationMinutes: number; teacherNotes: string; archived: boolean };
export type Material = { id: string; title: string; kind: string; url?: string };
export type Syllabus = { course: Course; teacherAccountIds: number[]; lessons: { lesson: Lesson; materials: Material[] }[] };
export const labels: Record<string,string> = { ACTIVE:"Aktiv", COMPLETED:"Tamamlayıb", LEFT:"Ayrılıb", DRAFT:"Qaralama", PUBLISHED:"Yayımda", ARCHIVED:"Arxiv", TEACHER:"Müəllim", STUDENT:"Tələbə", PARENT:"Valideyn", ADMIN:"Admin", CUSTOMER:"Müştəri" };
const adminErrors: Record<string, string> = {
  "The last administrator cannot be removed": "Son administratorun admin rolunu silmək mümkün deyil.",
  "Email or username already exists": "Bu e-poçt və ya istifadəçi adı ilə hesab artıq var. Hesablar bölməsində axtarın.",
  "Account already exists": "Bu e-poçtla hesab artıq yaradılıb. Hesablar bölməsində rollarını yoxlayın.",
  "Account profile is not ready yet": "Hesab profili hazırlanır. Bir az sonra yenidən cəhd edin.",
  "Account must be active and have role STUDENT": "Aktiv tələbə hesabı seçin.",
  "Account must be active and have role TEACHER": "Aktiv müəllim hesabı seçin.",
  "Course is archived": "Kurs arxivləndiyi üçün bu dəyişikliyi saxlamaq mümkün deyil.",
  "Lesson is archived": "Dərs arxivləndiyi üçün bu dəyişikliyi saxlamaq mümkün deyil.",
  "The award already has the requested value": "Bu mükafatın xalı artıq daxil etdiyiniz qiymətə bərabərdir.",
  "Points require an active enrollment in a published course": "Xal vermək üçün kurs yayımda, tələbənin qeydiyyatı isə aktiv olmalıdır.",
  "Use an HTTPS link without embedded credentials": "https:// ilə başlayan və giriş məlumatları olmayan keçid daxil edin.",
};
export async function api<T>(path: string, method = "GET", body?: object, signal?: AbortSignal): Promise<T> {
  const file = body instanceof FormData;
  const res = await fetch("/api/admin/"+path, { method, cache:"no-store", signal,
    headers: body && !file ? {"Content-Type":"application/json"} : undefined,
    body: file ? body : body ? JSON.stringify(body) : undefined });
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    if (res.status === 401) throw new Error("Sessiya bitib. Hesaba yenidən daxil olun.");
    if (res.status >= 500) throw new Error("Xidmət hazırda əlçatan deyil. Bir az sonra yenidən cəhd edin.");
    throw new Error(adminErrors[problem.detail] || problem.detail || "Əməliyyat alınmadı ("+res.status+").");
  }
  return res.status === 204 ? undefined as T : await res.json();
}
export function useData<T>(path: string, revision: number): {data?:T;error?:string} {
  const [state, set] = useState<{path:string; data?:T; error?:string}>();
  useEffect(() => {
    const controller = new AbortController();
    api<T>(path,"GET",undefined,controller.signal).then(data => {
      if (!controller.signal.aborted) set({path,data});
    }).catch(e => {
      if (!controller.signal.aborted) set({path,error:e.message});
    });
    return () => controller.abort();
  }, [path,revision]);
  return state?.path === path ? state : {};
}
export function Pending({error}:{error?:string}) {
  return <div className="ap-pending" role="status">{error ? <>{error} <button type="button" onClick={() => window.location.reload()}>Yenilə</button></> : "Yüklənir…"}</div>;
}
export function SearchBox({label, value, onSearch, limit=254}: {
  label: string; value: string; onSearch: (query: string) => void; limit?: number;
}) {
  return <form className="ap-search" role="search" aria-label={label} onSubmit={event => {
    event.preventDefault();
    onSearch(str(new FormData(event.currentTarget), "q"));
  }}><Icon name="search" width="18" height="18"/>
    <input key={value} type="search" name="q" aria-label={label} placeholder={label} defaultValue={value} maxLength={limit}/>
    <button type="submit">Axtar</button>
    {value && <button type="button" aria-label="Axtarışı təmizlə" onClick={()=>onSearch("")}><Icon name="close" width="16"/></button>}
  </form>;
}
export function AccountPicker({role, name, button, exclude, submit, done, revision}: {
  role: "STUDENT" | "TEACHER"; name:string; button:string; exclude?: ReadonlySet<number>;
  submit:(id:number)=>Promise<unknown>; done:()=>void; revision:number;
}) {
  const [query,setQuery]=useState(""), [page,setPage]=useState(0);
  const {data,error}=useData<Account[]>(`accounts?role=${role}&q=${encodeURIComponent(query)}&page=${page}`,revision);
  const options=data?.filter(a=>a.active && !exclude?.has(a.accountId));
  return <><SearchBox label="E-poçt, istifadəçi adı və ya ID" value={query} onSearch={text=>{setQuery(text);setPage(0);}}/>
    {data && options ? <>{options.length ? <Action button={button} done={done} submit={form=>submit(Number(form.get(name)))}>
      <label>{labels[role]}<select key={JSON.stringify([query,page,options.map(a=>a.accountId)])} name={name} required defaultValue={options.length===1 ? options[0].accountId : ""}>
        <option value="">Hesabı seçin</option>{options.map(a=><option value={a.accountId} key={a.accountId}>{a.email||a.username} · #{a.accountId}</option>)}
      </select></label></Action> : <p>Bu səhifədə seçimə uyğun hesab yoxdur.</p>}
      <Pager page={page} next={data.length===50} set={setPage}/></> : <Pending error={error}/>}
  </>;
}
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
