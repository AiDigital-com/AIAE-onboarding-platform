import { LibrarySearchToolbar } from "./LibrarySearchToolbar";
import { Autocomplete, Chip, MenuItem, TextField } from "@mui/material";

const enrollmentOptions = [
    { value: "all", label: "Any roadmap" },
    { value: "enrolled", label: "In My Roadmaps" },
    { value: "not-enrolled", label: "Not in My Roadmaps" },
];

interface RoadmapLibraryFiltersProps {
    query: string;
    onQueryChange: (value: string) => void;
    selectedTags?: string[];
    onSelectedTagsChange?: (tags: string[]) => void;
    availableTags?: string[];
    enrollment: string;
    onEnrollmentChange: (value: string) => void;
    sort?: string;
    onSortChange?: (value: string) => void;
    totalCount?: number;
    resultCount?: number;
    hasActiveFilters?: boolean;
    filtersOpen?: boolean;
    onToggleFilters?: () => void;
    onReset?: () => void;
}

export function RoadmapLibraryFilters({
    query,
    onQueryChange,
    selectedTags = [],
    onSelectedTagsChange,
    availableTags = [],
    enrollment,
    onEnrollmentChange,
    sort = "recent",
    onSortChange,
    totalCount = 0,
    resultCount = 0,
    hasActiveFilters = false,
    filtersOpen = false,
    onToggleFilters,
    onReset,
}: RoadmapLibraryFiltersProps) {
    return (
        <LibrarySearchToolbar
            searchLabel="Search roadmaps"
            placeholder="Title, tag, creator..."
            query={query}
            onQueryChange={onQueryChange}
            resultCount={resultCount}
            totalCount={totalCount}
            filtersOpen={filtersOpen}
            onToggleFilters={onToggleFilters}
            hasActiveFilters={hasActiveFilters}
            onReset={onReset}
            sort={sort}
            onSortChange={onSortChange}
            filterContent={
                <div className="library-filters-grid library-filters-grid--mui">
                    <Autocomplete
                        multiple
                        options={availableTags}
                        value={selectedTags}
                        onChange={(_event, nextTags) => onSelectedTagsChange?.(nextTags)}
                        size="small"
                        renderValue={(value, getItemProps) =>
                            value.map((tag, index) => {
                                const { key, ...itemProps } = getItemProps({ index });
                                return <Chip key={key} label={tag} size="small" {...itemProps} />;
                            })
                        }
                        renderInput={(params) => (
                            <TextField {...params} label="Tags" placeholder="Choose tags" />
                        )}
                    />
                    <TextField
                        select
                        label="My Roadmaps"
                        value={enrollment}
                        onChange={(event) => onEnrollmentChange(event.target.value)}
                        size="small"
                        fullWidth
                    >
                        {enrollmentOptions.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                </div>
            }
        />
    );
}
