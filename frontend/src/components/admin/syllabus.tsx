"use client";
import { Action, api, Field, Notes, str, type Lesson, type Syllabus } from "./shared";

function LessonForm({course,lesson,done}:{course:string;lesson?:Lesson;done:()=>void}){
  return <Action reset={!lesson} button={lesson?"Dərsi saxla":"Dərs əlavə et"} done={done} submit={f=>api("courses/"+course+"/lessons"+(lesson?"/"+lesson.id:""),lesson?"PUT":"POST",{
    title:str(f,"title"),objective:str(f,"objective"), durationMinutes:Number(f.get("duration")),teacherNotes:str(f,"notes")
  })}><Field label="Dərsin adı" name="title" value={lesson?.title} limit={160}/>
  <Field label="Müddət (dəqiqə)" type="number" name="duration" min={1} max={600} value={lesson?.durationMinutes??60}/>
  <Notes label="Dərsin məqsədi" name="objective" value={lesson?.objective} limit={10000}/>
  <Notes label="Müəllim üçün qeydlər" name="notes" value={lesson?.teacherNotes} limit={50000}/></Action>;
}
export function Lessons({syllabus,done}:{syllabus:Syllabus;done:()=>void}){
  const {course,lessons}=syllabus, editable=course.status!=="ARCHIVED";
  const move=(from:number,to:number)=>{
    const ids=lessons.map(item=>item.lesson.id);
    [ids[from],ids[to]]=[ids[to],ids[from]];
    return api("courses/"+course.id+"/lesson-order","PUT",{lessonIds:ids});
  };
  return <section>{editable&&<details className="ap-editor"><summary>+ Dərs əlavə et</summary><LessonForm course={course.id} done={done}/></details>}
    {!lessons.length&&<p>Sillabusa dərs əlavə edilməyib.</p>}
    {lessons.map(({lesson:l,materials},i)=><details key={l.id} className="ap-lesson"><summary>{i+1}. {l.title} <small>{l.durationMinutes} dəq {l.archived&&"· Arxiv"}</small></summary>
      <p>{l.objective}</p>{l.teacherNotes&&<p><b>Müəllim qeydi</b> · {l.teacherNotes}</p>}
      {materials.map(m=><div className="ap-row" key={m.id}><a className="text-link" href={m.kind==="FILE"?"/api/admin/course-materials/"+m.id+"/download":m.url} target="_blank" rel="noreferrer">{m.title} ↗</a>
        {editable&&!l.archived&&<Action button="Materialı arxivlə" confirm="Material arxivlənsin?" done={done} submit={()=>api("course-materials/"+m.id,"DELETE")}/>}</div>)}
      {editable&&!l.archived&&<><details><summary>Dərsi redaktə et</summary><LessonForm course={course.id} lesson={l} done={done}/></details>
      <details><summary>Material əlavə et</summary>
        <Action reset button="Faylı yüklə" done={done} submit={f=>{const file=f.get("file") as File;if(file.size>25*1024*1024) throw new Error("Fayl 25 MB-dan böyükdür.");return api("course-materials/lessons/"+l.id+"/files","POST",f);}}><Field name="title" label="Faylın adı" limit={160}/><label>Fayl<input name="file" type="file" accept=".pdf,.png,.jpg,.jpeg,.pptx,.ino" required/></label></Action>
        <Action reset button="Linki əlavə et" done={done} submit={f=>api("course-materials/lessons/"+l.id+"/links","POST",{title:str(f,"title"),url:str(f,"url")})}><Field name="title" label="Mənbənin adı" limit={160}/><Field type="url" label="HTTPS keçidi" name="url" limit={2048}/></Action></details>
      <Action button="Dərsi arxivlə" confirm="Dərs arxivlənsin?" done={done} submit={()=>api("courses/"+course.id+"/lessons/"+l.id,"DELETE")}/></>}
      {editable&&<div className="ap-tools">{i>0&&<Action button="↑ Yuxarı" done={done} submit={()=>move(i,i-1)}/>} {i+1<lessons.length&&<Action button="↓ Aşağı" done={done} submit={()=>move(i,i+1)}/>}</div>}
    </details>)}</section>;
}
