// @ts-nocheck
import { useCallback, useRef } from "react"
import TiptapImage from "@tiptap/extension-image"
import { NodeViewWrapper, ReactNodeViewRenderer } from "@tiptap/react"

function getNumericWidth(value) {
  const parsed = Number.parseInt(String(value || "").replace("px", ""), 10)

  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

function ResizableImageNode({ node, selected, updateAttributes, editor }) {
  const wrapperRef = useRef(null)
  const startStateRef = useRef(null)
  const width = getNumericWidth(node.attrs.width)
  const isEditable = editor?.isEditable

  const handlePointerMove = useCallback((event) => {
    const startState = startStateRef.current

    if (!startState) {
      return
    }

    const parentWidth = wrapperRef.current?.parentElement?.clientWidth || 900
    const direction = startState.direction || "right"
    const delta = event.clientX - startState.x
    const signedDelta = direction.includes("left") ? -delta : delta
    const nextWidth = Math.round(
      Math.min(parentWidth, Math.max(120, startState.width + signedDelta))
    )

    updateAttributes({ width: nextWidth })
  }, [updateAttributes])

  const stopResize = useCallback(() => {
    startStateRef.current = null
    window.removeEventListener("pointermove", handlePointerMove)
    window.removeEventListener("pointerup", stopResize)
    document.body.style.cursor = ""
    document.body.style.userSelect = ""
  }, [handlePointerMove])

  const startResize = useCallback((event, direction) => {
    event.preventDefault()
    event.stopPropagation()

    const currentWidth = wrapperRef.current?.getBoundingClientRect().width || width || 320
    startStateRef.current = {
      x: event.clientX,
      width: currentWidth,
      direction,
    }
    document.body.style.cursor = "nwse-resize"
    document.body.style.userSelect = "none"
    window.addEventListener("pointermove", handlePointerMove)
    window.addEventListener("pointerup", stopResize)
  }, [handlePointerMove, stopResize, width])

  return (
    <NodeViewWrapper className="resizable-image-node-shell">
      <span
        ref={wrapperRef}
        className={`resizable-image-node${selected ? " is-selected" : ""}`}
        style={{
          width: width ? `${width}px` : "auto",
        }}
      >
        <img
          src={node.attrs.src}
          alt={node.attrs.alt || ""}
          title={node.attrs.title || ""}
          draggable={isEditable}
        />
        {isEditable && selected && (
          <>
            {["top-left", "top", "top-right", "right", "bottom-right", "bottom", "bottom-left", "left"].map((direction) => (
              <button
                key={direction}
                type="button"
                aria-label={`Resize image ${direction}`}
                className={`resizable-image-node__handle resizable-image-node__handle--${direction}`}
                onPointerDown={(event) => startResize(event, direction)}
              />
            ))}
          </>
        )}
      </span>
    </NodeViewWrapper>
  )
}

export const ResizableImage = TiptapImage.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      width: {
        default: null,
        parseHTML: (element) => (
          getNumericWidth(element.getAttribute("width")) ||
          getNumericWidth(element.style.width)
        ),
        renderHTML: (attributes) => {
          const width = getNumericWidth(attributes.width)

          if (!width) {
            return {}
          }

          return {
            width,
            style: `width: ${width}px; height: auto;`,
          }
        },
      },
      storageKey: {
        default: null,
        parseHTML: (element) => element.getAttribute("data-storage-key"),
        renderHTML: (attributes) => {
          if (!attributes.storageKey) {
            return {}
          }

          return {
            "data-storage-key": attributes.storageKey,
          }
        },
      },
    }
  },

  addNodeView() {
    return ReactNodeViewRenderer(ResizableImageNode)
  },
})
