"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";

export default function AcceptParentInvitation() {
  const [token, setToken] = useState(""), [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false), [accepted, setAccepted] = useState(false);
  useEffect(() => {
    const value = window.location.hash.slice(1);
    if (value) {
      window.history.replaceState(null, "", window.location.pathname);
      if (/^[0-9a-f]{64}$/.test(value)) setToken(value);
    }
  }, []);

  async function accept(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    const form = new FormData(event.currentTarget);
    try {
      const response = await fetch("/api/auth/accept-invitation", {method: "POST",
        headers: {"Content-Type": "application/json"}, body: JSON.stringify({
          token, firstName: form.get("firstName"), lastName: form.get("lastName"), password: form.get("password"),
        })});
      if (!response.ok) throw new Error((await response.json()).detail);
      setToken(""); setAccepted(true);
    } catch (error) { setMessage(error instanceof Error ? error.message : "Dəvət qəbul edilmədi."); }
    finally { setBusy(false); }
  }

  return <main className="auth-shell"><h1>Valideyn hesabı</h1>{accepted ?
    <><p>Hesabınız hazırdır. Dəvət aldığınız e-poçt və seçdiyiniz şifrə ilə daxil olun.</p>
      <Link className="button" href="/hesab?next=/valideyn">Daxil ol ↗</Link></> :
    !token ? <p>Davam etmək üçün adminin göndərdiyi dəvət keçidini açın.</p> :
    <form onSubmit={accept}><label className="field">Ad<input name="firstName" required maxLength={80} autoComplete="given-name"/></label>
      <label className="field">Soyad<input name="lastName" required maxLength={80} autoComplete="family-name"/></label>
      <label className="field">Şifrə<input name="password" required type="password" minLength={8} maxLength={72} autoComplete="new-password"/></label>
      {message && <p role="alert">{message}</p>}
      <button className="button" disabled={busy}>{busy ? "Gözləyin…" : "Hesabımı yarat"}</button></form>}</main>;
}
