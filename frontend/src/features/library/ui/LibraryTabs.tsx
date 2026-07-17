import type { ReactNode } from "react";
import { Tab, Tabs } from "@mui/material";

const libraryTabs = [
    { value: "materials", label: "Materials" },
    { value: "lessons", label: "Lessons" },
    { value: "roadmaps", label: "Roadmaps" },
] as const;

interface LibraryTabsProps {
    activeTab: string;
    onTabChange: (tab: string) => void;
    actionSlot?: ReactNode;
    counts?: Partial<Record<string, number>>;
}

export function LibraryTabs({ activeTab, onTabChange, actionSlot, counts = {} }: LibraryTabsProps) {
    return (
        <section className="library-tabs">
            <div className="library-tabs__row">
                <Tabs
                    value={activeTab}
                    onChange={(_, v: string) => onTabChange(v)}
                    variant="scrollable"
                    scrollButtons="auto"
                    sx={{
                        minHeight: 50,
                        "& .MuiTab-root": {
                            minHeight: 50,
                            padding: "0 20px",
                            fontSize: 14,
                            fontWeight: 800,
                            letterSpacing: "0.04em",
                            textTransform: "uppercase",
                            color: "#80808e",
                            transition: "color 0.16s ease",
                            "&.Mui-selected": { color: "#0009dc" },
                        },
                        "& .MuiTabs-indicator": {
                            height: 2,
                            backgroundColor: "#0009dc",
                        },
                    }}
                >
                    {libraryTabs.map((tab) => (
                        <Tab
                            key={tab.value}
                            value={tab.value}
                            label={
                                <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                                    {tab.label}
                                    <span className="library-tabs__count">
                                        {counts[tab.value] === undefined ? "–" : counts[tab.value]}
                                    </span>
                                </span>
                            }
                        />
                    ))}
                </Tabs>
                {actionSlot && <div className="library-tabs__slot">{actionSlot}</div>}
            </div>
        </section>
    );
}
