import Link from "next/link";
export default function NotFound() { return <div className="not-found"><span className="eyebrow">BU YOL BİR AZ BAŞQA YERƏ ÇIXDI.</span><h1 className="mt-6">404</h1><p>Axtardığın səhifə tapılmadı. Kəşfə ana səhifədən davam edə bilərsən.</p><Link href="/" className="button">Ana səhifəyə qayıt ↗</Link></div>; }
