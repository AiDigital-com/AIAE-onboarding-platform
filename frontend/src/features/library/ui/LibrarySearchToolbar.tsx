import { useEffect, useRef, useState, type ReactNode } from "react";
import CheckOutlinedIcon from "@mui/icons-material/CheckOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import KeyboardArrowDownOutlinedIcon from "@mui/icons-material/KeyboardArrowDownOutlined";
import RestartAltOutlinedIcon from "@mui/icons-material/RestartAltOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import SortOutlinedIcon from "@mui/icons-material/SortOutlined";
import TuneOutlinedIcon from "@mui/icons-material/TuneOutlined";
import { Button } from "@/shared/ui/Button";
import { LIBRARY_SORT_OPTIONS } from "./library-utils";

interface LibrarySearchToolbarProps {
    searchLabel: string;
    placeholder: string;
    query: string;
    onQueryChange: (value: string) => void;
    resultCount?: number;
    totalCount?: number;
    filtersOpen?: boolean;
    onToggleFilters?: () => void;
    hasActiveFilters?: boolean;
    onReset?: () => void;
    filterContent?: ReactNode;
    sort?: string;
    onSortChange?: (value: string) => void;
    sortOptions?: ReadonlyArray<{ value: string; label: string }>;
}

export function LibrarySearchToolbar({
    searchLabel,
    placeholder,
    query,
    onQueryChange,
    resultCount = 0,
    totalCount = 0,
    filtersOpen = false,
    onToggleFilters,
    hasActiveFilters = false,
    onReset,
    filterContent,
    sort = "recent",
    onSortChange,
    sortOptions = LIBRARY_SORT_OPTIONS,
}: LibrarySearchToolbarProps) {
    const [sortOpen, setSortOpen] = useState(false);
    const filtersRef = useRef<HTMLDivElement>(null);
    const sortRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClick = (event: MouseEvent) => {
            const target = event.target as Element;
            // MUI Select/Menu/Autocomplete dropdowns render into a portal on document.body,
            // outside filtersRef. Treat clicks inside those surfaces as inside the panel so
            // selecting an option doesn't close (and unmount) the filters before it applies.
            const isPortalSurface = Boolean(
                target.closest(".MuiAutocomplete-popper, .MuiPopover-root, .MuiMenu-root, .MuiModal-root"),
            );

            if (
                filtersOpen &&
                filtersRef.current &&
                !filtersRef.current.contains(target) &&
                !isPortalSurface
            ) {
                onToggleFilters?.();
            }
            if (sortOpen && sortRef.current && !sortRef.current.contains(event.target as Node)) {
                setSortOpen(false);
            }
        };

        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, [filtersOpen, onToggleFilters, sortOpen]);

    return (
        <div className="library-search">
            <div className="library-search__field-wrap">
                <p className="library-search__label">{searchLabel}</p>
                <div className="library-search__input-wrap">
                    <SearchOutlinedIcon aria-hidden="true" />
                    <input
                        className="library-search__input"
                        value={query}
                        placeholder={placeholder}
                        onChange={(event) => onQueryChange(event.target.value)}
                    />
                    {query && (
                        <button
                            type="button"
                            className="library-search__clear"
                            aria-label={`Clear ${searchLabel.toLowerCase()}`}
                            onClick={() => onQueryChange("")}
                        >
                            <CloseOutlinedIcon />
                        </button>
                    )}
                </div>
            </div>

            <span className="library-search__count-pill">{`${resultCount}/${totalCount}`}</span>

            <div style={{ position: "relative" }} ref={filtersRef}>
                <Button
                    variant={filtersOpen ? "primary" : "ghost"}
                    size="sm"
                    onClick={onToggleFilters}
                >
                    <TuneOutlinedIcon /> Filters
                </Button>
                {filtersOpen && (
                    <div className="library-filters-popover">
                        {filterContent}
                        {hasActiveFilters && (
                            <Button
                                variant="ghost"
                                size="sm"
                                className="library-filters-popover__reset"
                                onClick={onReset}
                            >
                                <RestartAltOutlinedIcon /> Reset filters
                            </Button>
                        )}
                    </div>
                )}
            </div>

            <div style={{ position: "relative" }} ref={sortRef}>
                <Button variant="ghost" size="sm" onClick={() => setSortOpen((prev) => !prev)}>
                    <SortOutlinedIcon /> Sort <KeyboardArrowDownOutlinedIcon />
                </Button>
                {sortOpen && (
                    <div className="library-menu">
                        {sortOptions.map((option) => (
                            <button
                                key={option.value}
                                type="button"
                                className="library-menu__item"
                                onClick={() => {
                                    onSortChange?.(option.value);
                                    setSortOpen(false);
                                }}
                            >
                                {option.label}
                                {option.value === sort && <CheckOutlinedIcon />}
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
