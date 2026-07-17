import { SignIn } from "@clerk/clerk-react";
import "./login.css";

// Login — Clerk SSO sign-in (the only auth mode). Clerk renders and manages the
// full sign-in flow; on success it redirects per
// VITE_CLERK_SIGN_IN_FORCE_REDIRECT_URL. There is no mock-login form.

export default function Login() {
    return (
        <main className="login-page">
            <div className="login-shell">
                <section className="login-brand">
                    <div className="login-brand__logo" aria-hidden="true">AI</div>
                    <p className="login-brand__eyebrow">AI Digital</p>
                    <h1 className="login-brand__title">Learning Hub</h1>
                    <p className="login-brand__subtitle">
                        Sign in to access the internal learning library, manage lessons, and track team progress.
                    </p>
                </section>
                <section className="login-panel">
                    <h2 className="login-panel__title">Log in</h2>
                    <SignIn
                        appearance={{
                            variables: {
                                colorPrimary: "var(--brand-yves-klein-blue)",
                                borderRadius: "var(--radius-lg)",
                            },
                            elements: {
                                rootBox: "login-panel__clerk-root",
                                cardBox: "login-panel__clerk-box",
                                card: "login-panel__clerk-card",
                                main: "login-panel__clerk-main",
                                footer: "login-panel__clerk-footer",
                            },
                        }}
                    />
                </section>
            </div>
        </main>
    );
}
