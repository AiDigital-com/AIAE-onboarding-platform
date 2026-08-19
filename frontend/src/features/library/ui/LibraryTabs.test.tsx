import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { LibraryTabDefinition } from "../api/types";
import { LibraryTabs } from "./LibraryTabs";

function Wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient();
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("LibraryTabs", () => {
    afterEach(() => {
        cleanup();
    });

    it("should render only the tabs it is given test", () => {
        // Given: a Member's permission-filtered tab list — only Lessons
        const tabs: LibraryTabDefinition[] = [{ value: "lessons", label: "Lessons" }];

        // When:
        render(
            <Wrapper>
                <LibraryTabs tabs={tabs} activeTab="lessons" onTabChange={vi.fn()} />
            </Wrapper>,
        );

        // Then:
        expect(screen.getByText("Lessons")).toBeTruthy();
        expect(screen.queryByText("Materials")).toBeNull();
        expect(screen.queryByText("Roadmaps")).toBeNull();
    });

    it("should render every tab it is given for a Team Lead test", () => {
        // Given: all three tabs visible
        const tabs: LibraryTabDefinition[] = [
            { value: "materials", label: "Materials" },
            { value: "lessons", label: "Lessons" },
            { value: "roadmaps", label: "Roadmaps" },
        ];

        // When:
        render(
            <Wrapper>
                <LibraryTabs tabs={tabs} activeTab="materials" onTabChange={vi.fn()} />
            </Wrapper>,
        );

        // Then:
        expect(screen.getByText("Materials")).toBeTruthy();
        expect(screen.getByText("Lessons")).toBeTruthy();
        expect(screen.getByText("Roadmaps")).toBeTruthy();
    });

    it("should show an unknown-count placeholder until the count query resolves test", () => {
        // Given:
        const tabs: LibraryTabDefinition[] = [{ value: "lessons", label: "Lessons" }];

        // When: no counts supplied
        render(
            <Wrapper>
                <LibraryTabs tabs={tabs} activeTab="lessons" onTabChange={vi.fn()} />
            </Wrapper>,
        );

        // Then:
        expect(screen.getByText("–")).toBeTruthy();
    });
});
