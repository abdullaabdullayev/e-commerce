"use client";

import { useActionState, useState, type ReactNode } from "react";
import { submitLessonAction } from "./actions";
import type { LessonReport } from "@/lib/learning";

function ReportForm({children, button}: {children: ReactNode; button: string}) {
  const [message, action, pending] = useActionState(submitLessonAction, "");
  return <form action={action} className="order-card"><fieldset className="grid gap-3" disabled={pending}>
    {children}<button type="submit" className="secondary-button">{pending ? "Gözləyin…" : button}</button>
  </fieldset><p role="status">{message}</p></form>;
}

export function NewSession({course, lessons}: {course: string; lessons: {id: string; title: string}[]}) {
  const [heldAt, setHeldAt] = useState("");
  return <ReportForm button="Dərsi qeyd et">
    <input type="hidden" name="operation" value="create"/><input type="hidden" name="course" value={course}/>
    <label className="field">Sillabus mövzusu<select required name="lesson" defaultValue="">
      <option value="">Mövzunu seç</option>{lessons.map(l => <option key={l.id} value={l.id}>{l.title}</option>)}
    </select></label>
    <label className="field">Dərsin keçirildiyi yerli vaxt<input type="datetime-local" required onChange={e => {
      const date = new Date(e.target.value); setHeldAt(Number.isNaN(date.getTime()) ? "" : date.toISOString());
    }}/></label><input type="hidden" name="heldAt" value={heldAt}/>
  </ReportForm>;
}

export function ReportEditor({course, session, student, draft}: {course: string; session: string; student: string; draft?: LessonReport}) {
  const [attended, setAttended] = useState(draft?.attended ?? true);
  const ids = <><input type="hidden" name="course" value={course}/><input type="hidden" name="session" value={session}/>
    <input type="hidden" name="student" value={student}/></>;
  if (draft?.publishedAt) return <article className="order-card"><strong>Yayımlanıb</strong>
    <p>{draft.attended ? draft.learned : "İştirak etməyib"}</p><p>{draft.participation}</p><p>{draft.recommendation}</p></article>;
  return <><ReportForm button="Qaralamanı saxla">{ids}<input type="hidden" name="operation" value="save"/>
    <label className="field">Davamiyyət<select name="attended" value={attended ? "yes" : "no"} onChange={e => setAttended(e.target.value === "yes")}>
      <option value="yes">İştirak edib</option><option value="no">İştirak etməyib</option></select></label>
    {attended && <><label className="field">Nə öyrəndi?<textarea name="learned" maxLength={2000} defaultValue={draft?.learned}/></label>
      <label className="field">Dərsdə necə iştirak etdi?<textarea name="participation" maxLength={2000} defaultValue={draft?.participation}/></label></>}
    <label className="field">Evdə məşq tövsiyəsi<textarea name="recommendation" maxLength={1000} defaultValue={draft?.recommendation}/></label>
  </ReportForm>{draft && <ReportForm button="Saxlanmış hesabatı valideynə aç">{ids}<input type="hidden" name="operation" value="publish"/></ReportForm>}</>;
}
