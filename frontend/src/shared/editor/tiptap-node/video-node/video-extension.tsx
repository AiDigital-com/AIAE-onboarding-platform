// @ts-nocheck
import { useState } from "react"
import { mergeAttributes, Node } from "@tiptap/core"
import { NodeViewWrapper, ReactNodeViewRenderer } from "@tiptap/react"
import { DragHandleIcon } from "@/shared/editor/tiptap-icons/drag-handle-icon"
import { getVideoErrorMessage } from "@/shared/lib/videoError"

function VideoNode({ node, selected, editor }) {
  const isEditable = editor?.isEditable
  const [errorMessage, setErrorMessage] = useState(null)

  const handleError = (event) => {
    const mediaError = event.currentTarget.error
    console.error("Lesson video failed to load/play:", {
      src: node.attrs.src,
      code: mediaError?.code,
      message: mediaError?.message,
    })
    setErrorMessage(getVideoErrorMessage(mediaError))
  }

  return (
    <NodeViewWrapper className="video-node-shell">
      <span className={`video-node${selected ? " is-selected" : ""}`}>
        {isEditable && (
          // A native draggable attribute on the <video> itself conflicts with the
          // element's own control surface (scrubbing/play hit-testing swallows the
          // drag gesture), so dragging is exposed through this handle instead.
          // data-drag-handle is Tiptap/ProseMirror's own contract for recognizing a
          // custom drag-initiation element on a NodeView (see stopEvent handling in
          // @tiptap/core) — without it, drags only work by accident.
          <span
            className="video-node__drag-handle"
            data-drag-handle
            contentEditable={false}
            role="button"
            aria-label="Drag to reposition video"
            title="Drag to reposition">
            <DragHandleIcon />
          </span>
        )}
        {node.attrs.src && !errorMessage ? (
          <video
            src={node.attrs.src}
            title={node.attrs.title || ""}
            controls
            preload="metadata"
            onError={handleError}
          />
        ) : (
          <div className="video-node__error" contentEditable={false}>
            {errorMessage || "Video unavailable."}
          </div>
        )}
      </span>
    </NodeViewWrapper>
  )
}

export const Video = Node.create({
  name: "video",
  group: "block",
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      src: {
        default: null,
      },
      title: {
        default: null,
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

  parseHTML() {
    return [{ tag: "video" }]
  },

  renderHTML({ HTMLAttributes }) {
    return ["video", mergeAttributes(HTMLAttributes, { controls: "true", preload: "metadata" })]
  },

  addNodeView() {
    return ReactNodeViewRenderer(VideoNode)
  },
})
