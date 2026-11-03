import { useEffect, useId, useRef, useState } from "react";
import type { KeyboardEvent } from "react";

import { TEAM_SCOPE, TEAM_SCOPE_LABEL } from "../constants/teamScope";
import { filterMembersByQuery } from "../lib/filterMembersByQuery";
import type { ScopeOption, ScopePickerMember, TeamScopePickerProps } from "../model/teamScopePicker";

/**
 * Builds the option list for a query: the team-wide option is always first and is never filtered
 * out, so the learner can always get back to the team view; members are narrowed by
 * {@link filterMembersByQuery} (name, email, or role).
 */
export function buildScopeOptions(members: ScopePickerMember[], query: string): ScopeOption[] {
    return [
        { id: TEAM_SCOPE, label: TEAM_SCOPE_LABEL },
        ...filterMembersByQuery(members, query).map((member) => ({
            id: member.id,
            label: member.name,
            hint: [member.role, member.email].filter(Boolean).join(" · ") || undefined,
        })),
    ];
}

/**
 * Searchable scope picker for the Team Progress hero. Replaces the plain `<select>` so a team lead
 * can type a name, email, or role instead of scrolling a long list. Picking an option changes the
 * dashboard scope, so every KPI, chart, and table below follows the selection.
 *
 * <p>Implements the WAI-ARIA combobox pattern: the input is the combobox, the popup is a listbox,
 * Arrow keys move the highlight, Enter picks, Escape closes and restores the current label.
 */
export function TeamScopePicker({ members, scope, icon, onScopeChange }: TeamScopePickerProps) {
    const listboxId = useId();
    const rootRef = useRef<HTMLDivElement>(null);
    const [isOpen, setIsOpen] = useState(false);
    const [query, setQuery] = useState("");
    const [highlightedIndex, setHighlightedIndex] = useState(0);

    const selectedLabel =
        scope === TEAM_SCOPE
            ? TEAM_SCOPE_LABEL
            : (members.find((member) => member.id === scope)?.name ?? TEAM_SCOPE_LABEL);
    const options = buildScopeOptions(members, query);
    const hasMemberMatches = options.length > 1;
    const inputValue = isOpen ? query : selectedLabel;

    useEffect(() => {
        if (!isOpen) {
            return undefined;
        }
        function handlePointerDown(event: PointerEvent) {
            if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }
        document.addEventListener("pointerdown", handlePointerDown);
        return () => document.removeEventListener("pointerdown", handlePointerDown);
    }, [isOpen]);

    function open() {
        setQuery("");
        setHighlightedIndex(0);
        setIsOpen(true);
    }

    function close() {
        setIsOpen(false);
        setQuery("");
    }

    function choose(option: ScopeOption) {
        onScopeChange(option.id);
        close();
    }

    function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
        if (!isOpen) {
            if (event.key === "ArrowDown" || event.key === "ArrowUp" || event.key === "Enter") {
                event.preventDefault();
                open();
            }
            return;
        }
        switch (event.key) {
            case "ArrowDown":
                event.preventDefault();
                setHighlightedIndex((index) => Math.min(index + 1, options.length - 1));
                break;
            case "ArrowUp":
                event.preventDefault();
                setHighlightedIndex((index) => Math.max(index - 1, 0));
                break;
            case "Enter": {
                event.preventDefault();
                const option = options[highlightedIndex];
                if (option) {
                    choose(option);
                }
                break;
            }
            case "Escape":
                event.preventDefault();
                close();
                break;
            case "Tab":
                close();
                break;
            default:
                break;
        }
    }

    return (
        <div ref={rootRef} className={`team-scope-picker${isOpen ? " team-scope-picker--open" : ""}`}>
            <span className="team-scope-picker__icon" aria-hidden="true">
                {icon}
            </span>
            <input
                className="team-scope-picker__input"
                type="text"
                role="combobox"
                aria-label="Team member"
                aria-autocomplete="list"
                aria-expanded={isOpen}
                aria-controls={listboxId}
                aria-activedescendant={isOpen ? `${listboxId}-${highlightedIndex}` : undefined}
                autoComplete="off"
                placeholder={isOpen ? "Search by name, email, or role…" : undefined}
                value={inputValue}
                onFocus={open}
                onClick={() => {
                    if (!isOpen) {
                        open();
                    }
                }}
                onChange={(event) => {
                    setQuery(event.target.value);
                    setHighlightedIndex(0);
                    setIsOpen(true);
                }}
                onKeyDown={handleKeyDown}
            />
            {isOpen && (
                <ul id={listboxId} className="team-scope-picker__list" role="listbox" aria-label="Team members">
                    {options.map((option, index) => (
                        <li
                            key={option.id}
                            id={`${listboxId}-${index}`}
                            role="option"
                            aria-selected={option.id === scope}
                            className={[
                                "team-scope-picker__option",
                                index === highlightedIndex ? "team-scope-picker__option--active" : "",
                                option.id === scope ? "team-scope-picker__option--selected" : "",
                            ]
                                .filter(Boolean)
                                .join(" ")}
                            onMouseDown={(event) => event.preventDefault()}
                            onMouseEnter={() => setHighlightedIndex(index)}
                            onClick={() => choose(option)}
                        >
                            <span className="team-scope-picker__option-label">{option.label}</span>
                            {option.hint && <span className="team-scope-picker__option-hint">{option.hint}</span>}
                        </li>
                    ))}
                    {!hasMemberMatches && query.trim() && (
                        <li className="team-scope-picker__empty" role="presentation">
                            No members match “{query.trim()}”.
                        </li>
                    )}
                </ul>
            )}
        </div>
    );
}
