const orderDetails: Record<string, string> = {
  "Requested product is unavailable": "Seçilən məhsullardan biri artıq satışda deyil. Səbətini yenilə.",
  "Order items must use the same currency": "Fərqli valyutalı məhsulları ayrıca sifariş et.",
  "Maximum item quantity is 1000": "Bir məhsul üçün sifariş edilən say həddən artıqdır.",
  "Idempotency key was used with different items": "Səbət dəyişib. Səhifəni yeniləyib yenidən cəhd et.",
  "Paid orders require a refund workflow": "Ödənişə keçmiş sifarişi buradan ləğv etmək mümkün deyil. Bizimlə əlaqə saxla.",
  "Payment is already being processed": "Bu sifarişin ödənişi artıq emal olunur.",
};

export function backendError(status: number, detail?: string, action?: "login" | "register") {
  if (status >= 500) return "Xidmət hazırda əlçatan deyil. Bir az sonra yenidən cəhd et.";
  if (status === 429) return "Çox sayda sorğu göndərilib. Bir az gözləyib yenidən cəhd et.";
  if (action === "login" && (status === 401 || status === 403)) return "Email və ya şifrə düzgün deyil.";
  if (action === "register" && status === 409) return "Bu email ilə hesab artıq mövcuddur. Hesabına daxil ol.";
  if (status === 401) return "Sessiyanın vaxtı bitib. Hesabına yenidən daxil ol.";
  if (detail && orderDetails[detail]) return orderDetails[detail];
  if (status === 404) return "Axtarılan məlumat artıq mövcud deyil.";
  if (status === 409) return "Sifarişin vəziyyəti dəyişib. Məlumatı yeniləyib yenidən yoxla.";
  if (status === 400 || status === 422) return "Göndərilən məlumatları yoxlayıb yenidən cəhd et.";
  return "Sorğunu tamamlamaq mümkün olmadı. Yenidən cəhd et.";
}
