import { useMemo, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import GroupsOutlinedIcon from "@mui/icons-material/GroupsOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import QuizOutlinedIcon from "@mui/icons-material/QuizOutlined";
import RouteOutlinedIcon from "@mui/icons-material/RouteOutlined";
import type { components } from "@/shared/api/generated/schema";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { ErrorAlert } from "@/shared/ui/ErrorAlert";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import { useFilePreviewUrls } from "@/shared/api/files";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import "@/shared/ui/select.css";
import {
    useTeamDashboardQuery,
    type DashboardPeriod,
} from "../api/useTeamDashboardQuery";
import "./team-progress.css";

type TeamDashboardResponseV1 = components["schemas"]["TeamDashboardResponseV1"];
type TeamDashboardMemberV1 = components["schemas"]["TeamDashboardMemberV1"];
type TeamDashboardLowConfidenceLessonV1 =
    components["schemas"]["TeamDashboardLowConfidenceLessonV1"];
type TeamDashboardIndividualRoadmapV1 =
    components["schemas"]["TeamDashboardIndividualRoadmapV1"];

const COLORS = {
    ink: "#0B0B0B",
    slate: "#33344A",
    mute: "#80808E",
    blue: "#0009DC",
    blue50: "#F5F5FE",
    blue100: "#E5E5FA",
    blue200: "#C7C7F0",
    orange: "#FF642D",
    success: "#229E5A",
};

const PERIOD_OPTIONS: { id: DashboardPeriod; label: string }[] = [
    { id: "week", label: "This week" },
    { id: "month", label: "This month" },
    { id: "quarter", label: "This quarter" },
];

function formatQuizScore(score: number | null | undefined) {
    return score === null || score === undefined ? "N/A" : `${score}%`;
}

function formatDateShort(value?: string | null) {
    if (!value) {
        return "n/a";
    }

    try {
        return new Intl.DateTimeFormat("en", {
            month: "short",
            day: "2-digit",
        }).format(new Date(value));
    } catch {
        return "n/a";
    }
}

function averageQuizScore(members: TeamDashboardMemberV1[]) {
    const scores = members
        .map((member) => member.quiz)
        .filter((score): score is number => score !== null && score !== undefined);

    if (scores.length === 0) {
        return null;
    }

    return Math.round(scores.reduce((total, score) => total + score, 0) / scores.length);
}

function buildRoadmapProgressFromMembers(
    members: TeamDashboardMemberV1[],
    groupsByMemberId: TeamDashboardResponseV1["individualRoadmapsByMemberId"],
) {
    const roadmapsById = new Map<
        string,
        {
            id: string;
            name: string;
            learners: number;
            progressTotal: number;
            lessonCount: number;
        }
    >();
    const colors = ["#0009DC", "#F0348E", "#42B1CF", "#FF642D", "#229E5A", "#42B1CF"];

    members.forEach((member) => {
        member.roadmaps.forEach((roadmap) => {
            const roadmapGroup = (groupsByMemberId[member.id] ?? []).find(
                (group) => group.id === roadmap.id,
            );

            if (!roadmapsById.has(roadmap.id)) {
                roadmapsById.set(roadmap.id, {
                    id: roadmap.id,
                    name: roadmap.title,
                    learners: 0,
                    progressTotal: 0,
                    lessonCount: 0,
                });
            }

            const current = roadmapsById.get(roadmap.id)!;
            current.learners += 1;
            current.progressTotal += roadmapGroup?.progress ?? 0;
            current.lessonCount += roadmapGroup?.lessonCount ?? 0;
        });
    });

    return [...roadmapsById.values()].map((roadmap, index) => ({
        ...roadmap,
        progress: roadmap.learners > 0 ? Math.round(roadmap.progressTotal / roadmap.learners) : 0,
        color: colors[index % colors.length],
    }));
}

function aggregateWeeklyActivity(
    weeklyRows: TeamDashboardResponseV1["weekly"],
    members: TeamDashboardMemberV1[],
) {
    const visibleUserIds = new Set(members.map((member) => member.id));
    const totalsByLabel = new Map<string, { label: string; lessons: number; quizzes: number }>();

    weeklyRows.forEach((row) => {
        if (!visibleUserIds.has(row.userId)) {
            return;
        }

        const current = totalsByLabel.get(row.label) || {
            label: row.label,
            lessons: 0,
            quizzes: 0,
        };
        current.lessons += row.lessons;
        current.quizzes += row.quizzes;
        totalsByLabel.set(row.label, current);
    });

    return [...totalsByLabel.values()];
}

/**
 * Low-confidence lesson KPIs (attempts/learners/avgScore) come straight from the backend's
 * full-history aggregates — never recomputed from `attemptItems`, which is only a bounded recent
 * sample for drill-down display. The `includeTeamLead` toggle picks between
 * the backend's two precomputed variants instead of re-deriving numbers client-side, which would
 * silently go wrong once the embedded sample no longer holds every attempt.
 */
export function buildLowConfidenceLessonsForMembers(
    lessons: TeamDashboardResponseV1["lowConfidenceLessons"],
    leadId: string | null,
    includeTeamLead: boolean,
): TeamDashboardLowConfidenceLessonV1[] {
    return lessons
        .map((lesson) => {
            const attempts = includeTeamLead ? lesson.attempts : lesson.attemptsExcludingLead;
            const learners = includeTeamLead ? lesson.learners : lesson.learnersExcludingLead;
            const avgScore = includeTeamLead ? lesson.avgScore : lesson.avgScoreExcludingLead;

            if (attempts === 0 || avgScore >= 80) {
                return null;
            }

            const attemptItems =
                includeTeamLead || !leadId
                    ? lesson.attemptItems
                    : (lesson.attemptItems ?? []).filter((attempt) => attempt.userId !== leadId);

            return { ...lesson, attempts, learners, avgScore, attemptItems };
        })
        .filter((lesson): lesson is TeamDashboardLowConfidenceLessonV1 => lesson !== null);
}

function ProgressBar({
    value,
    color = COLORS.blue,
    height = 8,
}: {
    value: number;
    color?: string;
    height?: number;
}) {
    return (
        <div className="team-progress-bar" style={{ height }}>
            <div
                className="team-progress-bar__fill"
                style={{
                    width: `${Math.min(100, Math.max(0, value))}%`,
                    backgroundColor: color,
                }}
            />
        </div>
    );
}

function StatusDot({ status }: { status: TeamDashboardMemberV1["status"] }) {
    const map = {
        "not-started": { color: COLORS.mute, label: "Not started" },
        "in-progress": { color: COLORS.blue, label: "In progress" },
        done: { color: COLORS.blue, label: "Completed" },
    } as const;
    const current = map[status] || map["not-started"];

    return (
        <span className="team-progress-status" style={{ color: current.color }}>
            <span className="team-progress-status__dot" />
            {current.label}
        </span>
    );
}

function QuizAttemptsDialog({
    lesson,
    open,
    onClose,
}: {
    lesson: TeamDashboardLowConfidenceLessonV1 | null;
    open: boolean;
    onClose: () => void;
}) {
    const [personFilter, setPersonFilter] = useState("all");
    const [resultFilter, setResultFilter] = useState("all");
    const attempts = useMemo(() => lesson?.attemptItems ?? [], [lesson]);
    const people = useMemo(() => {
        const peopleById = new Map<string, { id: string; name: string }>();
        attempts.forEach((attempt) => {
            peopleById.set(attempt.userId, { id: attempt.userId, name: attempt.userName });
        });
        return [...peopleById.values()].sort((a, b) => a.name.localeCompare(b.name));
    }, [attempts]);
    const filteredAttempts = attempts.filter((attempt) => {
        const personMatches = personFilter === "all" || attempt.userId === personFilter;
        const resultMatches =
            resultFilter === "all" ||
            (resultFilter === "passed" && attempt.passed) ||
            (resultFilter === "failed" && !attempt.passed);
        return personMatches && resultMatches;
    });
    const avatarStorageKeys = filteredAttempts
        .map((attempt) => attempt.avatarStorageKey)
        .filter((key): key is string => Boolean(key));
    const { data: avatarUrlByStorageKey } = useFilePreviewUrls(avatarStorageKeys);

    if (!open || !lesson) {
        return null;
    }

    return (
        <div className="team-progress-dialog__backdrop" role="presentation" onClick={onClose}>
            <div
                className="team-progress-dialog"
                role="dialog"
                aria-modal="true"
                onClick={(event) => event.stopPropagation()}
            >
                <div className="team-progress-dialog__header">
                    <p className="team-progress-dialog__eyebrow">Quiz review</p>
                    <h3>{lesson.lesson || "Quiz attempts"}</h3>
                    <button type="button" aria-label="Close quiz review" onClick={onClose}>
                        <CloseOutlinedIcon />
                    </button>
                </div>
                <div className="team-progress-dialog__body">
                    <div className="team-progress-dialog__filters">
                        <select
                            className="ui-select ui-select--sm"
                            value={personFilter}
                            onChange={(event) => setPersonFilter(event.target.value)}
                        >
                            <option value="all">All people</option>
                            {people.map((person) => (
                                <option key={person.id} value={person.id}>
                                    {person.name}
                                </option>
                            ))}
                        </select>
                        <select
                            className="ui-select ui-select--sm"
                            value={resultFilter}
                            onChange={(event) => setResultFilter(event.target.value)}
                        >
                            <option value="all">All results</option>
                            <option value="passed">Passed</option>
                            <option value="failed">Failed</option>
                        </select>
                        <span>
                            {filteredAttempts.length} of {attempts.length} shown
                            {lesson.attempts > attempts.length ? ` (${lesson.attempts} total)` : ""}
                        </span>
                    </div>
                    <div className="team-progress-dialog__attempts">
                        {filteredAttempts.length === 0 && (
                            <p className="team-progress-dialog__empty">
                                No attempts match these filters.
                            </p>
                        )}
                        {filteredAttempts.map((attempt) => (
                            <div key={attempt.id} className="team-progress-dialog__attempt">
                                <div className="team-progress-dialog__attempt-user">
                                    <UserAvatar
                                        user={{
                                            name: attempt.userName,
                                            avatarStorageKey: attempt.avatarStorageKey,
                                            avatarColor: attempt.avatarColor,
                                        }}
                                        previewUrl={
                                            attempt.avatarStorageKey
                                                ? avatarUrlByStorageKey?.[attempt.avatarStorageKey]
                                                : undefined
                                        }
                                        size={34}
                                    />
                                    <div>
                                        <p>{attempt.userName}</p>
                                        <p>
                                            {attempt.activityTitle} - attempt {attempt.attemptNumber}
                                        </p>
                                    </div>
                                </div>
                                <div className="team-progress-dialog__attempt-meta">
                                    <span
                                        className={
                                            attempt.passed
                                                ? "team-progress-chip team-progress-chip--success"
                                                : "team-progress-chip team-progress-chip--warning"
                                        }
                                    >
                                        {attempt.passed ? "Passed" : "Failed"}
                                    </span>
                                    <strong>{attempt.score}%</strong>
                                    <span>
                                        {attempt.correctCount}/{attempt.totalCount}
                                    </span>
                                    <span>{formatDateShort(attempt.createdAt)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

/** Team progress dashboard for team leads and admins. */
export function TeamProgressDashboard() {
    const { user } = useCurrentUser();
    const [scope, setScope] = useState<string>("team");
    const [period, setPeriod] = useState<DashboardPeriod>("month");
    const [includeTeamLead, setIncludeTeamLead] = useState(false);
    const [selectedLesson, setSelectedLesson] =
        useState<TeamDashboardLowConfidenceLessonV1 | null>(null);
    const [activityRange, setActivityRange] = useState<"24h" | "7d" | "30d">("24h");

    const { data, isLoading, error } = useTeamDashboardQuery(period);
    const avatarStorageKeys = [
        ...(data?.members ?? []).map((member) => member.avatarStorageKey),
        ...(data?.recentActivity ?? []).map((item) => item.avatarStorageKey),
    ].filter((key): key is string => Boolean(key));
    const { data: avatarUrlByStorageKey } = useFilePreviewUrls(avatarStorageKeys);

    if (user && user.role !== "teamlead" && user.role !== "admin") {
        return <Navigate to="/library" replace />;
    }

    if (isLoading) {
        return <LoadingBlock label="Loading team progress…" />;
    }

    if (error || !data) {
        return (
            <ErrorAlert
                message={getApiErrorMessage(error, "Failed to load team progress dashboard.")}
            />
        );
    }

    const allMembers = (data.members ?? []).map((member) => ({
        ...member,
        completedInPeriod: member.completedByPeriod?.[period] ?? member.completedInPeriod,
        quiz: member.quizByPeriod?.[period] ?? member.quiz,
    }));
    const hasTeamLead = allMembers.some((member) => member.isTeamLead);
    const leadId = allMembers.find((member) => member.isTeamLead)?.id ?? null;
    const members = allMembers.filter((member) => includeTeamLead || !member.isTeamLead);
    const selectedMember = members.find((member) => member.id === scope);
    const isTeam = scope === "team";
    const scopeOptions = [
        { id: "team", label: "Whole team" },
        ...members.map((member) => ({ id: member.id, label: member.name })),
    ];
    const periodLabel =
        PERIOD_OPTIONS.find((item) => item.id === period)?.label.toLowerCase() ?? "this month";
    const teamAverageQuizScore = averageQuizScore(members);
    const learnersInProgress = members.filter((member) => member.status === "in-progress").length;
    const activeRoadmapCount = new Set(
        members.flatMap((member) => member.roadmaps.map((roadmap) => roadmap.id)),
    ).size;
    const visibleUserIds = new Set(members.map((member) => member.id));
    const displayedWeekly = aggregateWeeklyActivity(data.weekly ?? [], members);
    const displayedLowConfidenceLessons = buildLowConfidenceLessonsForMembers(
        data.lowConfidenceLessons ?? [],
        leadId,
        includeTeamLead,
    );
    const displayedRecentActivity = (data.recentActivity ?? []).filter((item) =>
        visibleUserIds.has(item.userId),
    );
    const filteredActivity = displayedRecentActivity.filter((item) => {
        if (activityRange === "30d") return true;
        if (activityRange === "7d") {
            return (
                item.when.includes("min") ||
                item.when.includes("h") ||
                item.when === "yesterday" ||
                item.when.includes("d")
            );
        }
        return item.when.includes("min") || item.when.includes("h");
    });

    const kpis = isTeam
        ? [
              { label: "Roadmaps in progress", value: activeRoadmapCount, sub: "across team" },
              {
                  label: "Lessons completed",
                  value: members.reduce((total, member) => total + member.completedInPeriod, 0),
                  sub: periodLabel,
              },
              {
                  label: "Avg quiz score",
                  value: formatQuizScore(teamAverageQuizScore),
                  sub: teamAverageQuizScore === null ? "no attempts yet" : undefined,
                  accent: teamAverageQuizScore === null ? COLORS.mute : COLORS.success,
              },
              {
                  label: "Learners in progress",
                  value: learnersInProgress,
                  sub: "started learning",
                  accent: COLORS.blue,
              },
          ]
        : [
              {
                  label: "Active roadmaps",
                  value: selectedMember?.roadmapCount ?? 0,
                  sub: "assigned",
              },
              {
                  label: "Lessons completed",
                  value: selectedMember?.roadmapCompletedCount ?? 0,
                  sub: `/ ${selectedMember?.roadmapLessonCount ?? 0} across roadmaps`,
              },
              {
                  label: "Avg quiz score",
                  value: formatQuizScore(selectedMember?.quiz),
                  sub: selectedMember?.quiz === null ? "no attempts yet" : undefined,
                  accent:
                      (selectedMember?.quiz ?? 0) >= 85 ? COLORS.success : COLORS.ink,
              },
              {
                  label: "Learning status",
                  value:
                      selectedMember?.status === "done"
                          ? "Done"
                          : selectedMember?.status === "in-progress"
                            ? "Active"
                            : "New",
                  sub: selectedMember?.lastActive ?? "",
                  accent: COLORS.blue,
              },
          ];

    const visibleRoadmapGroups = selectedMember
        ? data.individualRoadmapsByMemberId?.[selectedMember.id] ?? []
        : [];
    const visibleLessonCount = visibleRoadmapGroups.reduce(
        (total, group) => total + group.lessonCount,
        0,
    );
    const displayedRoadmaps = isTeam
        ? buildRoadmapProgressFromMembers(members, data.individualRoadmapsByMemberId)
        : visibleRoadmapGroups
              .filter((group) => group.id !== "standalone-lessons")
              .map((group, index) => ({
                  id: group.id,
                  name: group.title,
                  learners: 1,
                  lessonCount: group.lessonCount,
                  progress: group.progress,
                  color: ["#0009DC", "#F0348E", "#42B1CF", "#FF642D"][index % 4],
              }));

    const maxSingleValue = Math.max(
        1,
        ...displayedWeekly.flatMap((week) => [week.lessons, week.quizzes]),
    );
    const bestWeek = displayedWeekly.reduce(
        (current, week) =>
            week.lessons + week.quizzes > current.lessons + current.quizzes ? week : current,
        displayedWeekly[0] ?? { label: "n/a", lessons: 0, quizzes: 0 },
    );

    return (
        <div className="team-progress">
            <div className="team-progress__hero">
                <div>
                    <p className="team-progress__eyebrow">Team progress - {data.teamName}</p>
                    <h1>{isTeam ? "How the team is doing" : selectedMember?.name}</h1>
                    <p className="team-progress__lede">
                        {isTeam
                            ? `${members.length} learners across ${activeRoadmapCount} active roadmaps.`
                            : `${selectedMember?.role || "Member"} - ${selectedMember?.roadmap || "No roadmap assigned"}`}
                    </p>
                </div>
                <div className="team-progress__controls">
                    <span className="team-progress__field">
                        <span className="team-progress__field-icon" aria-hidden="true">
                            {isTeam ? <GroupsOutlinedIcon /> : <PersonOutlineOutlinedIcon />}
                        </span>
                        <select className="ui-select" value={scope} onChange={(event) => setScope(event.target.value)}>
                            {scopeOptions.map((option) => (
                                <option key={option.id} value={option.id}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </span>
                    <span className="team-progress__field">
                        <span className="team-progress__field-icon" aria-hidden="true">
                            <CalendarMonthOutlinedIcon />
                        </span>
                        <select
                            className="ui-select"
                            value={period}
                            onChange={(event) => setPeriod(event.target.value as DashboardPeriod)}
                        >
                            {PERIOD_OPTIONS.map((option) => (
                                <option key={option.id} value={option.id}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </span>
                    {hasTeamLead && (
                        <label className="team-progress__include-me">
                            <input
                                type="checkbox"
                                checked={includeTeamLead}
                                onChange={(event) => {
                                    const shouldInclude = event.target.checked;
                                    if (!shouldInclude && selectedMember?.isTeamLead) {
                                        setScope("team");
                                    }
                                    setIncludeTeamLead(shouldInclude);
                                }}
                            />
                            Include me
                        </label>
                    )}
                    <Link className="team-progress__assign-btn" to="/library?tab=roadmaps">
                        <AddOutlinedIcon /> Assign roadmap
                    </Link>
                </div>
            </div>

            <div className="team-progress__kpis">
                {kpis.map((kpi) => (
                    <article key={kpi.label} className="team-progress-kpi">
                        <p className="team-progress-kpi__label">{kpi.label}</p>
                        <p className="team-progress-kpi__value" style={{ color: kpi.accent || COLORS.ink }}>
                            {kpi.value}
                            {kpi.sub && <span>{kpi.sub}</span>}
                        </p>
                    </article>
                ))}
            </div>

            <div className="team-progress__grid team-progress__grid--two">
                <section className="team-progress-widget">
                    <div className="team-progress-widget__header">
                        <div>
                            <p className="team-progress-widget__eyebrow">Roadmap progress</p>
                            <h2>
                                {isTeam
                                    ? "Active roadmaps across team"
                                    : "Assigned roadmaps"}
                            </h2>
                        </div>
                        <span className="team-progress-chip">
                            {displayedRoadmaps.length} roadmap
                            {displayedRoadmaps.length === 1 ? "" : "s"}
                        </span>
                    </div>
                    <div className="team-progress-widget__scroll">
                        {displayedRoadmaps.length === 0 && (
                            <p>No active roadmap assignments yet.</p>
                        )}
                        {displayedRoadmaps.map((roadmap) => (
                            <div key={roadmap.id} className="team-progress-roadmap-row">
                                <div className="team-progress-roadmap-row__meta">
                                    <span style={{ backgroundColor: roadmap.color }} />
                                    <strong>{roadmap.name}</strong>
                                    <span>
                                        {isTeam
                                            ? `${roadmap.learners} learner${roadmap.learners === 1 ? "" : "s"}`
                                            : `${roadmap.lessonCount} lesson${roadmap.lessonCount === 1 ? "" : "s"}`}
                                    </span>
                                    <strong>{roadmap.progress}%</strong>
                                </div>
                                <ProgressBar value={roadmap.progress} color={roadmap.color} />
                            </div>
                        ))}
                    </div>
                </section>

                <section className="team-progress-widget">
                    <div className="team-progress-widget__header">
                        <div>
                            <p className="team-progress-widget__eyebrow">Activity over time</p>
                            <h2>Last 8 weeks</h2>
                            <p className="team-progress-widget__hint">
                                Weekly completed lessons and quiz attempts.
                            </p>
                        </div>
                        <div className="team-progress-legend">
                            <span><i className="team-progress-legend__dot team-progress-legend__dot--lessons" />Lessons</span>
                            <span><i className="team-progress-legend__dot team-progress-legend__dot--quizzes" />Quizzes</span>
                        </div>
                    </div>
                    <div className="team-progress-chart">
                        {displayedWeekly.map((week) => {
                            const lessonsHeight =
                                week.lessons === 0 ? 0 : Math.max(5, (week.lessons / maxSingleValue) * 100);
                            const quizzesHeight =
                                week.quizzes === 0 ? 0 : Math.max(5, (week.quizzes / maxSingleValue) * 100);
                            const isBest = week.label === bestWeek.label;
                            return (
                                <div key={week.label} className="team-progress-chart__column">
                                    <div
                                        className={
                                            isBest
                                                ? "team-progress-chart__bars team-progress-chart__bars--best"
                                                : "team-progress-chart__bars"
                                        }
                                    >
                                        <div
                                            className="team-progress-chart__bar team-progress-chart__bar--lessons"
                                            style={{ height: `${lessonsHeight}%` }}
                                        />
                                        <div
                                            className="team-progress-chart__bar team-progress-chart__bar--quizzes"
                                            style={{ height: `${quizzesHeight}%` }}
                                        />
                                    </div>
                                    <span>{week.label.split(" ")[1]}</span>
                                </div>
                            );
                        })}
                    </div>
                    <p className="team-progress-chart__footer">
                        Total: <strong>{displayedWeekly.reduce((t, w) => t + w.lessons, 0)}</strong> lessons +{" "}
                        <strong>{displayedWeekly.reduce((t, w) => t + w.quizzes, 0)}</strong> quizzes. Best week:{" "}
                        <strong>{bestWeek.label}</strong>
                    </p>
                </section>
            </div>

            <section className="team-progress-section">
                <div className="team-progress-section__header">
                    <div>
                        {!isTeam && (
                            <button type="button" onClick={() => setScope("team")}>
                                <ArrowBackOutlinedIcon /> Team
                            </button>
                        )}
                        <h2>{isTeam ? "Team members" : "Roadmaps and lessons"}</h2>
                        {!isTeam && <p>{selectedMember?.name}</p>}
                    </div>
                    <p>
                        {isTeam
                            ? `${members.length} members`
                            : `${visibleRoadmapGroups.length} roadmap${visibleRoadmapGroups.length === 1 ? "" : "s"} - ${visibleLessonCount} lessons total`}
                    </p>
                </div>

                {isTeam ? (
                    <div className="team-progress-table">
                        <div className="team-progress-table__head">
                            {["Member", "Role", "Roadmaps", "Progress", "Last active", "Quiz avg", ""].map(
                                (heading) => (
                                    <span key={heading}>{heading}</span>
                                ),
                            )}
                        </div>
                        {members.map((member) => (
                            <div key={member.id} className="team-progress-table__row">
                                <div className="team-progress-table__member">
                                    <UserAvatar
                                        user={member}
                                        previewUrl={
                                            member.avatarStorageKey
                                                ? avatarUrlByStorageKey?.[member.avatarStorageKey]
                                                : undefined
                                        }
                                        size={36}
                                    />
                                    <div>
                                        <strong>{member.name}</strong>
                                        <StatusDot status={member.status} />
                                    </div>
                                </div>
                                <span>{member.role}</span>
                                <span>{member.roadmap}</span>
                                <div className="team-progress-table__progress">
                                    <ProgressBar
                                        value={member.progress}
                                        color={
                                            member.status === "done" ? COLORS.success : COLORS.blue
                                        }
                                        height={7}
                                    />
                                    <span>{member.progress}%</span>
                                </div>
                                <span>{member.lastActive}</span>
                                <span
                                    style={{
                                        color:
                                            member.quiz == null
                                                ? COLORS.mute
                                                : member.quiz >= 85
                                                  ? COLORS.success
                                                  : member.quiz < 70
                                                    ? COLORS.orange
                                                    : COLORS.ink,
                                    }}
                                >
                                    {formatQuizScore(member.quiz)}
                                </span>
                                <button type="button" onClick={() => setScope(member.id)}>
                                    Open
                                </button>
                            </div>
                        ))}
                    </div>
                ) : (
                    <IndividualRoadmapGroups groups={visibleRoadmapGroups} />
                )}
            </section>

            <div className="team-progress__grid team-progress__grid--two">
                <section className="team-progress-widget">
                    <p className="team-progress-widget__eyebrow">Needs review</p>
                    <h2>Low quiz confidence</h2>
                    <div className="team-progress-widget__scroll">
                        {displayedLowConfidenceLessons.length === 0 && (
                            <p>No low-confidence quiz results yet.</p>
                        )}
                        {displayedLowConfidenceLessons.map((item) => (
                            <div key={item.id} className="team-progress-review-row">
                                <span className="team-progress-review-row__icon"><ErrorOutlineOutlinedIcon /></span>
                                <div>
                                    <strong>{item.lesson}</strong>
                                    <p>
                                        {item.attempts} attempt{item.attempts === 1 ? "" : "s"} - avg quiz{" "}
                                        {item.avgScore}%
                                    </p>
                                </div>
                                <button type="button" onClick={() => setSelectedLesson(item)}>
                                    Review
                                </button>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="team-progress-widget">
                    <div className="team-progress-widget__header">
                        <div>
                            <p className="team-progress-widget__eyebrow">Recent activity</p>
                            <h2>
                                {activityRange === "24h"
                                    ? "Last 24 hours"
                                    : activityRange === "7d"
                                      ? "Last 7 days"
                                      : "Last 30 days"}
                            </h2>
                        </div>
                        <div className="team-progress-range">
                            {[
                                ["24h", "24h"],
                                ["7d", "7 days"],
                                ["30d", "30 days"],
                            ].map(([id, label]) => (
                                <button
                                    key={id}
                                    type="button"
                                    className={
                                        activityRange === id ? "team-progress-range__btn--active" : ""
                                    }
                                    onClick={() => setActivityRange(id as typeof activityRange)}
                                >
                                    {label}
                                </button>
                            ))}
                        </div>
                    </div>
                    <div className="team-progress-widget__scroll">
                        {filteredActivity.length === 0 && <p>No activity in this window.</p>}
                        {filteredActivity.map((item) => {
                            const quizPassed = item.passed ?? (item.score ?? 0) >= 80;
                            const activityColor =
                                item.kind === "quiz"
                                    ? quizPassed
                                        ? COLORS.success
                                        : COLORS.orange
                                    : COLORS.blue;
                            return (
                                <div key={item.id} className="team-progress-activity-row">
                                    <UserAvatar
                                        user={{
                                            name: item.who,
                                            avatarStorageKey: item.avatarStorageKey,
                                            avatarColor: item.avatarColor,
                                        }}
                                        previewUrl={
                                            item.avatarStorageKey
                                                ? avatarUrlByStorageKey?.[item.avatarStorageKey]
                                                : undefined
                                        }
                                        size={30}
                                    />
                                    <div>
                                        <p>
                                            <strong>{item.who}</strong> {item.action}{" "}
                                            <span
                                                className="team-progress-chip team-progress-chip--feed"
                                                style={{ color: activityColor }}
                                            >
                                                {item.kind === "quiz" ? <QuizOutlinedIcon /> : <RouteOutlinedIcon />} {item.what}
                                            </span>
                                        </p>
                                        <small>{item.when}</small>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            </div>

            <QuizAttemptsDialog
                lesson={selectedLesson}
                open={Boolean(selectedLesson)}
                onClose={() => setSelectedLesson(null)}
            />
        </div>
    );
}

function IndividualRoadmapGroups({ groups }: { groups: TeamDashboardIndividualRoadmapV1[] }) {
    const stateMap = {
        completed: { label: "Completed", color: COLORS.success, bg: "rgba(34,158,90,0.10)" },
        "in-progress": { label: "In progress", color: COLORS.blue, bg: COLORS.blue50 },
    } as const;

    return (
        <div className="team-progress-individual">
            {groups.length === 0 && <p>No assigned roadmaps or lessons yet.</p>}
            {groups.map((group) => (
                <div key={group.id} className="team-progress-individual__group">
                    <div className="team-progress-individual__group-header">
                        <div>
                            <strong>{group.title}</strong>
                            <p>
                                {group.completedCount} / {group.lessonCount} lessons completed
                            </p>
                        </div>
                        <div className="team-progress-individual__group-progress">
                            <ProgressBar
                                value={group.progress}
                                color={group.progress >= 100 ? COLORS.success : COLORS.blue}
                                height={7}
                            />
                            <span>{group.progress}%</span>
                        </div>
                    </div>
                    <div className="team-progress-individual__head">
                        {["Lesson", "State", "Score", "When"].map((heading) => (
                            <span key={heading}>{heading}</span>
                        ))}
                    </div>
                    {group.lessons.map((lesson) => {
                        const state = stateMap[lesson.state as keyof typeof stateMap] || {
                            label: "Not started",
                            color: COLORS.mute,
                            bg: "#F2F1F3",
                        };
                        return (
                            <div key={lesson.id} className="team-progress-individual__row">
                                <strong>{lesson.title}</strong>
                                <span
                                    className="team-progress-chip team-progress-chip--state"
                                    style={{ color: state.color, backgroundColor: state.bg }}
                                >
                                    {state.label}
                                </span>
                                <span>{lesson.score === null ? "N/A" : `${lesson.score}%`}</span>
                                <span>{lesson.when}</span>
                            </div>
                        );
                    })}
                </div>
            ))}
        </div>
    );
}
