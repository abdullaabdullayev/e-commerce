import type { Metadata } from "next";
import Link from "next/link";
import { courses } from "@/lib/content";
import { Icon } from "@/components/icons";
export const metadata: Metadata = { title: "Kurslar və emalatxanalar" };
export default function Courses() {
  return <div className="shell content-bottom"><div className="page-heading"><div className="breadcrumbs"><Link href="/">Ana səhifə</Link><span>/</span><span>Kurslar</span></div><span className="eyebrow">DƏRSDƏN ÇOX, BİR KƏŞF TƏCRÜBƏSİ</span><h1>Əllər işdə.<br />Fikirlər hərəkətdə.</h1><p>Robototexnika, elektronika və yaradıcı emalatxanalar. Uşağının marağına uyğun başlanğıcı birlikdə seçək.</p></div>{courses.map(course => <section id={course.id} key={course.id} className="course-feature"><div className="course-feature-image"><img className={course.imageClass} src={course.image} alt={course.title} loading="lazy" /></div><div><span className="eyebrow">{course.number} / {course.category}</span><h2>{course.title}</h2><p>{course.description}</p><ul className="topic-list">{course.topics.map((topic, index) => <li key={topic}><span>0{index + 1}</span>{topic}</li>)}</ul><p className="text-xs">Yaş qrupu, proqram, müddət və cədvəli müraciət zamanı dəqiqləşdirək.</p><Link href={`/elaqe?kurs=${course.id}`} className="text-link">Bu istiqamət haqqında danışaq <Icon name="arrowUp" /></Link></div></section>)}</div>;
}
