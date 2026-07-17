// @ts-nocheck
import { forwardRef, useCallback, useEffect, useId, useState } from "react"

// --- Hooks ---
import { useIsBreakpoint } from "@/shared/editor/hooks/use-is-breakpoint"
import { useTiptapEditor } from "@/shared/editor/hooks/use-tiptap-editor"

// --- Icons ---
import { CornerDownLeftIcon } from "@/shared/editor/tiptap-icons/corner-down-left-icon"
import { ExternalLinkIcon } from "@/shared/editor/tiptap-icons/external-link-icon"
import { LinkIcon } from "@/shared/editor/tiptap-icons/link-icon"
import { TrashIcon } from "@/shared/editor/tiptap-icons/trash-icon"

import { useLinkPopover } from "@/shared/editor/tiptap-ui/link-popover"

import { Button } from "@/shared/editor/tiptap-ui-primitive/button"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/shared/editor/tiptap-ui-primitive/popover"
import { Separator } from "@/shared/editor/tiptap-ui-primitive/separator"
import {
  Card,
  CardBody,
  CardItemGroup,
} from "@/shared/editor/tiptap-ui-primitive/card"
import { Input } from "@/shared/editor/tiptap-ui-primitive/input"
import { ButtonGroup } from "@/shared/editor/tiptap-ui-primitive/button-group"

import "./link-popover.css"

const EDITOR_OVERLAY_OPEN_EVENT = "tiptap-editor-overlay-open"

/**
 * Link button component for triggering the link popover
 */
export const LinkButton = forwardRef(({ className, children, ...props }, ref) => {
  return (
    <Button
      type="button"
      className={className}
      variant="ghost"
      role="button"
      tabIndex={-1}
      aria-label="Link"
      tooltip="Link"
      ref={ref}
      {...props}>
      {children || <LinkIcon className="tiptap-button-icon" />}
    </Button>
  );
})

LinkButton.displayName = "LinkButton"

/**
 * Main content component for the link popover
 */
const LinkMain = ({
  url,
  setUrl,
  setLink,
  removeLink,
  openLink,
  isActive,
}) => {
  const isMobile = useIsBreakpoint()

  const handleKeyDown = (event) => {
    if (event.key === "Enter") {
      event.preventDefault()
      setLink()
    }
  }

  return (
    <Card
      style={{
        ...(isMobile ? { boxShadow: "none", border: 0 } : {}),
      }}>
      <CardBody
        style={{
          ...(isMobile ? { padding: 0 } : {}),
        }}>
        <CardItemGroup orientation="horizontal">
          <Input
            type="url"
            placeholder="Paste a link..."
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            onKeyDown={handleKeyDown}
            autoFocus
            autoComplete="off"
            autoCorrect="off"
            autoCapitalize="off"
            className="tiptap-link-input" />

          <ButtonGroup>
            <Button
              type="button"
              onClick={setLink}
              title="Apply link"
              disabled={!url && !isActive}
              variant="ghost">
              <CornerDownLeftIcon className="tiptap-button-icon" />
            </Button>
          </ButtonGroup>

          <Separator />

          <ButtonGroup>
            <ButtonGroup>
              <Button
                type="button"
                onClick={openLink}
                title="Open in new window"
                disabled={!url && !isActive}
                variant="ghost">
                <ExternalLinkIcon className="tiptap-button-icon" />
              </Button>
            </ButtonGroup>

            <ButtonGroup>
              <Button
                type="button"
                onClick={removeLink}
                title="Remove link"
                disabled={!url && !isActive}
                variant="ghost">
                <TrashIcon className="tiptap-button-icon" />
              </Button>
            </ButtonGroup>
          </ButtonGroup>
        </CardItemGroup>
      </CardBody>
    </Card>
  );
}

/**
 * Link content component for standalone use
 */
export const LinkContent = ({ editor }) => {
  const linkPopover = useLinkPopover({
    editor,
  })

  return <LinkMain {...linkPopover} />;
}

/**
 * Link popover component for Tiptap editors.
 *
 * For custom popover implementations, use the `useLinkPopover` hook instead.
 */
export const LinkPopover = forwardRef((
  {
    editor: providedEditor,
    hideWhenUnavailable = false,
    onSetLink,
    onOpenChange,
    autoOpenOnLinkActive = true,
    onClick,
    children,
    forceClosed = false,
    ...buttonProps
  },
  ref
) => {
  const { editor } = useTiptapEditor(providedEditor)
  const [isOpen, setIsOpen] = useState(false)
  const overlayId = useId()

  const {
    isVisible,
    canSet,
    isActive,
    url,
    setUrl,
    setLink,
    removeLink,
    openLink,
    label,
    Icon,
  } = useLinkPopover({
    editor,
    hideWhenUnavailable,
    onSetLink,
  })

  const handleOnOpenChange = useCallback((nextIsOpen) => {
    if (nextIsOpen) {
      document.dispatchEvent(new CustomEvent(EDITOR_OVERLAY_OPEN_EVENT, {
        detail: { overlayId },
      }))
    }

    setIsOpen(nextIsOpen)
    onOpenChange?.(nextIsOpen)
  }, [onOpenChange, overlayId])

  const handleSetLink = useCallback(() => {
    setLink()
    handleOnOpenChange(false)
  }, [setLink, handleOnOpenChange])

  const handleClick = useCallback((event) => {
    onClick?.(event)
    if (event.defaultPrevented) return
    handleOnOpenChange(!isOpen)
  }, [onClick, isOpen, handleOnOpenChange])

  useEffect(() => {
    if (autoOpenOnLinkActive && isActive) {
      handleOnOpenChange(true)
    }
  }, [autoOpenOnLinkActive, isActive, handleOnOpenChange])

  // Radix popovers don't know about sibling editor overlays unless one explicitly says
  // "I just opened, close yourself."
  useEffect(() => {
    if (forceClosed) {
      handleOnOpenChange(false)
    }
  }, [forceClosed, handleOnOpenChange])

  if (!isVisible) {
    return null
  }

  return (
    <Popover open={isOpen} onOpenChange={handleOnOpenChange} overlayId={overlayId}>
      <PopoverTrigger asChild>
        <LinkButton
          disabled={!canSet}
          data-active-state={isActive ? "on" : "off"}
          data-disabled={!canSet}
          aria-label={label}
          aria-pressed={isActive}
          onClick={handleClick}
          {...buttonProps}
          ref={ref}>
          {children ?? <Icon className="tiptap-button-icon" />}
        </LinkButton>
      </PopoverTrigger>
      <PopoverContent>
        <LinkMain
          url={url}
          setUrl={setUrl}
          setLink={handleSetLink}
          removeLink={removeLink}
          openLink={openLink}
          isActive={isActive} />
      </PopoverContent>
    </Popover>
  );
})

LinkPopover.displayName = "LinkPopover"

export default LinkPopover
