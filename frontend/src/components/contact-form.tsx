"use client";
import { useState, type FormEvent } from "react";
import { courses, social } from "@/lib/content";
import { Icon } from "./icons";
export function ContactForm({ initialCourse = "" }: { initialCourse?: string }) {
  const [prepared, setPrepared] = useState(""), [copied, setCopied] = useState(false);
  function prepare(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = new FormData(event.currentTarget);
    setPrepared(`Salam, Kodariun!\nAdım: ${data.get("name")}\nMaraqlandığım istiqamət: ${data.get("interest")}\n${data.get("message") || "Daha ətraflı məlumat almaq istəyirəm."}`); setCopied(false);
  }
  return <div className="form-panel"><h2>Bir sualdan başlayaq.</h2><p>Müraciət mətnini hazırla, sonra Instagram-da bizimlə paylaş.</p>{!prepared ? <form onSubmit={prepare}><label className="field"><span>Adın</span><input name="name" required maxLength={80} autoComplete="given-name" placeholder="Sənə necə müraciət edək?" /></label><label className="field"><span>Nə ilə maraqlanırsan?</span><select name="interest" defaultValue={courses.find(c => c.id === initialCourse)?.title || "Özün qur dəstləri"}><option>Özün qur dəstləri</option>{courses.map(c => <option key={c.id}>{c.title}</option>)}<option>Tədris mərkəzi ilə əməkdaşlıq</option><option>Başqa sualım var</option></select></label><label className="field"><span>Sualın və ya qeydin</span><textarea name="message" maxLength={1000} placeholder="Bir az danış…" /></label><button className="button wide" type="submit">Müraciət mətnini hazırla <Icon name="arrow" /></button><p className="text-[10px] mt-4">Bu forma məlumatını serverə göndərmir. Mesajı özün Instagram-da paylaşacaqsan.</p></form> : <><div className="prepared-message">{prepared}</div><button className="button wide" onClick={async () => { try { await navigator.clipboard.writeText(prepared); setCopied(true); } catch { setCopied(false); } }}>{copied ? "Mətn kopyalandı" : "Mətni kopyala"}<Icon name={copied ? "check" : "arrow"} /></button><a className="text-link mt-4" href={social.school} target="_blank" rel="noreferrer">Instagram-da davam et ↗</a><button className="block text-xs underline mt-5" onClick={() => setPrepared("")}>Mətni dəyiş</button></>}</div>;
}
