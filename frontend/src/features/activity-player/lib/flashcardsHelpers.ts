import type { LessonActivityV1 } from "@/features/lessons/api/types";

export interface FlashcardItem {
    front: string;
    back: string;
    explanation?: string;
}

export function getCards(activity: LessonActivityV1): FlashcardItem[] {
    const payload = activity.payload as { cards?: FlashcardItem[] } | undefined;
    return Array.isArray(payload?.cards) ? payload.cards : [];
}
