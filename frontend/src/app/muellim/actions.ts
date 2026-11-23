"use server";

import { revalidatePath } from "next/cache";
import { accountSession } from "@/lib/admin-session";
import { learningRequest, uuid } from "@/lib/learning";

export async function submitLessonAction(_previous: string, form: FormData): Promise<string> {
  const session = await accountSession();
  if (session.status !== 200 || !session.roles.some(role => role === "TEACHER" || role === "ADMIN")) return "Müəllim hesabı ilə daxil olun.";
  const field = (key: string) => String(form.get(key) ?? "");
  const course = field("course");
  if (!uuid.test(course)) return "Kursu seçin.";
  const base = `courses/${course}/sessions`;
  try {
    if (field("operation") === "create") {
      if (!uuid.test(field("lesson"))) return "Dərsi seçin.";
      await learningRequest(session.token, base, "POST", {lessonId: field("lesson"), heldAt: field("heldAt")});
    } else {
      const id = field("session"), student = field("student");
      if (!uuid.test(id) || !/^[1-9]\d*$/.test(student) || !Number.isSafeInteger(Number(student))) return "Dərs və tələbəni seçin.";
      const reports = base + "/" + id + "/reports";
      if (field("operation") === "publish") {
        await learningRequest(session.token, reports + "/publish", "POST", {studentIds: [Number(student)]});
      } else if (field("operation") === "save") {
        await learningRequest(session.token, reports + "/" + student, "PUT", {
          attended: field("attended") === "yes", learned: field("learned"),
          participation: field("participation"), recommendation: field("recommendation"),
        });
      } else return "Əməliyyat tanınmadı.";
    }
  } catch (error) { return error instanceof Error ? error.message : "Sorğu alınmadı."; }
  revalidatePath("/muellim");
  return field("operation") === "publish" ? "Hesabat valideynə açıldı." : "Saxlanıldı.";
}
