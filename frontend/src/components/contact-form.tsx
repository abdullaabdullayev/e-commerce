"use client";
import { useState, type FormEvent } from "react";
import { courses, social } from "@/lib/content";

const kits = "Özün qur dəstləri";
export function ContactForm({ initialCourse, program, kit }: { initialCourse?: string; program?: string; kit?: string }) {
  const [name, setName] = useState("");
  const [interest, setInterest] = useState(kit ? kits : program || courses.find(c => c.id === initialCourse)?.title || kits);
  const [note, setNote] = useState(kit ? kit + " dəsti haqqında məlumat almaq istəyirəm." : "");
  const [prepared, setPrepared] = useState("");
  const [feedback, setFeedback] = useState("");
  const choices = [...new Set([kits, ...courses.map(c => c.title), ...(program ? [program] : []), "Tədris mərkəzi ilə əməkdaşlıq", "Başqa sualım var"])];
  function prepare(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFeedback("");
    setPrepared(`Salam, Kodariun! Adım ${name.trim()}.\nMaraqlandığım mövzu: ${interest}.\n${note.trim() || "Ətraflı məlumat almaq istəyirəm."}`);
  }
  async function copy() {
    try { await navigator.clipboard.writeText(prepared); setFeedback("Mətn kopyalandı. Instagram-da mesaja əlavə edə bilərsiniz."); }
    catch { setFeedback("Aşağıdakı mətni seçib kopyalaya bilərsiniz."); }
  }
  return <section className="form-panel" id="muraciet"><h2>{prepared ? "Mesajınız hazırdır" : "Emalatxanaya yazın"}</h2>
    {prepared ? <><p>Mətni kopyalayın və Instagram-da emalatxanaya göndərin.</p><div className="prepared-message">{prepared}</div>
      <button className="button" onClick={copy}>Mətni kopyala</button>
      <p role="status">{feedback}</p>
      <a className="journal-link" href={interest === kits ? social.shop : social.school} target="_blank" rel="noreferrer">Instagram-ı aç</a>
      <button className="secondary-button ml-4" onClick={() => setPrepared("")}>Mətni dəyiş</button></> :
      <><p>Burada mesajı hazırlayın. Növbəti addımda onu Instagram-da göndərə bilərsiniz.</p>
      <form onSubmit={prepare}>
        <label className="field"><span>Adınız</span><input value={name} onChange={e => setName(e.target.value)} required maxLength={80} autoComplete="given-name"/></label>
        <label className="field"><span>Mövzu</span><select value={interest} onChange={e => setInterest(e.target.value)}>{choices.map(c => <option key={c}>{c}</option>)}</select></label>
        <label className="field"><span>Sualınız (istəyə bağlı)</span><textarea value={note} onChange={e => setNote(e.target.value)} maxLength={1000}/></label>
        <button className="button" type="submit">Mesajı hazırla</button>
      </form></>}
  </section>;
}
