"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useSearchParams } from "next/navigation";
import { Icon } from "../icons";
import Link from "next/link";
import { Action, api, Field, labels, Notes, Pager, Pending, SearchBox, str, useData, type Account, type Course, type Page, type Syllabus } from "./shared";
import { Lessons } from "./syllabus";
import { People } from "./people";
import { FamilyLinks, ParentInvitation } from "./family-links";

function CourseForm({course,done,onCreated}:{course?:Course;done:()=>void;onCreated?:(course:Course)=>void}) {
  return <Action button={course?"Dəyişikliyi saxla":"Kurs yarat"} done={done} reset={!course} submit={async f=>{
    const minAge=Number(f.get("minAge")), maxAge=Number(f.get("maxAge"));
    if(minAge>maxAge) throw new Error("Minimum yaş maksimum yaşdan böyükdür.");
    const saved = await api<Course>(course?"courses/"+course.id:"courses",course?"PUT":"POST",{
      title:str(f,"title"), description:str(f,"description"),level:str(f,"level"),minAge,maxAge
    });
    if (!course) onCreated?.(saved);
  }}>
    <Field name="title" label="Kursun adı" value={course?.title} limit={160}/>
    <Notes name="description" label="Kurs haqqında" value={course?.description} limit={10000}/>
    <Field name="level" label="Səviyyə" value={course?.level} limit={80}/>
    <div className="ap-pair"><Field name="minAge" label="Minimum yaş" type="number" min={3} max={100} value={course?.minAge}/><Field name="maxAge" label="Maksimum yaş" type="number" min={3} max={100} value={course?.maxAge}/></div>
  </Action>;
}

const roleOptions = ["STUDENT", "TEACHER", "PARENT", "CUSTOMER", "ADMIN"];

function AccountRoles({ account, done }: { account: Account; done: () => void }) {
  return <div className="ap-inset"><h3>Hesabın rolları</h3>
    <p>Yeni rollar istifadəçi hesabına yenidən daxil olduqda tətbiq edilir.</p>
    <Action button="Rolları saxla" confirm="Hesabın giriş səlahiyyətləri dəyişdirilsin?" done={done} submit={form => {
      const roles = form.getAll("roles").map(String);
      if (!roles.length) throw new Error("Ən azı bir rol seçin.");
      return api("accounts/" + account.accountId + "/roles", "PUT", { roles });
    }}>
      <fieldset className="role-options"><legend>Giriş səlahiyyətləri</legend>{roleOptions.map(role => <label key={role}>
        <input type="checkbox" name="roles" value={role} defaultChecked={account.roles.includes(role)}/>{labels[role]}
      </label>)}</fieldset>
    </Action>
  </div>;
}

const sections = {
  courses: { title: "Kurslar", icon: "books", action: "Kurs yarat" },
  accounts: { title: "Hesablar", icon: "people", action: "Hesab yarat" },
  families: { title: "Valideynlər", icon: "family", action: "Valideyni dəvət et" },
} as const;
type Section = keyof typeof sections;
const tabs = { lessons: "Dərs planı", people: "İştirakçılar və xallar", settings: "Kurs məlumatı" } as const;
type Tab = keyof typeof tabs;
type Changes = Record<string, string | null>;
type Href = (changes: Changes) => string;
const navigate = (url: string) => window.history.pushState(null, "", url);

function EditorDialog({title, close, children}:{title:string; close:()=>void; children:ReactNode}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current!;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return <dialog ref={ref} className="ap-dialog" aria-labelledby="ap-dialog-title" onCancel={close}>
    <header><h2 id="ap-dialog-title">{title}</h2><button type="button" aria-label="Pəncərəni bağla" onClick={close}><Icon name="close"/></button></header>
    {children}
  </dialog>;
}

function AccountForm({created}:{created:(account:Account)=>void}) {
  return <Action button="Hesab yarat" done={()=>{}} submit={async form => {
    const account = await api<Account>("accounts","POST",{
      email:str(form,"email"), password:form.get("password"), firstName:str(form,"firstName"),
      lastName:str(form,"lastName"), roles:[str(form,"role")]
    });
    created(account);
  }}>
    <label>Hesabın rolu<select name="role"><option value="STUDENT">Tələbə</option><option value="TEACHER">Müəllim</option></select></label>
    <div className="ap-pair"><Field name="firstName" label="Ad" limit={80}/><Field name="lastName" label="Soyad" limit={80}/></div>
    <Field name="email" label="E-poçt" type="email" limit={254}/>
    <label>İlkin şifrə<input name="password" type="password" minLength={8} maxLength={72} required autoComplete="new-password"/>
      <small>Ən azı 8 simvol daxil edin.</small></label>
  </Action>;
}

function Empty({filtered, text, reset}:{filtered:boolean; text:string; reset:()=>void}) {
  return <div className="ap-empty"><Icon name={filtered?"search":"books"} width="28" height="28"/>
    <h3>{filtered?"Nəticə tapılmadı":text}</h3>
    {filtered && <button className="ap-secondary" onClick={reset}>Filtrləri təmizlə</button>}
  </div>;
}

type ListProps = {rev:number; page:number; q:string; href:Href};
function Courses({rev,page,q,status,href}:ListProps & {status:string}) {
  const params = new URLSearchParams({page:String(page),q});
  if (status) params.set("status",status);
  const {data,error} = useData<Page<Course>>("courses?"+params,rev);
  return <>
    <div className="ap-filters" aria-label="Kurs statusu">{["","PUBLISHED","DRAFT","ARCHIVED"].map(s=>
      <Link key={s} scroll={false} aria-current={status===s?"page":undefined} href={href({status:s,page:null})}>{labels[s]||"Bütün kurslar"}</Link>)}</div>
    <div className="ap-listbox"><div className="ap-tools"><SearchBox label="Kursun adına görə axtar" value={q} limit={160}
      onSearch={text=>navigate(href({q:text,page:null}))}/></div>
    {!data ? <Pending error={error}/> : data.content.length ? <div className="ap-course-list">
      <div className="ap-columns" aria-hidden="true"><span>Kursun adı</span><span>Status</span><span>İdarəetmə</span></div>
      {data.content.map(c=><div className="ap-course-row" key={c.id}>
        <Link className="ap-course-name" href={href({course:c.id,tab:null})}>
          <span className="ap-book"><Icon name="books" width="20"/></span><span><strong>{c.title}</strong><small>{c.level} · {c.minAge}–{c.maxAge} yaş</small></span></Link>
        <span className="ap-badge" data-status={c.status}>{labels[c.status]}</span>
        <Link className="ap-row-link" href={href({course:c.id,tab:"people"})} aria-label={c.title+" kursunun iştirakçıları"}>İştirakçılar <Icon name="arrow" width="15"/></Link>
      </div>)}</div> : <Empty filtered={!!(q||status)} text="Hələ kurs yaradılmayıb" reset={()=>navigate(href({status:null,q:null,page:null}))}/>}
    {data && <Pager page={page} next={data.hasNext} set={n=>navigate(href({page:String(n)}))}/>}</div>
  </>;
}

function Accounts({rev,page,q,href,families,role,expanded,done}:ListProps & {
  families:boolean; role:string; expanded:string; done:()=>void;
}) {
  const params = new URLSearchParams({page:String(page),q});
  if (families || role) params.set("role",families?"PARENT":role);
  const {data,error} = useData<Account[]>("accounts?"+params,rev);
  return <div className="ap-listbox"><div className="ap-tools">
    <SearchBox label="E-poçt, istifadəçi adı və ya ID" value={q} onSearch={text=>navigate(href({q:text,page:null,account:null}))}/>
    {!families && <label className="ap-role-filter">Rol <select value={role} onChange={e=>navigate(href({role:e.target.value,page:null,account:null}))}>
      <option value="">Bütün rollar</option>{roleOptions.map(r=><option value={r} key={r}>{labels[r]}</option>)}</select></label>}
  </div>
    {!data ? <Pending error={error}/> : data.length ? data.map(a=>{
      const open=String(a.accountId)===expanded;
      return <div className="ap-person" key={a.accountId}>
        <button className="ap-account-row" aria-expanded={open} onClick={()=>navigate(href({account:open?null:String(a.accountId)}))}>
          <span className="ap-account-name"><strong>{a.email||a.username}</strong><small>#{a.accountId} · {a.roles.map(r=>labels[r]||r).join(", ")}</small></span>
          <span className="ap-row-link">{families?"Uşaqlar":"Rollar"} <span aria-hidden="true">{open?"▴":"▾"}</span></span>
        </button>
        {open && (families?<FamilyLinks parentId={a.accountId} rev={rev} done={done}/>:<AccountRoles key={a.roles.join()} account={a} done={done}/>)}
      </div>;
    }) : <Empty filtered={!!(q||role)} text={families?"Hələ valideyn hesabı yoxdur":"Hesab yoxdur"} reset={()=>navigate(href({role:null,q:null,page:null,account:null}))}/>}
    {data && <Pager page={page} next={data.length===50} set={n=>navigate(href({page:String(n),account:null}))}/>}
  </div>;
}

function CourseDesk({id,tab,rev,href,done}:{id:string;tab:Tab;rev:number;href:Href;done:()=>void}) {
  const {data,error}=useData<Syllabus>("courses/"+id+"/syllabus",rev);
  if (!data) return <Pending error={error}/>;
  const c=data.course, editable=c.status!=="ARCHIVED";
  return <article className="ap-desk">
    <div className="ap-heading"><div><span className="ap-badge" data-status={c.status}>{labels[c.status]}</span><h1>{c.title}</h1>
      <p>{c.level} · {c.minAge}–{c.maxAge} yaş</p></div>
      <Link className="ap-secondary" href={"/muellim?course="+id}>Dərs hesabatları <Icon name="arrowUp" width="16"/></Link></div>
    <div className="ap-filters">{(Object.keys(tabs) as Tab[]).map(t=>
      <Link key={t} aria-current={tab===t?"page":undefined} scroll={false} href={href({tab:t})}>{tabs[t]}</Link>)}</div>
    <div className="ap-sheet">{tab==="lessons"?<Lessons syllabus={data} done={done}/>:tab==="people"?<People course={c} teachers={data.teacherAccountIds} rev={rev} done={done}/>:<>
      <h2>Kurs məlumatı</h2>{editable?<><CourseForm key={c.id} course={c} done={done}/>
        <section className="ap-lifecycle"><h3>Yayım statusu</h3><Action button={c.status==="DRAFT"?"Kursu yayımla":"Qaralamaya qaytar"}
          done={done} submit={()=>api("courses/"+id+"/status","PUT",{status:c.status==="DRAFT"?"PUBLISHED":"DRAFT"})}/>
        <div className="ap-danger"><Action button="Kursu arxivlə" confirm="Kurs arxivlənsin? Bu əməliyyat geri qaytarılmır." done={done} submit={()=>api("courses/"+id+"/status","PUT",{status:"ARCHIVED"})}/></div></section>
      </>:<p>{c.description}</p>}</>}</div>
  </article>;
}

export function AdminPanel({label}:{label:string}) {
  const params=useSearchParams();
  const section:Section=params.get("section")==="accounts"?"accounts":params.get("section")==="families"?"families":"courses";
  const selected=sections[section];
  const course=section==="courses"?params.get("course"):null;
  const tab:Tab=params.get("tab")==="people"?"people":params.get("tab")==="settings"?"settings":"lessons";
  const page= Math.max(0,Math.min(100000,parseInt(params.get("page")||"",10)||0));
  const [revision,setRevision]=useState(0), [notice,setNotice]=useState(""), [editor,setEditor]=useState<Section|null>(null);
  function href(changes:Changes) {
    const query=new URLSearchParams(params.toString());
    for(const [key,value] of Object.entries(changes)) {if(value) query.set(key,value); else query.delete(key);}
    return "/admin"+(query.size?"?"+query:"");
  }
  function done(message="Dəyişiklik saxlanıldı.") {setRevision(r=>r+1);setNotice(message);}
  const list={href,rev:revision,page,q:params.get("q")||""};
  const listHref=href({course:null,tab:null,account:null});
  return <div className="ap-workspace">
    <aside className="ap-sidebar"><Link className="ap-brand" href="/admin" aria-label="Kodariun idarəetmə paneli"><img src="/brand/logo.jpg" width="36" height="36" alt=""/><span>kodariun.</span></Link>
      <nav aria-label="İdarəetmə bölmələri">{(Object.keys(sections) as Section[]).map(key=><Link key={key} href={key==="courses"?"/admin":"/admin?section="+key} aria-current={section===key?"page":undefined}
        onClick={()=>{setNotice("");setEditor(null);}}><Icon name={sections[key].icon} width="19"/>{sections[key].title}</Link>)}</nav>
      <Link href="/hesab?tab=overview" className="ap-my-account"><Icon name="user" width="18"/> Hesabıma qayıt</Link></aside>
    <div className="ap-main"><header className="ap-top"><nav aria-label="Səhifə yolu"><span>İdarəetmə</span><span aria-hidden="true">/</span><Link href={listHref}>{selected.title}</Link>
      {course && <><span aria-hidden="true">/</span><span>{tabs[tab]}</span></>}</nav><Link href="/hesab?tab=overview" className="ap-identity"><Icon name="user" width="17"/>{label}</Link></header>
      <div className="ap-content">
        {notice && <div className="ap-notice" role="status"><Icon name="check" width="18"/>{notice}<button aria-label="Bildirişi bağla" onClick={()=>setNotice("")}><Icon name="close" width="16"/></button></div>}
        {course ? <><Link className="ap-back" href={listHref}>← Kurs siyahısına qayıt</Link><CourseDesk key={course} id={course} rev={revision} tab={tab} href={href} done={()=>done()}/></> : <>
          <div className="ap-heading"><div><h1>{selected.title}</h1><p>{section==="courses"?"Dərs planı, müəllim təyinatı və tələbə qeydiyyatı.":section==="accounts"?"Tələbə və müəllim hesabları, rollar və giriş səlahiyyətləri.":"Valideyn dəvətləri və valideynə bağlı tələbələr."}</p></div>
            <button className="ap-primary" onClick={()=>{setNotice("");setEditor(section);}}><Icon name="plus" width="18"/>{selected.action}</button></div>
          {section==="courses"?<Courses {...list} status={params.get("status")||""}/>:<Accounts {...list} families={section==="families"} role={params.get("role")||""} expanded={params.get("account")||""} done={()=>done()}/>}
        </>}
        {editor===section && <EditorDialog title={selected.action} close={()=>setEditor(null)}>
          {section==="families"?<ParentInvitation close={()=>{setEditor(null);setRevision(r=>r+1);}}/>:section==="accounts"?<AccountForm created={a=>{setEditor(null);navigate(href({q:String(a.accountId),role:null,page:null,account:String(a.accountId)}));done("Hesab yaradıldı.");}}/>:
            <CourseForm onCreated={c=>{setEditor(null);navigate(href({course:c.id,tab:"lessons"}));}} done={()=>done("Kurs yaradıldı.")}/>}
        </EditorDialog>}
      </div></div>
  </div>;
}
