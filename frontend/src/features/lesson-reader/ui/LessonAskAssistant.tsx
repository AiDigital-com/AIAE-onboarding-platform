import { useEffect, useMemo, useRef, useState } from "react";
import AutoAwesomeOutlinedIcon from "@mui/icons-material/AutoAwesomeOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import DeleteSweepOutlinedIcon from "@mui/icons-material/DeleteSweepOutlined";
import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import { AI_DIGITAL_COLORS, hexToRgba } from "@/shared/lib/brandColors";
import { LoadingSpinner } from "@/shared/ui/LoadingSpinner";
import { markdownToHtml } from "@/shared/lib/lessonContent";
import type { components } from "@/shared/api/generated/schema";
import { useAskLessonMutation, type LessonAssistantPresetV1 } from "../api/useAskLessonMutation";
import {
    useClearLessonAssistantConversationMutation,
    useLessonAssistantConversationQuery,
} from "../api/useLessonAssistantConversationQuery";
import "./lesson-ask-assistant.css";

const STARTER_QUESTIONS = [
    "Explain the core idea in simpler words.",
    "What should I remember from this lesson?",
    "Give me an example from the lesson.",
];

const SMALL_PORTIONS_STARTER_QUESTIONS = [
    "Let's start with the first part.",
    "What parts does this lesson have?",
    "Start with the most important part.",
];

const NEXT_PART_PROMPT = "Continue with the next part.";

type ChatMessageV1 = components["schemas"]["ChatMessageV1"];

interface ChatMessage {
    id: string;
    role: "user" | "assistant";
    content: string;
}

function AssistantMarkdown({ content }: { content: string }) {
    return (
        <div
            className="lesson-assistant__markdown"
            dangerouslySetInnerHTML={{ __html: markdownToHtml(content) }}
        />
    );
}

interface Props {
    lessonId: number;
}

export function LessonAskAssistant({ lessonId }: Props) {
    const askMutation = useAskLessonMutation();
    const clearConversationMutation = useClearLessonAssistantConversationMutation();
    const [question, setQuestion] = useState("");
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [error, setError] = useState("");
    const [isOpen, setIsOpen] = useState(false);
    const [preset, setPreset] = useState<LessonAssistantPresetV1>("regular");
    const inputRef = useRef<HTMLTextAreaElement>(null);
    const hasHydratedRef = useRef(false);
    const hasInteractedRef = useRef(false);

    // Chat is restored from the backend (scoped to this user + lesson) once the panel is first
    // opened, so it survives navigating away and coming back — not fetched until then, since most
    // lesson visits never open the assistant.
    const conversationQuery = useLessonAssistantConversationQuery(lessonId, { enabled: isOpen });

    useEffect(() => {
        if (hasHydratedRef.current || hasInteractedRef.current || !conversationQuery.data) {
            return;
        }
        hasHydratedRef.current = true;
        const saved = conversationQuery.data;
        if (saved.messages.length === 0) {
            return;
        }
        setMessages(
            saved.messages.map((message) => ({
                id: crypto.randomUUID(),
                role: message.role,
                content: message.content,
            })),
        );
        setPreset(saved.preset);
    }, [conversationQuery.data]);

    const trimmedQuestion = question.trim();
    const starterQuestions = preset === "small_portions" ? SMALL_PORTIONS_STARTER_QUESTIONS : STARTER_QUESTIONS;
    const recentHistory = useMemo(
        () => messages.map(({ role, content }) => ({ role, content }) as ChatMessageV1).slice(-8),
        [messages],
    );

    async function askAssistant(nextQuestion = trimmedQuestion) {
        const normalizedQuestion = nextQuestion.trim();
        if (!normalizedQuestion || askMutation.isPending) {
            return;
        }
        hasInteractedRef.current = true;

        const userMessage: ChatMessage = {
            id: crypto.randomUUID(),
            role: "user",
            content: normalizedQuestion,
        };

        setMessages((current) => [...current, userMessage]);
        setQuestion("");
        setError("");

        try {
            const data = await askMutation.mutateAsync({
                lessonId,
                question: normalizedQuestion,
                history: recentHistory,
                preset,
            });

            setMessages((current) => [
                ...current,
                {
                    id: crypto.randomUUID(),
                    role: "assistant",
                    content: data.answer,
                },
            ]);
        } catch (requestError) {
            setError(
                requestError instanceof Error
                    ? requestError.message
                    : "Failed to answer the question.",
            );
            setMessages((current) => current.filter((message) => message.id !== userMessage.id));
        } finally {
            inputRef.current?.focus();
        }
    }

    function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        askAssistant();
    }

    async function clearChat() {
        setMessages([]);
        setError("");
        setQuestion("");
        hasHydratedRef.current = true;
        hasInteractedRef.current = true;
        inputRef.current?.focus();
        try {
            await clearConversationMutation.mutateAsync(lessonId);
        } catch {
            // Local chat is already cleared; the saved copy may still exist server-side.
        }
    }

    return (
        <div className="lesson-assistant">
            {!isOpen && (
                <button type="button" className="lesson-assistant__launcher" onClick={() => setIsOpen(true)}>
                    <AutoAwesomeOutlinedIcon /> Ask AI
                </button>
            )}

            {isOpen && (
                <div className="lesson-assistant__panel">
                    <header className="lesson-assistant__header">
                        <div className="lesson-assistant__header-copy">
                            <div
                                className="lesson-assistant__icon"
                                style={{
                                    color: AI_DIGITAL_COLORS.yvesKleinBlue,
                                    backgroundColor: hexToRgba(AI_DIGITAL_COLORS.brightAqua, 0.24),
                                }}
                            >
                                <AutoAwesomeOutlinedIcon />
                            </div>
                            <div>
                                <p className="lesson-assistant__title">Ask AI</p>
                                <p className="lesson-assistant__subtitle">Lesson-aware mini assistant</p>
                            </div>
                        </div>
                        <div className="lesson-assistant__header-actions">
                            <button
                                type="button"
                                className={`lesson-assistant__preset-toggle ${
                                    preset === "small_portions" ? "lesson-assistant__preset-toggle--active" : ""
                                }`}
                                onClick={() =>
                                    setPreset((prev) => (prev === "small_portions" ? "regular" : "small_portions"))
                                }
                                aria-pressed={preset === "small_portions"}
                                title="Explain in short portions, one at a time"
                            >
                                Small portions
                            </button>
                            <button
                                type="button"
                                className="lesson-assistant__icon-btn"
                                onClick={() => void clearChat()}
                                disabled={messages.length === 0 && !question && !error}
                                aria-label="Clear lesson assistant chat"
                                title="Clear chat"
                            >
                                <DeleteSweepOutlinedIcon />
                            </button>
                            <button
                                type="button"
                                className="lesson-assistant__icon-btn"
                                onClick={() => setIsOpen(false)}
                                aria-label="Close lesson assistant chat"
                                title="Close chat"
                            >
                                <CloseOutlinedIcon />
                            </button>
                        </div>
                    </header>

                    <div className="lesson-assistant__messages">
                        {messages.length === 0 ? (
                            <div className="lesson-assistant__starters">
                                <p className="lesson-assistant__intro">
                                    {preset === "small_portions"
                                        ? "Small portions mode is on — the assistant will build a plan and walk through this lesson step by step. Start below."
                                        : "Ask a question about this lesson. The assistant answers from the lesson and its source assets."}
                                </p>
                                {starterQuestions.map((starter) => (
                                    <button
                                        key={starter}
                                        type="button"
                                        className="lesson-assistant__starter"
                                        onClick={() => askAssistant(starter)}
                                        disabled={askMutation.isPending}
                                    >
                                        {starter}
                                    </button>
                                ))}
                            </div>
                        ) : (
                            <div className="lesson-assistant__thread">
                                {messages.map((message) => (
                                    <div
                                        key={message.id}
                                        className={`lesson-assistant__bubble lesson-assistant__bubble--${message.role}`}
                                    >
                                        {message.role === "assistant" ? (
                                            <AssistantMarkdown content={message.content} />
                                        ) : (
                                            <p>{message.content}</p>
                                        )}
                                    </div>
                                ))}
                                {askMutation.isPending && (
                                    <div className="lesson-assistant__loading">
                                        <LoadingSpinner label="Reading the lesson context" size="sm" />
                                    </div>
                                )}
                                {preset === "small_portions" && !askMutation.isPending && (
                                    <button
                                        type="button"
                                        className="lesson-assistant__next-part"
                                        onClick={() => askAssistant(NEXT_PART_PROMPT)}
                                    >
                                        Next part →
                                    </button>
                                )}
                            </div>
                        )}
                    </div>

                    {error && <div className="lesson-assistant__error">{error}</div>}

                    <form className="lesson-assistant__form" onSubmit={handleSubmit}>
                        <textarea
                            ref={inputRef}
                            value={question}
                            onChange={(event) => setQuestion(event.target.value)}
                            placeholder="Ask about this lesson..."
                            rows={1}
                            maxLength={2000}
                            disabled={askMutation.isPending}
                        />
                        <button
                            type="submit"
                            className="lesson-assistant__send"
                            disabled={!trimmedQuestion || askMutation.isPending}
                            aria-label="Send lesson question"
                            title="Send question"
                        >
                            {askMutation.isPending ? "…" : <SendOutlinedIcon />}
                        </button>
                    </form>
                </div>
            )}
        </div>
    );
}
