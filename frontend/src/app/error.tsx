"use client";
export default function ErrorPage({ reset }: { reset: () => void }) { return <div className="shell section"><div className="empty-state"><h2>Kiçik bir fasilə.</h2><p>Səhifəni hazırlayarkən xəta oldu. Yenidən cəhd edə bilərsən.</p><button className="button" onClick={reset}>Yenidən yoxla ↗</button></div></div>; }
