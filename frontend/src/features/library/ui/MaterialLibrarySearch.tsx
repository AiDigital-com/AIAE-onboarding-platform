import { Autocomplete, Chip, TextField } from "@mui/material";
import { LibrarySearchToolbar } from "./LibrarySearchToolbar";

interface MaterialLibrarySearchProps {
    query: string;
    onQueryChange: (value: string) => void;
    sort?: string;
    onSortChange?: (value: string) => void;
    selectedTags?: string[];
    onSelectedTagsChange?: (tags: string[]) => void;
    availableTags?: string[];
    totalCount?: number;
    resultCount?: number;
    hasActiveFilters?: boolean;
    filtersOpen?: boolean;
    onToggleFilters?: () => void;
    onReset?: () => void;
}

export function MaterialLibrarySearch({
    query,
    onQueryChange,
    sort = "recent",
    onSortChange,
    selectedTags = [],
    onSelectedTagsChange,
    availableTags = [],
    totalCount = 0,
    resultCount = 0,
    hasActiveFilters = false,
    filtersOpen = false,
    onToggleFilters,
    onReset,
}: MaterialLibrarySearchProps) {
    return (
        <LibrarySearchToolbar
            searchLabel="Search materials"
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
                </div>
            }
        />
    );
}
