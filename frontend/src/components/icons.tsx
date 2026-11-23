import type { SVGProps } from "react";
type Name = "arrow" | "arrowUp" | "bag" | "user" | "close" | "menu" | "plus" | "minus" | "search" | "check" | "books" | "people" | "family";
const paths: Record<Name, string> = {
  books: "M4 4h5v16H4z M10 4h4v16h-4z M15 5l4-1 3 15-4 1z",
  people: "M3 20v-2a5 5 0 0 1 5-5h2a5 5 0 0 1 5 5v2 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M16 4a3.5 3.5 0 0 1 0 7 M17 14a5 5 0 0 1 4 5v1",
  family: "M9 8l3-3a5 5 0 0 1 7 7l-3 3 M15 16l-3 3a5 5 0 0 1-7-7l3-3 M8 16l8-8",
  arrow: "M4 12h16m-6-6 6 6-6 6", arrowUp: "M5 19 19 5M5 5h14v14",
  bag: "M5 7h14l1 14H4L5 7Zm3 0V5a4 4 0 0 1 8 0v2", user: "M20 21a8 8 0 0 0-16 0M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
  close: "m6 6 12 12M6 18 18 6", menu: "M3 6h18M3 12h18M3 18h18", plus: "M12 4v16M4 12h16", minus: "M4 12h16",
  search: "m21 21-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z", check: "m4 12 5 5L20 6",
};
export function Icon({ name, ...props }: SVGProps<SVGSVGElement> & { name: Name }) {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name]} /></svg>;
}
