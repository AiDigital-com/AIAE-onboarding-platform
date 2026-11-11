// Reference-counted page scroll lock for overlays. Dialogs can be stacked (a confirmation over
// an editor) and can close in any order, so each one saving and restoring `body.style.overflow`
// on its own leaves the page locked. The first lock saves the value; the last unlock restores it.

const state = { count: 0, previousOverflow: "" };

/** Locks page scroll and returns the matching unlock; calling the unlock twice is harmless. */
export function lockBodyScroll(): () => void {
    if (state.count === 0) {
        state.previousOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";
    }
    state.count += 1;

    let released = false;
    return () => {
        if (released) {
            return;
        }
        released = true;
        state.count -= 1;
        if (state.count === 0) {
            document.body.style.overflow = state.previousOverflow;
        }
    };
}
