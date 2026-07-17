import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { TaskTrayProvider } from "@/shared/context/TaskTrayContext";
import { CreateLessonDialog } from "./CreateLessonDialog";

function Wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient();
    return (
        <QueryClientProvider client={queryClient}>
            <TaskTrayProvider>{children}</TaskTrayProvider>
        </QueryClientProvider>
    );
}

describe("CreateLessonDialog", () => {
    it("should render the MUI Autocomplete-based prompt form without crashingTest", () => {
        // Given / When: LessonPromptForm (rendered inside) uses MUI's Autocomplete, which
        // transitively pulls in react-transition-group — this is a smoke test for the
        // Vitest/MUI transition-group ESM resolution fix in vite.config.ts.
        render(
            <Wrapper>
                <CreateLessonDialog open materials={[]} onClose={() => {}} onLessonGenerated={() => {}} />
            </Wrapper>,
        );

        // Then:
        expect(screen.getByRole("button", { name: "Generate lesson" })).toBeTruthy();
    });
});
