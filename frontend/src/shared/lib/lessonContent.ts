function escapeHtml(value = ""): string {
    return value
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function formatInlineMarkdown(value = ""): string {
    return escapeHtml(value)
        .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
        .replace(/\*(.+?)\*/g, "<em>$1</em>")
        .replace(/`(.+?)`/g, "<code>$1</code>");
}

function flushList(output: string[], listItems: string[], ordered: boolean) {
    if (listItems.length === 0) {
        return;
    }

    const tag = ordered ? "ol" : "ul";
    output.push(`<${tag}>`);
    for (const item of listItems) {
        output.push(`<li>${formatInlineMarkdown(item)}</li>`);
    }
    output.push(`</${tag}>`);
    listItems.length = 0;
}

export function markdownToHtml(markdown = ""): string {
    const lines = markdown.replace(/\r\n/g, "\n").split("\n");
    const output: string[] = [];
    const listItems: string[] = [];
    let isOrderedList = false;
    let paragraph: string[] = [];

    const flushParagraph = () => {
        if (paragraph.length === 0) {
            return;
        }

        output.push(`<p>${formatInlineMarkdown(paragraph.join(" "))}</p>`);
        paragraph = [];
    };

    for (const line of lines) {
        const trimmed = line.trim();

        if (!trimmed) {
            flushParagraph();
            flushList(output, listItems, isOrderedList);
            continue;
        }

        const headingMatch = trimmed.match(/^(#{1,4})\s+(.+)$/);
        if (headingMatch) {
            flushParagraph();
            flushList(output, listItems, isOrderedList);
            const level = headingMatch[1].length;
            output.push(`<h${level}>${formatInlineMarkdown(headingMatch[2])}</h${level}>`);
            continue;
        }

        const unorderedMatch = trimmed.match(/^[-*]\s+(.+)$/);
        if (unorderedMatch) {
            flushParagraph();
            if (listItems.length > 0 && isOrderedList) {
                flushList(output, listItems, isOrderedList);
            }
            isOrderedList = false;
            listItems.push(unorderedMatch[1]);
            continue;
        }

        const orderedMatch = trimmed.match(/^\d+\.\s+(.+)$/);
        if (orderedMatch) {
            flushParagraph();
            if (listItems.length > 0 && !isOrderedList) {
                flushList(output, listItems, isOrderedList);
            }
            isOrderedList = true;
            listItems.push(orderedMatch[1]);
            continue;
        }

        if (trimmed.startsWith("> ")) {
            flushParagraph();
            flushList(output, listItems, isOrderedList);
            output.push(`<blockquote><p>${formatInlineMarkdown(trimmed.slice(2))}</p></blockquote>`);
            continue;
        }

        paragraph.push(trimmed);
    }

    flushParagraph();
    flushList(output, listItems, isOrderedList);

    return output.join("\n");
}
