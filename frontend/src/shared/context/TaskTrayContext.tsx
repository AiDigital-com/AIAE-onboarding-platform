import {
    type CSSProperties,
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from "react";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import ExpandLessOutlinedIcon from "@mui/icons-material/ExpandLessOutlined";
import ExpandMoreOutlinedIcon from "@mui/icons-material/ExpandMoreOutlined";
import HourglassTopOutlinedIcon from "@mui/icons-material/HourglassTopOutlined";
import "./task-tray.css";

export type TaskStatus = "running" | "success" | "error";

export interface TaskItem {
    id: string;
    title: string;
    description: string;
    status: TaskStatus;
}

interface TaskTrayContextValue {
    addTask: (task: Pick<TaskItem, "title" | "description">) => string;
    updateTask: (id: string, patch: Partial<Pick<TaskItem, "description" | "status">>) => void;
    dismissTask: (id: string) => void;
    dismissCompleted: () => void;
}

const TaskTrayContext = createContext<TaskTrayContextValue | null>(null);

function createTaskId() {
    if (typeof crypto !== "undefined" && crypto.randomUUID) {
        return crypto.randomUUID();
    }

    return `task-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function getStatusLabel(status: TaskStatus) {
    if (status === "error") return "Failed";
    if (status === "success") return "Done";
    return "In progress";
}

function TaskTrayPanel({
    tasks,
    onDismiss,
    onDismissCompleted,
}: {
    tasks: TaskItem[];
    onDismiss: (id: string) => void;
    onDismissCompleted: () => void;
}) {
    const [expanded, setExpanded] = useState(false);

    if (tasks.length === 0) {
        return null;
    }

    const activeCount = tasks.filter((task) => task.status === "running").length;
    const errorCount = tasks.filter((task) => task.status === "error").length;
    const successCount = tasks.filter((task) => task.status === "success").length;
    const hasErrors = errorCount > 0;
    const hasActiveTasks = activeCount > 0;

    return (
        <aside
            className={`task-tray${expanded ? " task-tray--expanded" : ""}${hasErrors ? " task-tray--error" : ""}`}
            style={
                {
                    "--task-tray-border": hasErrors ? "rgba(220,38,38,0.16)" : "rgba(0,9,220,0.16)",
                    "--task-tray-header-bg": hasErrors ? "#7f1d1d" : "#0b0b0b",
                } as CSSProperties
            }
        >
            <div className={`task-tray__header${hasErrors ? " task-tray__header--error" : ""}`}>
                <div className="task-tray__header-text">
                    <p className="task-tray__title">
                        {hasErrors ? "Task failed" : hasActiveTasks ? "Working..." : "Done"}
                    </p>
                    <span className="task-tray__subtitle">
                        {hasErrors
                            ? `${errorCount} error${errorCount === 1 ? "" : "s"}`
                            : hasActiveTasks
                              ? `${activeCount} running`
                              : `${successCount} completed`}
                    </span>
                </div>
                <button
                    type="button"
                    aria-label={expanded ? "Collapse task tray" : "Expand task tray"}
                    className="task-tray__icon-button"
                    onClick={() => setExpanded((prev) => !prev)}
                >
                    {expanded ? <ExpandMoreOutlinedIcon /> : <ExpandLessOutlinedIcon />}
                </button>
            </div>

            {!expanded && activeCount > 0 && !hasErrors && (
                <div className="task-tray__progress">
                    <div className="task-tray__progress-bar" />
                </div>
            )}

            {expanded && (
                <>
                    <ul className="task-tray__list">
                        {tasks.map((task) => (
                            <li key={task.id} className={`task-tray__item task-tray__item--${task.status}`}>
                                <div className="task-tray__item-row">
                                    <span
                                        aria-hidden="true"
                                        className={`task-tray__item-icon${task.status === "error" ? " task-tray__item-icon--error" : ""}`}
                                    >
                                        {task.status === "error" ? (
                                            <ErrorOutlineOutlinedIcon />
                                        ) : (
                                            <HourglassTopOutlinedIcon />
                                        )}
                                    </span>
                                    <div className="task-tray__item-body">
                                        <div className="task-tray__item-heading">
                                            <p className="task-tray__item-title">{task.title}</p>
                                            <span
                                                className={`task-tray__item-status task-tray__item-status--${task.status}`}
                                            >
                                                {getStatusLabel(task.status)}
                                            </span>
                                        </div>
                                        {task.description && (
                                            <p
                                                className={`task-tray__item-description${task.status === "error" ? " task-tray__item-description--error" : ""}`}
                                            >
                                                {task.description}
                                            </p>
                                        )}
                                        {task.status === "running" && (
                                            <div className="task-tray__item-progress">
                                                <div className="task-tray__item-progress-bar" />
                                            </div>
                                        )}
                                    </div>
                                    <button
                                        type="button"
                                        aria-label={task.status === "running" ? "Hide task" : "Dismiss task"}
                                        className="task-tray__icon-button"
                                        onClick={() => onDismiss(task.id)}
                                    >
                                        <CloseOutlinedIcon fontSize="small" />
                                    </button>
                                </div>
                            </li>
                        ))}
                    </ul>
                    {hasErrors && (
                        <div className="task-tray__footer">
                            <button type="button" className="task-tray__clear-button" onClick={onDismissCompleted}>
                                Clear errors
                            </button>
                        </div>
                    )}
                </>
            )}
        </aside>
    );
}

export function TaskTrayProvider({ children }: { children: ReactNode }) {
    const [tasks, setTasks] = useState<TaskItem[]>([]);

    useEffect(() => {
        if (!tasks.some((task) => task.status === "success")) {
            return;
        }

        const timeoutId = window.setTimeout(() => {
            setTasks((current) => current.filter((task) => task.status !== "success"));
        }, 1500);

        return () => window.clearTimeout(timeoutId);
    }, [tasks]);

    const addTask = useCallback((task: Pick<TaskItem, "title" | "description">) => {
        const id = createTaskId();
        setTasks((prev) => [
            {
                id,
                title: task.title,
                description: task.description,
                status: "running",
            },
            ...prev,
        ]);
        return id;
    }, []);

    const updateTask = useCallback(
        (id: string, patch: Partial<Pick<TaskItem, "description" | "status">>) => {
            setTasks((prev) =>
                prev.map((task) => (task.id === id ? { ...task, ...patch } : task)),
            );
        },
        [],
    );

    const dismissTask = useCallback((id: string) => {
        setTasks((prev) => prev.filter((task) => task.id !== id));
    }, []);

    const dismissCompleted = useCallback(() => {
        setTasks((prev) => prev.filter((task) => task.status === "running"));
    }, []);

    const value = useMemo(
        () => ({ addTask, updateTask, dismissTask, dismissCompleted }),
        [addTask, dismissCompleted, dismissTask, updateTask],
    );

    return (
        <TaskTrayContext.Provider value={value}>
            {children}
            <TaskTrayPanel
                tasks={tasks}
                onDismiss={dismissTask}
                onDismissCompleted={dismissCompleted}
            />
        </TaskTrayContext.Provider>
    );
}

export function useTaskTray() {
    const context = useContext(TaskTrayContext);

    if (!context) {
        throw new Error("useTaskTray must be used within TaskTrayProvider");
    }

    return context;
}
