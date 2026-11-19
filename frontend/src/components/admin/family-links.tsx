"use client";

import { useState } from "react";
import { Action, api, Pager, Pending, str, useData, type Account } from "./shared";

type Child = { accountId: number; firstName: string; lastName: string };

export function ParentInvitation() {
  const [invitation, setInvitation] = useState<{ email: string; url: string }>();
  return <details className="ap-editor"><summary>+ Valideyni dəvət et</summary>
    <Action button="Dəvət keçidi yarat" done={() => {}} submit={async form => {
      setInvitation(undefined);
      const email = str(form, "email");
      const issued = await api<{token: string}>("parent-invitations", "POST", { email });
      setInvitation({ email, url: window.location.origin + "/devet#" + issued.token });
    }}>
      <label>Valideynin e-poçtu<input name="email" type="email" required maxLength={254}/></label>
    </Action>
    {invitation && <label>Bu birdəfəlik keçidi {invitation.email} ünvanının sahibinə göndərin. Keçid 48 saat qüvvədədir.
      <input readOnly value={invitation.url} onFocus={event => event.currentTarget.select()}/>
    </label>}
  </details>;
}

export function FamilyLinks({parentId, rev, done}: {parentId: number; rev: number; done: () => void}) {
  const path = `parents/${parentId}/children`;
  const [page, setPage] = useState(0);
  const children = useData<Child[]>(path, rev);
  const students = useData<Account[]>(`accounts?role=STUDENT&page=${page}`, rev);
  if (!children.data) return <Pending error={children.error}/>;
  const linked = new Set(children.data.map(child => child.accountId));
  return <section className="ap-inset">
    <h3>Valideynə bağlı tələbələr</h3>
    {children.data.length === 0 && <p>Hələ tələbə bağlanmayıb.</p>}
    {children.data.map(child => <div className="ap-row" key={child.accountId}>
      <strong>{child.firstName} {child.lastName}</strong>
      <Action button="Əlaqəni sil" confirm="Bu valideynin tələbənin məlumatlarına girişi bağlansın?"
        done={done} submit={() => api(`${path}/${child.accountId}`, "DELETE")}/>
    </div>)}
    {students.data ? <>
      <Action button="Tələbəni bağla" done={done} submit={form => api(`${path}/${str(form, "student")}`, "PUT")}>
        <label>Tələbə<select name="student" required defaultValue="" key={page}>
          <option value="">Tələbəni seç</option>
          {students.data.filter(a => a.active && !linked.has(a.accountId)).map(a =>
            <option key={a.accountId} value={a.accountId}>{a.email || a.username}</option>)}
        </select></label>
      </Action>
      <Pager page={page} next={students.data.length === 50} set={setPage}/>
    </> : <Pending error={students.error}/>}
  </section>;
}
