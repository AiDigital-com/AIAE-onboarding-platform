import { useState } from "react";
import { Autocomplete, TextField } from "@mui/material";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { useGroupCandidateUsersQuery } from "../api/useGroupsQuery";
import type { UserSummaryV1 } from "../api/types";

interface UserSearchComboboxProps {
    groupId: number;
    forLeads: boolean;
    label: string;
    placeholder: string;
    disabled?: boolean;
    onSelect: (user: UserSummaryV1 | null) => void;
}

/**
 * Type-to-search user picker for adding a group member/lead. Queries the server on each keystroke
 * (debounced) instead of holding a static, unpaged candidate list.
 */
export function UserSearchCombobox({
    groupId,
    forLeads,
    label,
    placeholder,
    disabled = false,
    onSelect,
}: UserSearchComboboxProps) {
    const [inputValue, setInputValue] = useState("");
    const [selected, setSelected] = useState<UserSummaryV1 | null>(null);
    const debouncedInput = useDebounce(inputValue, 300);

    const candidatesQuery = useGroupCandidateUsersQuery(groupId, forLeads, debouncedInput.trim() || undefined, {
        enabled: debouncedInput.trim().length > 0,
    });
    const options = candidatesQuery.data ?? [];

    return (
        <Autocomplete
            size="small"
            value={selected}
            options={options}
            loading={candidatesQuery.isFetching}
            disabled={disabled}
            filterOptions={(items) => items}
            getOptionLabel={(user) => `${user.name || user.email} (${user.email})`}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            onInputChange={(_event, value) => setInputValue(value)}
            onChange={(_event, user) => {
                setSelected(user);
                onSelect(user);
            }}
            noOptionsText={debouncedInput.trim() ? "No matching users." : "Type a name or email…"}
            renderInput={(params) => <TextField {...params} label={label} placeholder={placeholder} />}
        />
    );
}
