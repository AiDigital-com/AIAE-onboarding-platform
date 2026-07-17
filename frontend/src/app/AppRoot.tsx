import { lazy, Suspense, useEffect, useRef } from "react";
import { QueryClientProvider, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@clerk/clerk-react";
import { ThemeProvider, createTheme, CssBaseline } from "@mui/material";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { queryClient } from "../shared/api/queryClient";
import App from "../App";
import { AuthProvider } from "../shared/auth/AuthProvider";
import { ProtectedRoute } from "../shared/auth/ProtectedRoute";
import { TaskTrayProvider } from "../shared/context/TaskTrayContext";
import { GlobalApiErrorToast } from "../shared/ui/GlobalApiErrorToast";
import { LoadingBlock } from "../shared/ui/LoadingBlock";

// Route pages are code-split — each only downloads when a user actually navigates
// there, instead of every page (Admin, Teams, Roadmaps, ...) riding in the initial bundle.
const Admin = lazy(() => import("../pages/Admin"));
const Home = lazy(() => import("../pages/Home"));
const LessonActivity = lazy(() => import("../pages/LessonActivity"));
const LessonDetail = lazy(() => import("../pages/LessonDetail"));
const Lessons = lazy(() => import("../pages/Lessons"));
const Library = lazy(() => import("../pages/Library"));
const Login = lazy(() => import("../pages/Login"));
const NotFoundPage = lazy(() => import("../pages/NotFoundPage"));
const Roadmaps = lazy(() => import("../pages/Roadmaps"));
const TeamPermissions = lazy(() => import("../pages/TeamPermissions"));
const TeamProgress = lazy(() => import("../pages/TeamProgress"));
const Teams = lazy(() => import("../pages/Teams"));

/**
 * Clears the React Query cache whenever the authenticated Clerk user changes or signs out,
 * preventing one user's cached data from leaking to the next session.
 */
function CacheIsolationBridge() {
    const { userId } = useAuth();
    const client = useQueryClient();
    const prevUserIdRef = useRef<string | null | undefined>(undefined);

    useEffect(() => {
        const prev = prevUserIdRef.current;
        prevUserIdRef.current = userId ?? null;
        if (prev !== undefined && (userId ?? null) !== prev) {
            client.clear();
        }
    }, [userId, client]);

    return null;
}

// Full MUI theme ported from the original app's AppProviders.jsx.
const muiTheme = createTheme({
    palette: {
        mode: "light",
        primary: {
            main: "#0009DC",
        },
        secondary: {
            main: "#FF7CF5",
        },
        background: {
            default: "#F9F9F9",
        },
    },
    shape: {
        borderRadius: 12,
    },
    typography: {
        fontFamily: "var(--ff-sans), 'Inter Variable', Inter, Arial, sans-serif",
    },
    components: {
        MuiButton: {
            defaultProps: {
                disableElevation: true,
            },
            styleOverrides: {
                root: {
                    borderRadius: 999,
                    fontSize: 12,
                    fontWeight: 700,
                    letterSpacing: "0.06em",
                    textTransform: "uppercase",
                },
                sizeMedium: {
                    minHeight: 42,
                    padding: "10px 18px",
                },
                sizeSmall: {
                    minHeight: 32,
                    padding: "7px 14px",
                    fontSize: 11,
                },
            },
            variants: [
                {
                    props: { variant: "contained", color: "primary" },
                    style: {
                        backgroundColor: "#0009DC",
                        color: "#fff",
                        "&:hover": { backgroundColor: "#0007B8" },
                    },
                },
                {
                    props: { variant: "outlined", color: "primary" },
                    style: {
                        borderColor: "rgba(0, 9, 220, 0.24)",
                        color: "#0009DC",
                        "&:hover": {
                            borderColor: "#0009DC",
                            backgroundColor: "rgba(0, 9, 220, 0.04)",
                        },
                    },
                },
                {
                    props: { variant: "text", color: "primary" },
                    style: {
                        color: "#0009DC",
                        "&:hover": { backgroundColor: "rgba(0, 9, 220, 0.04)" },
                    },
                },
            ],
        },
    },
});

/** Router + providers — main.tsx mounts only this component. */
export function AppRoot() {
    return (
        <BrowserRouter>
            <ThemeProvider theme={muiTheme}>
                <CssBaseline />
                <AuthProvider>
                    <QueryClientProvider client={queryClient}>
                        <CacheIsolationBridge />
                        <TaskTrayProvider>
                            <GlobalApiErrorToast />
                            <Suspense fallback={<LoadingBlock />}>
                                <Routes>
                                    <Route path="/login" element={<Login />} />
                                    <Route
                                        path="/"
                                        element={
                                            <ProtectedRoute>
                                                <App />
                                            </ProtectedRoute>
                                        }
                                    >
                                        <Route index element={<Home />} />
                                        <Route path="library" element={<Library />} />
                                        <Route path="lessons" element={<Lessons />} />
                                        <Route path="lessons/:id" element={<LessonDetail />} />
                                        <Route
                                            path="lessons/:id/activities/:activityId"
                                            element={<LessonActivity />}
                                        />
                                        <Route path="roadmaps" element={<Roadmaps />} />
                                        <Route path="team-progress" element={<TeamProgress />} />
                                        <Route path="teams" element={<Teams />} />
                                        <Route path="admin" element={<Admin />} />
                                        <Route path="team-permissions" element={<TeamPermissions />} />
                                    </Route>
                                    <Route path="*" element={<NotFoundPage />} />
                                </Routes>
                            </Suspense>
                        </TaskTrayProvider>
                    </QueryClientProvider>
                </AuthProvider>
            </ThemeProvider>
        </BrowserRouter>
    );
}
