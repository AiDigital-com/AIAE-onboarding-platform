import { Outlet } from "react-router-dom";
import { AppShell } from "./app/AppShell";
import "./App.css";

/** Authenticated layout shell — child routes render in the outlet. */
export default function App() {
    return (
        <AppShell>
            <Outlet />
        </AppShell>
    );
}
