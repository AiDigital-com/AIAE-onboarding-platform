// @ts-nocheck
import { mergeAttributes, Node } from "@tiptap/core"
import { NodeViewWrapper, ReactNodeViewRenderer } from "@tiptap/react"
import { DragHandleIcon } from "@/shared/editor/tiptap-icons/drag-handle-icon"
import { getYoutubeEmbedUrl } from "@/shared/lib/youtube"

function YoutubeNode({ node, selected, editor }) {
  const isEditable = editor?.isEditable
  const embedUrl = node.attrs.url ? getYoutubeEmbedUrl(node.attrs.url) : null

  return (
    <NodeViewWrapper className="youtube-node-shell">
      <span className={`youtube-node${selected ? " is-selected" : ""}`}>
        {isEditable && (
          // Same drag-handle rationale as the video node: a native draggable attribute
          // on the <iframe> would conflict with the embedded player's own input surface.
          <span
            className="youtube-node__drag-handle"
            data-drag-handle
            contentEditable={false}
            role="button"
            aria-label="Drag to reposition video"
            title="Drag to reposition">
            <DragHandleIcon />
          </span>
        )}
        {embedUrl ? (
          <iframe
            src={embedUrl}
            title={node.attrs.title || "YouTube video"}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowFullScreen
          />
        ) : (
          <div className="youtube-node__error" contentEditable={false}>
            Video unavailable.
          </div>
        )}
      </span>
    </NodeViewWrapper>
  )
}

export const YoutubeEmbed = Node.create({
  name: "youtubeEmbed",
  group: "block",
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      url: {
        default: null,
        parseHTML: (element) => element.getAttribute("data-youtube-url"),
        renderHTML: (attributes) => {
          if (!attributes.url) {
            return {}
          }

          return {
            "data-youtube-url": attributes.url,
          }
        },
      },
      title: {
        default: null,
      },
    }
  },

  parseHTML() {
    return [{ tag: "iframe[data-youtube-url]" }]
  },

  renderHTML({ HTMLAttributes }) {
    const embedUrl = HTMLAttributes["data-youtube-url"]
      ? getYoutubeEmbedUrl(HTMLAttributes["data-youtube-url"])
      : null

    return [
      "iframe",
      mergeAttributes(HTMLAttributes, {
        src: embedUrl,
        allow: "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share",
        allowfullscreen: "true",
      }),
    ]
  },

  addNodeView() {
    return ReactNodeViewRenderer(YoutubeNode)
  },
})
