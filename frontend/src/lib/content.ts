import type { Catalog, Product } from "./types";

export const social = { shop: "https://www.instagram.com/kodariun/", school: "https://www.instagram.com/kodariun.az/" };
export const courses = [
  { id: "lego", number: "01", title: "LEGO robototexnika", category: "QURMAQ + KODLAMAQ", image: "/brand/classroom.jpg", imageClass: "classroom-image", description: "Bir ovuc detal, yüz fərqli fikir. LEGO ilə mexanizmləri tanıyır, ilk robotumuzu qurur və hərəkətə gətiririk.", topics: ["Mexanizmlər və hərəkət", "Bloklarla proqramlaşdırma", "Layihə qurmaq və təqdim etmək"] },
  { id: "steam", number: "02", title: "STEAM emalatxanası", category: "ELM + SƏNƏT", image: "/brand/rabbit-workshop.jpg", imageClass: "rabbit-image", description: "Taxta detallar birləşir, dövrə tamamlanır, rənglər işə qarışır. Sonda isə uşağın öz əli ilə hazırladığı bir oyuncaq var.", topics: ["Özün qur dəstləri", "Sadə elektrik dövrələri", "Rəngləmə və yaradıcı iş"] },
  { id: "arduino", number: "03", title: "Arduino ilə elektronika", category: "DÖVRƏ + PROQRAM", image: "/brand/worker.jpg", imageClass: "worker-image", description: "İşıq niyə yanır? Sensor nəyi hiss edir? Elektronikanın dilini kiçik təcrübələr və işləyən layihələrlə öyrənirik.", topics: ["Elektronikanın əsasları", "Sensor və motorlarla tanışlıq", "Arduino ilə ilk layihələr"] },
];

const example = (id: string, name: string, categoryId: string, image: string, description: string): Product => ({
  id, name, categoryId, sku: id, price: null, currency: "AZN", images: [image], active: true,
  attributes: { "Material": "Taxta konstruksiya", "İstiqamət": "STEAM — özün qur" }, description, source: "showcase",
});
export const showcase: Catalog = {
  mode: "showcase",
  categories: [{ id: "mechanics", name: "Mexanika", slug: "mexanika" }, { id: "electronics", name: "Elektronika", slug: "elektronika" }, { id: "creativity", name: "Yaradıcılıq", slug: "yaradiciliq" }],
  products: [
    example("zip-zip-dovsan", "Zıp-zıp dovşan", "creativity", "/brand/kit-box.jpg", "Detalları birləşdir, mexanizmi qur, dovşanını öz rənglərinlə tamamla. Qurmaqla sənəti bir araya gətirən Kodariun modeli."),
    example("robot-balqabaq", "Robot balqabaq", "mechanics", "/brand/pumpkin.jpg", "Mexanizmin necə işlədiyini kəşf et. Taxta detallar və hərəkət ideyasını birləşdirən yaradıcı bir qurma təcrübəsi."),
    example("fehle-baba", "Fəhlə Baba", "electronics", "/brand/worker.jpg", "Sadə detallar, işləyən bir mexanizm. Hərəkəti və elektronikanı birlikdə araşdırmaq üçün özün qur modeli."),
    example("yaris-masini", "Yarış maşını", "mechanics", "/brand/fire-truck.jpg", "Təkərlər, konstruksiya və hərəkət. Öz maşınını quraraq mexanikanın əsasları ilə tanış ol."),
  ],
};
export const orderLabels: Record<string, string> = {
  RESERVING: "Stok ayrılır", PENDING_PAYMENT: "Ödəniş gözlənilir", CONFIRMING: "Təsdiqlənir",
  CONFIRMED: "Təsdiqlənib", CANCELLING: "Ləğv edilir", CANCELLED: "Ləğv edilib",
  EXPIRED: "Vaxtı bitib", REJECTED: "Stok kifayət etmədi", PAYMENT_REVIEW: "Ödəniş araşdırılır",
};
export function money(amount: number | null, currency = "AZN") {
  return amount == null ? "Qiyməti soruş" : new Intl.NumberFormat("az-AZ", { style: "currency", currency }).format(amount);
}
