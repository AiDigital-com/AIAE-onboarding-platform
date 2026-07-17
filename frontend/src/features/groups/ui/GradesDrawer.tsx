import { Drawer } from "@/shared/ui/Drawer";
import { GradeManagementPanel } from "./GradeManagementPanel";

interface GradesDrawerProps {
    open: boolean;
    onClose: () => void;
    onToast: (message: string, severity?: "success" | "error" | "warning") => void;
}

/** Slide-out panel for the grade dictionary, kept out of the main Teams workspace. */
export function GradesDrawer({ open, onClose, onToast }: GradesDrawerProps) {
    return (
        <Drawer
            open={open}
            onClose={onClose}
            title="Grades"
            description="Manage the grade dictionary used across team member profiles and grade-filtered roadmap assignments."
        >
            <GradeManagementPanel onToast={onToast} />
        </Drawer>
    );
}
