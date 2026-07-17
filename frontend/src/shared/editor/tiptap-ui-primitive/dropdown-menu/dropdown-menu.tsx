// @ts-nocheck
import { useCallback, useEffect, useId } from "react"
import * as DropdownMenuPrimitive from "@radix-ui/react-dropdown-menu"
import { cn } from "@/shared/editor/lib/tiptap-utils"
import { CheckIcon } from "@/shared/editor/tiptap-icons/check-icon"

import "@/shared/editor/tiptap-ui-primitive/dropdown-menu/dropdown-menu.css"

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

function DropdownMenu({
  open,
  onOpenChange,
  ...props
}) {
  const overlayId = useId()
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

  return (<DropdownMenuPrimitive.Root data-slot="tiptap-dropdown-menu" open={open} onOpenChange={handleOpenChange} {...props} />);
}

function DropdownMenuPortal({
  ...props
}) {
  return (<DropdownMenuPrimitive.Portal data-slot="tiptap-dropdown-menu-portal" {...props} />);
}

function DropdownMenuTrigger({
  ...props
}) {
  return (<DropdownMenuPrimitive.Trigger data-slot="tiptap-dropdown-menu-trigger" {...props} />);
}

function DropdownMenuContent({
  className,
  align = "start",
  sideOffset = 4,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.Portal>
      <DropdownMenuPrimitive.Content
        data-slot="tiptap-dropdown-menu-content"
        sideOffset={sideOffset}
        align={align}
        className={cn("tiptap-dropdown-menu-content", className)}
        onCloseAutoFocus={(e) => e.preventDefault()}
        {...props} />
    </DropdownMenuPrimitive.Portal>
  );
}

function DropdownMenuGroup({
  className,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.Group
      data-slot="tiptap-dropdown-menu-group"
      className={cn("tiptap-dropdown-menu-group", className)}
      {...props} />
  );
}

function DropdownMenuItem({
  className,
  inset,
  variant = "default",
  ...props
}) {
  return (
    <DropdownMenuPrimitive.Item
      data-slot="tiptap-dropdown-menu-item"
      data-inset={inset}
      data-variant={variant}
      className={cn("tiptap-dropdown-menu-item", className)}
      {...props} />
  );
}

function DropdownMenuCheckboxItem({
  className,
  children,
  checked,
  inset,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.CheckboxItem
      data-slot="tiptap-dropdown-menu-checkbox-item"
      data-inset={inset}
      className={cn("tiptap-dropdown-menu-checkbox-item", className)}
      checked={checked}
      {...props}>
      <span
        className="tiptap-dropdown-menu-item-indicator"
        data-slot="tiptap-dropdown-menu-checkbox-item-indicator">
        <DropdownMenuPrimitive.ItemIndicator>
          <CheckIcon />
        </DropdownMenuPrimitive.ItemIndicator>
      </span>
      {children}
    </DropdownMenuPrimitive.CheckboxItem>
  );
}

function DropdownMenuRadioGroup({
  ...props
}) {
  return (<DropdownMenuPrimitive.RadioGroup data-slot="tiptap-dropdown-menu-radio-group" {...props} />);
}

function DropdownMenuRadioItem({
  className,
  children,
  inset,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.RadioItem
      data-slot="tiptap-dropdown-menu-radio-item"
      data-inset={inset}
      className={cn("tiptap-dropdown-menu-radio-item", className)}
      {...props}>
      <span
        className="tiptap-dropdown-menu-item-indicator"
        data-slot="tiptap-dropdown-menu-radio-item-indicator">
        <DropdownMenuPrimitive.ItemIndicator>
          <CheckIcon />
        </DropdownMenuPrimitive.ItemIndicator>
      </span>
      {children}
    </DropdownMenuPrimitive.RadioItem>
  );
}

function DropdownMenuLabel({
  className,
  inset,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.Label
      data-slot="tiptap-dropdown-menu-label"
      data-inset={inset}
      className={cn("tiptap-dropdown-menu-label", className)}
      {...props} />
  );
}

function DropdownMenuSeparator({
  className,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.Separator
      data-slot="tiptap-dropdown-menu-separator"
      className={cn("tiptap-dropdown-menu-separator", className)}
      {...props} />
  );
}

function DropdownMenuShortcut({
  className,
  ...props
}) {
  return (
    <span
      data-slot="tiptap-dropdown-menu-shortcut"
      className={cn("tiptap-dropdown-menu-shortcut", className)}
      {...props} />
  );
}

function DropdownMenuSub({
  ...props
}) {
  return (<DropdownMenuPrimitive.Sub data-slot="tiptap-dropdown-menu-sub" {...props} />);
}

function DropdownMenuSubTrigger({
  className,
  inset,
  children,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.SubTrigger
      data-slot="tiptap-dropdown-menu-sub-trigger"
      data-inset={inset}
      className={cn("tiptap-dropdown-menu-sub-trigger", className)}
      {...props}>
      {children}
    </DropdownMenuPrimitive.SubTrigger>
  );
}

function DropdownMenuSubContent({
  className,
  ...props
}) {
  return (
    <DropdownMenuPrimitive.SubContent
      data-slot="tiptap-dropdown-menu-sub-content"
      className={cn("tiptap-dropdown-menu-sub-content", className)}
      {...props} />
  );
}

export {
  DropdownMenu,
  DropdownMenuPortal,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuItem,
  DropdownMenuCheckboxItem,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuSub,
  DropdownMenuSubTrigger,
  DropdownMenuSubContent,
}
