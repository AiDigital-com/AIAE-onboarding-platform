import type { ButtonHTMLAttributes, ReactNode } from "react";
import "./button.css";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger" | "dark";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant;
    size?: "sm" | "md";
    block?: boolean;
    startIcon?: ReactNode;
    endIcon?: ReactNode;
}

export function Button({
    variant = "secondary",
    size = "md",
    block = false,
    startIcon,
    endIcon,
    className = "",
    children,
    type = "button",
    ...props
}: ButtonProps) {
    const classes = [
        "ui-btn",
        `ui-btn--${variant}`,
        size === "sm" ? "ui-btn--sm" : "",
        block ? "ui-btn--block" : "",
        className,
    ]
        .filter(Boolean)
        .join(" ");

    return (
        <button type={type} className={classes} {...props}>
            {startIcon}
            {children}
            {endIcon}
        </button>
    );
}
