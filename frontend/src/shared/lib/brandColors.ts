export const AI_DIGITAL_COLORS = {
    yvesKleinBlue: "#0009DC",
    lime: "#AEF33E",
    pink: "#FF7CF5",
    silverHaze: "#F9F9F9",
    midnightCharcoal: "#080808",
    brightAqua: "#8EE7F1",
    digitalLilac: "#DDA7EF",
    skywave: "#A9BEF8",
    neonAzure: "#3AB6FF",
    violetPulse: "#8263FF",
    coolGray: "#231F20",
} as const;

export const AI_DIGITAL_AVATAR_COLORS = [
    AI_DIGITAL_COLORS.yvesKleinBlue,
    AI_DIGITAL_COLORS.pink,
    AI_DIGITAL_COLORS.brightAqua,
    AI_DIGITAL_COLORS.neonAzure,
    AI_DIGITAL_COLORS.violetPulse,
    AI_DIGITAL_COLORS.lime,
    AI_DIGITAL_COLORS.digitalLilac,
    AI_DIGITAL_COLORS.midnightCharcoal,
] as const;

const LESSON_COVER_PALETTES = [
    ["#0009DC", "#8EE7F1", "#AEF33E"],
    ["#0009DC", "#FF7CF5", "#DDA7EF"],
    ["#0009DC", "#3AB6FF", "#A9BEF8"],
    ["#8263FF", "#FF7CF5", "#0009DC"],
    ["#3AB6FF", "#8EE7F1", "#0009DC"],
    ["#AEF33E", "#8EE7F1", "#0009DC"],
    ["#DDA7EF", "#A9BEF8", "#0009DC"],
    ["#FF7CF5", "#8263FF", "#3AB6FF"],
    ["#18D8FF", "#BFFF38", "#1F45FF"],
    ["#FF5FD8", "#FFB3FA", "#6C2BFF"],
    ["#75F7F2", "#2D8CFF", "#C6D4FF"],
    ["#D8A2FF", "#7E7BFF", "#2EE6C8"],
    ["#C8FF52", "#53E7FF", "#7A63FF"],
    ["#FF88F7", "#67B8FF", "#F9F9F9"],
    ["#B7C8FF", "#37D5FF", "#5B39FF"],
    ["#E8BAFF", "#FF72EA", "#2F38FF"],
    ["#98F2FF", "#B7FF5A", "#6A8CFF"],
    ["#F9F9F9", "#89DFFF", "#7B5CFF"],
] as const;

interface LessonPaletteSeed {
    id?: string | number;
    title?: string;
}

function hashString(value: string): number {
    return [...value].reduce((hash, char) => {
        return (hash * 31 + char.charCodeAt(0)) >>> 0;
    }, 0);
}

export function hexToRgba(hex: string, alpha: number): string {
    const normalized = hex.replace("#", "");
    const number = parseInt(normalized, 16);
    const red = (number >> 16) & 255;
    const green = (number >> 8) & 255;
    const blue = number & 255;

    return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}

export function getLessonPalette(lesson: LessonPaletteSeed) {
    const seed = hashString(`${lesson.id ?? ""}:${lesson.title ?? ""}`);

    return {
        seed,
        colors: LESSON_COVER_PALETTES[seed % LESSON_COVER_PALETTES.length],
    };
}

export function getLessonCoverBackground(lesson: LessonPaletteSeed): string {
    const { seed, colors } = getLessonPalette(lesson);
    const [base, accentOne, accentTwo] = colors;
    const angle = 105 + (seed % 70);
    const firstX = 16 + (seed % 18);
    const firstY = 12 + ((seed >> 3) % 18);
    const secondX = 72 + ((seed >> 5) % 16);
    const secondY = 58 + ((seed >> 7) % 18);
    const blueStop =
        base === AI_DIGITAL_COLORS.yvesKleinBlue ? 0 : 14 + ((seed >> 9) % 16);
    const accentStop = 46 + ((seed >> 11) % 16);

    return [
        `radial-gradient(circle at ${firstX}% ${firstY}%, ${hexToRgba(accentOne, 0.64)}, transparent 34%)`,
        `radial-gradient(circle at ${secondX}% ${secondY}%, ${hexToRgba(accentTwo, 0.54)}, transparent 40%)`,
        `linear-gradient(${angle}deg, ${base} ${blueStop}%, ${accentOne} ${accentStop}%, ${accentTwo} 100%)`,
    ].join(", ");
}

export function getLessonPageBackground(lesson: LessonPaletteSeed): string {
    const { colors } = getLessonPalette(lesson);
    const [base, accentOne, accentTwo] = colors;

    return [
        `radial-gradient(circle at 12% 0%, ${hexToRgba(base, 0.16)}, transparent 34%)`,
        `radial-gradient(circle at 88% 8%, ${hexToRgba(accentOne, 0.2)}, transparent 30%)`,
        `radial-gradient(circle at 70% 92%, ${hexToRgba(accentTwo, 0.16)}, transparent 36%)`,
        `linear-gradient(180deg, ${AI_DIGITAL_COLORS.silverHaze} 0%, #EEF3FF 100%)`,
    ].join(", ");
}
