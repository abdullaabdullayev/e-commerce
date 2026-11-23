"use client";

import { useState } from "react";
import { AccountPicker, Action, api, Pending, str, useData } from "./shared";

type Child = { accountId: number; firstName: string; lastName: string };

export function ParentInvitation({ close }: { close: () => void }) {
  const [invitation, setInvitation] = useState<{ email: string; url: string }>();
  const [copyMessage,setCopyMessage] = useState("");
  return <div>
    {!invitation && <Action button="Dəvət keçidi yarat" done={() => {}} submit={async form => {
      setInvitation(undefined);
      setCopyMessage("");
      const email = str(form, "email");
      const issued = await api<{token: string}>("parent-invitations", "POST", { email });
      setInvitation({ email, url: window.location.origin + "/devet#" + issued.token });
    }}>
      <label>Valideynin e-poçtu<input name="email" type="email" required maxLength={254}/></label>
    </Action>}
    {invitation && <div className="ap-inset"><strong>Dəvət keçidi hazırdır</strong><label>Bu keçidi {invitation.email} ünvanının sahibinə göndərin. Keçid 48 saat qüvvədədir.
      <input readOnly value={invitation.url} onFocus={event => event.currentTarget.select()}/>
    </label><button type="button" className="ap-secondary" onClick={async()=>{
      try {await navigator.clipboard.writeText(invitation.url);setCopyMessage("Keçid kopyalandı.");}
      catch {setCopyMessage("Keçidi seçib əl ilə kopyalayın.");}
    }}>Keçidi kopyala</button> <span role="status">{copyMessage}</span><p>Valideyn qeydiyyatı tamamlayandan sonra onu e-poçtuna görə tapıb tələbəyə bağlaya bilərsiniz.</p><button type="button" className="ap-secondary" onClick={close}>Valideyn siyahısına qayıt</button></div>}
  </div>;
}

export function FamilyLinks({parentId, rev, done}: {parentId: number; rev: number; done: () => void}) {
  const path = `parents/${parentId}/children`;
  const children = useData<Child[]>(path, rev);
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
    <h4>Tələbə əlavə et</h4>
    <AccountPicker role="STUDENT" name="student" revision={rev} exclude={linked} button="Valideynə bağla" done={done}
      submit={id=>api(`${path}/${id}`,"PUT")}/>
  </section>;
}
