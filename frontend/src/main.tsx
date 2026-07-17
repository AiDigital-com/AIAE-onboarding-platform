import React from "react";
import ReactDOM from "react-dom/client";
import { AppRoot } from "./app/AppRoot";
import { runtimeConfig } from "./shared/config/runtime";

// Self-hosted brand fonts (match the original app: Inter body + Barlow Semi Condensed display 700/900).
import "@fontsource-variable/inter";
import "@fontsource/barlow-semi-condensed/700.css";
import "@fontsource/barlow-semi-condensed/900.css";
import "./shared/ui/base/tokens.css";
import "./shared/ui/base/reset.css";

runtimeConfig.validate();

ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
        <AppRoot />
    </React.StrictMode>,
);
