"use client";
import { useRef, useState } from "react";
import { Action, api, Field, labels, Pager, Pending, str, useData, type Account, type Course, type Page } from "./shared";

type Entry = { enrollment:{studentAccountId:number;status:string}; profile:{firstName:string;lastName:string}|null };
type Point = { id:string;type:"AWARD"|"CORRECTION";points:number;reason:string;createdAt:string };
type History = {totalPoints:number; entries:Page<Point>};

function Assignment({role,course,rev,done}:{role:"TEACHER"|"STUDENT";course:string;rev:number;done:()=>void}) {
  const [page,setPage]=useState(0);
  const {data,error}=useData<Account[]>("accounts?role="+role+"&page="+page,rev);
  const teacher=role==="TEACHER";
  return <details className="ap-editor"><summary>{teacher?"+ Müəllim təyin et":"+ Tələbəni kursa yaz"}</summary>
    {data?<><Action button={teacher?"Təyin et":"Qeydiyyata al"} done={done} submit={f=>api("courses/"+course+"/"+(teacher?"teachers/":"students/")+str(f,"account"),"PUT",teacher?undefined:{status:"ACTIVE"})}>
    <label>{labels[role]}<select key={page} name="account" required defaultValue=""><option value="">Hesabı seçin</option>{data.filter(a=>a.active).map(a=><option key={a.accountId} value={a.accountId}>{a.email||a.username} (#{a.accountId})</option>)}</select></label></Action>
    <Pager page={page} set={setPage} next={data.length===50}/></>:<Pending error={error}/>}</details>;
}
function ScoreForm({path,award,done}:{path:string;award?:Point;done:()=>void}){
  const attempt=useRef<{signature:string;id:string}|null>(null);
  return <Action reset button={award?"Düzəlişi saxla":"Xal ver"} done={()=>{attempt.current=null;done();}} submit={f=>{
    const amount=Number(f.get("points")), reason=str(f,"reason");
    const signature=JSON.stringify([path,award?.id,amount,reason]);
    if(attempt.current?.signature!==signature) attempt.current={signature,id:crypto.randomUUID()};
    const body={requestId:attempt.current.id,reason,...(award?{correctedPoints:amount}:{points:amount})};
    return api(path+(award?"/"+award.id+"/corrections":""),"POST",body);
  }}><Field name="points" type="number" label={award?"Bu mükafatın yeni yekun xalı":"Xal"} min={award?0:1} max={1000}/>
  <Field label="Səbəb (tələbəyə görünür)" name="reason" limit={500}/></Action>;
}
function Points({path,canAward,rev,done}:{path:string;canAward:boolean;rev:number;done:()=>void}){
  const [page,setPage]=useState(0);
  const {data,error}=useData<History>(path+"?page="+page,rev);
  return <div className="ap-inset">{data?<><h4>Ümumi xal · {data.totalPoints}</h4>
    {canAward&&<details><summary>Xal ver</summary><ScoreForm path={path} done={done}/></details>}
    {data.entries.content.map(p=><div className="ap-entry" key={p.id}><b>{p.points>0?"+":""}{p.points} xal</b> · {p.reason}<small>{new Date(p.createdAt).toLocaleDateString("az-AZ")} · {p.type==="AWARD"?"Mükafat":"Düzəliş"}</small>
      {p.type==="AWARD"&&<details><summary>Bu mükafatı düzəlt</summary><ScoreForm path={path} award={p} done={done}/></details>}</div>)}
    <Pager page={page} next={data.entries.hasNext} set={setPage}/></>:<Pending error={error}/>}</div>;
}
export function People({course,teachers,rev,done}:{course:Course;teachers:number[];rev:number;done:()=>void}){
  const [page,setPage]=useState(0),[student,setStudent]=useState<number>();
  const prefix="courses/"+course.id, editable=course.status!=="ARCHIVED";
  const {data,error}=useData<Page<Entry>>(prefix+"/students?page="+page,rev);
  return <section><h3>Müəllimlər</h3>
    {!teachers.length&&<p>Müəllim təyin edilməyib.</p>}
    {teachers.map(id=><TeacherRow key={id} id={id} rev={rev} prefix={prefix} done={done}/>)}
    {editable&&<Assignment course={course.id} role="TEACHER" rev={rev} done={done}/>}
    <h3>Tələbələr</h3>{editable&&<Assignment course={course.id} role="STUDENT" rev={rev} done={done}/>}
    {data?<>{!data.content.length&&<p>Kursa qeydiyyat yoxdur.</p>}{data.content.map(({enrollment:e,profile})=>{
      const path=prefix+"/students/"+e.studentAccountId;
      return <div key={e.studentAccountId}><button className="ap-row ap-open" aria-expanded={student===e.studentAccountId} onClick={()=>setStudent(student===e.studentAccountId?undefined:e.studentAccountId)}>
        <strong>{profile?[profile.firstName,profile.lastName].join(" "):"Tələbə #"+e.studentAccountId}</strong><span>{labels[e.status]} ▾</span></button>
        {student===e.studentAccountId&&<><Action button="Statusu saxla" done={done} submit={f=>api(path,"PUT",{status:str(f,"status")})}><label>Qeydiyyat<select name="status" defaultValue={e.status}>{["ACTIVE","COMPLETED","LEFT"].map(s=><option key={s} disabled={!editable&&s==="ACTIVE"} value={s}>{labels[s]}</option>)}</select></label></Action>
        <Points path={path+"/points"} rev={rev} canAward={course.status==="PUBLISHED"&&e.status==="ACTIVE"} done={done}/></>}</div>;
    })}<Pager page={page} next={data.hasNext} set={setPage}/></>:<Pending error={error}/>}</section>;
}
function TeacherRow({id,rev,prefix,done}:{id:number;rev:number;prefix:string;done:()=>void}){
  const {data}=useData<Account>("accounts/"+id,rev);
  return <div className="ap-row"><strong>{data?.email||data?.username||"Müəllim #"+id}</strong><Action button="Təyinatı sil" confirm="Müəllimin bu kursa girişi bağlansın?" done={done} submit={()=>api(prefix+"/teachers/"+id,"DELETE")}/></div>;
}
