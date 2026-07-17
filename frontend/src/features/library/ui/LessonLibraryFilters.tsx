import { Autocomplete, Chip, MenuItem, TextField } from "@mui/material";
import { LibrarySearchToolbar } from "./LibrarySearchToolbar";

const activityOptions = [
    { value: "all", label: "Any activity" },
    { value: "quiz", label: "Has quiz" },
    { value: "flashcards", label: "Has flashcards" },
    { value: "no-activities", label: "No activities" },
];

const enrollmentOptions = [
    { value: "all", label: "Any enrollment" },
    { value: "enrolled", label: "In My Lessons" },
    { value: "not-enrolled", label: "Not in My Lessons" },
];

const lessonStatusOptions = [
    { value: "ready", label: "Ready" },
    { value: "archived", label: "Archived" },
    { value: "draft", label: "Draft" },
    { value: "all", label: "All" },
];

interface LessonLibraryFiltersProps {
    query: string;
    onQueryChange: (value: string) => void;
    status: string;
    onStatusChange: (value: string) => void;
    selectedTags?: string[];
    onSelectedTagsChange?: (tags: string[]) => void;
    availableTags?: string[];
    activity: string;
    onActivityChange: (value: string) => void;
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

export function LessonLibraryFilters(props: LessonLibraryFiltersProps) {
    const {
        query,
        onQueryChange,
        status,
        onStatusChange,
        selectedTags = [],
        onSelectedTagsChange,
        availableTags = [],
        activity,
        onActivityChange,
        enrollment,
        onEnrollmentChange,
        totalCount = 0,
        resultCount = 0,
        hasActiveFilters = false,
        filtersOpen = false,
        onToggleFilters,
        onReset,
        sort,
        onSortChange,
    } = props;

    return (
        <LibrarySearchToolbar
            searchLabel="Search lessons"
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
                        label="Status"
                        value={status}
                        onChange={(event) => onStatusChange(event.target.value)}
                        size="small"
                        fullWidth
                    >
                        {lessonStatusOptions.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Activity"
                        value={activity}
                        onChange={(event) => onActivityChange(event.target.value)}
                        size="small"
                        fullWidth
                    >
                        {activityOptions.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="My Lessons"
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
