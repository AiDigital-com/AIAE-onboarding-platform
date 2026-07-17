// @ts-nocheck
import { useCallback, useEffect, useId } from "react"
import * as PopoverPrimitive from "@radix-ui/react-popover"
import { cn } from "@/shared/editor/lib/tiptap-utils"
import "@/shared/editor/tiptap-ui-primitive/popover/popover.css"

const EDITOR_OVERLAY_OPEN_EVENT = "tiptap-editor-overlay-open"

function shouldKeepEditorOverlayOpen(target) {
  if (!(target instanceof Element)) {
    return true
  }

  if (target.closest(".tiptap-popover, .tiptap-dropdown-menu-content")) {
    return true
  }

  return Boolean(target.closest(
    '[data-slot="tiptap-popover-trigger"][data-state="open"], [data-slot="tiptap-dropdown-menu-trigger"][data-state="open"]'
  ))
}

function Popover({
  open,
  onOpenChange,
  overlayId: providedOverlayId,
  ...props
}) {
  const generatedOverlayId = useId()
  const overlayId = providedOverlayId ?? generatedOverlayId
  const handleOpenChange = useCallback((nextIsOpen) => {
    if (nextIsOpen) {
      document.dispatchEvent(new CustomEvent(EDITOR_OVERLAY_OPEN_EVENT, {
        detail: { overlayId },
      }))
    }

    onOpenChange?.(nextIsOpen)
  }, [onOpenChange, overlayId])

  useEffect(() => {
    if (!open || !onOpenChange) {
      return
    }

    const handleOverlayOpen = (event) => {
      if (event.detail?.overlayId !== overlayId) {
        onOpenChange(false)
      }
    }

    document.addEventListener(EDITOR_OVERLAY_OPEN_EVENT, handleOverlayOpen)
    return () => document.removeEventListener(EDITOR_OVERLAY_OPEN_EVENT, handleOverlayOpen)
  }, [open, onOpenChange, overlayId])

  useEffect(() => {
    if (!open || !onOpenChange) {
      return
    }

    const handlePointerDown = (event) => {
      if (shouldKeepEditorOverlayOpen(event.target)) {
        return
      }
      onOpenChange(false)
    }

    document.addEventListener("pointerdown", handlePointerDown, true)
    return () => document.removeEventListener("pointerdown", handlePointerDown, true)
  }, [open, onOpenChange])

  return <PopoverPrimitive.Root open={open} onOpenChange={handleOpenChange} {...props} />;
}

function PopoverTrigger({
  ...props
}) {
  return <PopoverPrimitive.Trigger data-slot="tiptap-popover-trigger" {...props} />;
}

function PopoverContent({
  className,
  align = "center",
  sideOffset = 4,
  ...props
}) {
  return (
    <PopoverPrimitive.Portal>
      <PopoverPrimitive.Content
        align={align}
        sideOffset={sideOffset}
        className={cn("tiptap-popover", className)}
        {...props} />
    </PopoverPrimitive.Portal>
  );
}

export { Popover, PopoverTrigger, PopoverContent }
