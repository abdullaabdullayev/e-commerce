"use client";
import { useState } from "react";
import Link from "next/link";
import { Action, api, Field, labels, Notes, Pager, Pending, str, useData, type Account, type Course, type Page, type Syllabus } from "./shared";
import { Lessons } from "./syllabus";
import { People } from "./people";

function CourseForm({course,done}:{course?:Course;done:()=>void}) {
  return <Action button={course?"Dəyişikliyi saxla":"Kurs yarat"} done={done} reset={!course} submit={f=>{
    const minAge=Number(f.get("minAge")), maxAge=Number(f.get("maxAge"));
    if(minAge>maxAge) throw new Error("Minimum yaş maksimum yaşdan böyükdür.");
    return api(course?"courses/"+course.id:"courses",course?"PUT":"POST",{
      title:str(f,"title"), description:str(f,"description"),level:str(f,"level"),minAge,maxAge
    });
  }}>
    <Field name="title" label="Kursun adı" value={course?.title} limit={160}/>
    <Notes name="description" label="Kurs haqqında" value={course?.description} limit={10000}/>
    <Field name="level" label="Səviyyə" value={course?.level} limit={80}/>
    <div className="ap-pair"><Field name="minAge" label="Minimum yaş" type="number" min={3} max={100} value={course?.minAge}/><Field name="maxAge" label="Maksimum yaş" type="number" min={3} max={100} value={course?.maxAge}/></div>
  </Action>;
}

function Accounts({rev,done}:{rev:number;done:()=>void}) {
  const [page,setPage]=useState(0);
  const {data,error}=useData<Account[]>("accounts?page="+page,rev);
  return <section><h2>Hesablar</h2><details className="ap-editor"><summary>+ Hesab yarat</summary>
    <Action reset button="Hesab yarat" done={done} submit={f=>api("accounts","POST",{email:str(f,"email"),password:String(f.get("password")),firstName:str(f,"firstName"),lastName:str(f,"lastName"),roles:[str(f,"role")]})}>
      <div className="ap-pair"><Field name="firstName" label="Ad" limit={80}/><Field name="lastName" label="Soyad" limit={80}/></div>
      <Field name="email" label="Email" type="email" limit={254}/><label>İlkin şifrə<input name="password" type="password" required minLength={8} maxLength={72} autoComplete="new-password"/></label>
      <label>Rol<select name="role"><option value="TEACHER">Müəllim</option><option value="STUDENT">Tələbə</option></select></label>
    </Action></details>
    {data ? <>{data.map(a=><div className="ap-row" key={a.accountId}><strong>{a.email||a.username}</strong><span>{a.roles.map(r=>labels[r]||r).join(", ")}</span></div>)}{!data.length&&<p>Hesab tapılmadı.</p>}<Pager page={page} next={data.length===50} set={setPage}/></>:<Pending error={error}/>}
  </section>;
}
function CourseDesk({id,rev,done}:{id:string;rev:number;done:()=>void}) {
  const [tab,setTab]=useState("Sillabus");
  const {data,error}=useData<Syllabus>("courses/"+id+"/syllabus",rev);
  if(!data) return <Pending error={error}/>;
  const c=data.course, editable=c.status!=="ARCHIVED";
  return <article className="ap-desk"><small>{labels[c.status]} · {c.level} · {c.minAge}–{c.maxAge} yaş</small><h2>{c.title}</h2>
    <div className="ap-tabs">{["Sillabus","İştirakçılar və xallar","Kurs məlumatı"].map(s=><button key={s} aria-pressed={s===tab} onClick={()=>setTab(s)}>{s}</button>)}</div>
    {tab==="Sillabus"?<Lessons syllabus={data} done={done}/>:tab==="İştirakçılar və xallar"?<People course={c} teachers={data.teacherAccountIds} rev={rev} done={done}/>:<><p>{c.description}</p>{editable&&<><CourseForm course={c} done={done}/><Action button={c.status==="DRAFT"?"Kursu yayımla":"Qaralamaya qaytar"} done={done} submit={()=>api("courses/"+id+"/status","PUT",{status:c.status==="DRAFT"?"PUBLISHED":"DRAFT"})}/><Action button="Kursu arxivlə" confirm="Kurs arxivlənsin? Bu əməliyyat geri qaytarılmır." done={done} submit={()=>api("courses/"+id+"/status","PUT",{status:"ARCHIVED"})}/></>}</>}
  </article>;
}
function Courses({rev,done}:{rev:number;done:()=>void}) {
  const [page,setPage]=useState(0),[course,setCourse]=useState("");
  const {data,error}=useData<Page<Course>>("courses?page="+page,rev);
  if(course) return <><button className="ap-back" onClick={()=>setCourse("")}>← Bütün kurslar</button><CourseDesk key={course} id={course} rev={rev} done={done}/></>;
  return <section><h2>Kurslar</h2><details className="ap-editor"><summary>+ Kurs yarat</summary><CourseForm done={()=>{setPage(0);done();}}/></details>
    {data?<>{data.content.map(c=><button key={c.id} className="ap-row ap-open" onClick={()=>setCourse(c.id)}><strong>{c.title}</strong><span>{labels[c.status]} ↗</span></button>)}{!data.content.length&&<p>Bu səhifədə kurs yoxdur.</p>}<Pager page={page} next={data.hasNext} set={setPage}/></>:<Pending error={error}/>}
  </section>;
}
export function AdminPanel({label}:{label:string}) {
  const [section,setSection]=useState("Kurslar"),[rev,setRev]=useState(0),[notice,setNotice]=useState("");
  function done(){setRev(v=>v+1);setNotice("Dəyişiklik saxlanıldı.");}
  return <div className="shell ap"><header className="ap-head"><div><h1>Admin paneli</h1><p>{label}</p></div><Link className="text-link" href="/hesab">Hesabım ↗</Link></header>
    <div className="ap-layout"><nav aria-label="İdarəetmə bölmələri">{["Kurslar","Hesablar"].map(s=><button key={s} aria-pressed={s===section} onClick={()=>{setSection(s);setNotice("");}}>{s}</button>)}</nav>
    <div className="ap-content">{notice&&<p role="status" className="ap-notice">{notice}</p>}{section==="Kurslar"?<Courses rev={rev} done={done}/>:<Accounts rev={rev} done={done}/>}</div></div></div>;
}
