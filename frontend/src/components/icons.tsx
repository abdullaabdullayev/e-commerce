import type { SVGProps } from "react";
type Name = "arrow" | "arrowUp" | "bag" | "user" | "close" | "menu" | "plus" | "minus" | "search" | "check";
const paths: Record<Name, string> = {
  arrow: "M4 12h16m-6-6 6 6-6 6", arrowUp: "M5 19 19 5M5 5h14v14",
  bag: "M5 7h14l1 14H4L5 7Zm3 0V5a4 4 0 0 1 8 0v2", user: "M20 21a8 8 0 0 0-16 0M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
  close: "m6 6 12 12M6 18 18 6", menu: "M3 6h18M3 12h18M3 18h18", plus: "M12 4v16M4 12h16", minus: "M4 12h16",
  search: "m21 21-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z", check: "m4 12 5 5L20 6",
};
export function Icon({ name, ...props }: SVGProps<SVGSVGElement> & { name: Name }) {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name]} /></svg>;
}
