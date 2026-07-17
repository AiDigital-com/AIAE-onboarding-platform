import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import { Button } from "@/shared/ui/Button";

const actionMap = {
    materials: "Upload Material",
    lessons: "Create Lesson",
    roadmaps: "Create Roadmap",
} as const;

type LibraryTab = keyof typeof actionMap;

interface LibraryToolbarProps {
    activeTab: LibraryTab | string;
    onPrimaryAction: () => void;
    canCreateByTab?: Partial<Record<LibraryTab, boolean>>;
}

export function LibraryToolbar({
    activeTab,
    onPrimaryAction,
    canCreateByTab = {},
}: LibraryToolbarProps) {
    const canCreate = canCreateByTab[activeTab as LibraryTab] !== false;

    return (
        <header className="library-toolbar">
            <div>
                <p className="library-toolbar__eyebrow">Knowledge base</p>
                <h1 className="library-toolbar__title">Library</h1>
                <p className="library-toolbar__subtitle">
                    Manage materials, lessons, and roadmaps in one place.
                </p>
            </div>
            {canCreate && (
                <Button variant="primary" onClick={onPrimaryAction}>
                    <AddOutlinedIcon /> {actionMap[activeTab as LibraryTab]}
                </Button>
            )}
        </header>
    );
}
