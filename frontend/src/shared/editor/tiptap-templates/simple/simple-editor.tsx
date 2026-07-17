// @ts-nocheck
import { useEffect, useRef, useState } from "react"
import { EditorContent, EditorContext, useEditor } from "@tiptap/react"

// --- Tiptap Core Extensions ---
import { StarterKit } from "@tiptap/starter-kit"
import { TaskItem, TaskList } from "@tiptap/extension-list"
import { TextAlign } from "@tiptap/extension-text-align"
import { Typography } from "@tiptap/extension-typography"
import { Highlight } from "@tiptap/extension-highlight"
import { Link as TiptapLink } from "@tiptap/extension-link"
import { Subscript } from "@tiptap/extension-subscript"
import { Superscript } from "@tiptap/extension-superscript"
import { Selection } from "@tiptap/extensions"

// --- UI Primitives ---
import { Button } from "@/shared/editor/tiptap-ui-primitive/button"
import { Spacer } from "@/shared/editor/tiptap-ui-primitive/spacer"
import {
  Toolbar,
  ToolbarGroup,
  ToolbarSeparator,
} from "@/shared/editor/tiptap-ui-primitive/toolbar"

// --- Tiptap Node ---
import { HorizontalRule } from "@/shared/editor/tiptap-node/horizontal-rule-node/horizontal-rule-node-extension"
import { ResizableImage as Image } from "@/shared/editor/tiptap-node/resizable-image-node/resizable-image-extension"
import { Video } from "@/shared/editor/tiptap-node/video-node/video-extension"
import { YoutubeEmbed } from "@/shared/editor/tiptap-node/youtube-node/youtube-extension"
import "@/shared/editor/tiptap-node/blockquote-node/blockquote-node.css"
import "@/shared/editor/tiptap-node/code-block-node/code-block-node.css"
import "@/shared/editor/tiptap-node/horizontal-rule-node/horizontal-rule-node.css"
import "@/shared/editor/tiptap-node/list-node/list-node.css"
import "@/shared/editor/tiptap-node/image-node/image-node.css"
import "@/shared/editor/tiptap-node/video-node/video-node.css"
import "@/shared/editor/tiptap-node/youtube-node/youtube-node.css"
import "@/shared/editor/tiptap-node/heading-node/heading-node.css"
import "@/shared/editor/tiptap-node/paragraph-node/paragraph-node.css"

// --- Tiptap UI ---
import { HeadingDropdownMenu } from "@/shared/editor/tiptap-ui/heading-dropdown-menu"
import { ListDropdownMenu } from "@/shared/editor/tiptap-ui/list-dropdown-menu"
import { BlockquoteButton } from "@/shared/editor/tiptap-ui/blockquote-button"
import { CodeBlockButton } from "@/shared/editor/tiptap-ui/code-block-button"
import {
  ColorHighlightPopover,
  ColorHighlightPopoverContent,
  ColorHighlightPopoverButton,
} from "@/shared/editor/tiptap-ui/color-highlight-popover"
import {
  LinkPopover,
  LinkContent,
  LinkButton,
} from "@/shared/editor/tiptap-ui/link-popover"
import { MarkButton } from "@/shared/editor/tiptap-ui/mark-button"
import { TextAlignButton } from "@/shared/editor/tiptap-ui/text-align-button"
import { UndoRedoButton } from "@/shared/editor/tiptap-ui/undo-redo-button"

// --- Icons ---
import { ArrowLeftIcon } from "@/shared/editor/tiptap-icons/arrow-left-icon"
import { HighlighterIcon } from "@/shared/editor/tiptap-icons/highlighter-icon"
import { ImagePlusIcon } from "@/shared/editor/tiptap-icons/image-plus-icon"
import { VideoPlusIcon } from "@/shared/editor/tiptap-icons/video-plus-icon"
import { LinkIcon } from "@/shared/editor/tiptap-icons/link-icon"

// --- Hooks ---
import { useIsBreakpoint } from "@/shared/editor/hooks/use-is-breakpoint"
import { useWindowSize } from "@/shared/editor/hooks/use-window-size"
import { useCursorVisibility } from "@/shared/editor/hooks/use-cursor-visibility"
import {
  LESSON_EDITOR_ASSET_DRAG_TYPE,
  isLessonEditorAssetDragPayload,
} from "@/shared/editor/lesson-editor-drag"
import { fetchFilePreviewUrl } from "@/shared/api/files"
import { isLikelyVideoFile } from "@/shared/lib/videoFileValidation"
import { extractYoutubeVideoId } from "@/shared/lib/youtube"

// --- Styles ---
import "@/shared/editor/styles/variables.css"
import "@/shared/editor/tiptap-templates/simple/simple-editor.css"

const StorageAwareLink = TiptapLink.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
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
})

function getImageFilesFromDataTransfer(dataTransfer) {
  if (!dataTransfer) {
    return []
  }

  const itemFiles = Array.from(dataTransfer.items || [])
    .filter((item) => item.kind === "file" && item.type?.startsWith("image/"))
    .map((item) => item.getAsFile())
    .filter(Boolean)

  if (itemFiles.length > 0) {
    return itemFiles
  }

  return Array.from(dataTransfer.files || [])
    .filter((file) => file.type?.startsWith("image/"))
}

function insertImageFiles({ editor, files, insertPosition, uploadImage }) {
  return Promise.all(
    files.map(async (file) => {
      const image = await uploadImage(file)

      if (!image?.src) {
        return null
      }

      return {
        type: "image",
        attrs: {
          src: image.src,
          alt: image.alt || file.name,
          title: image.title || file.name,
          storageKey: image.storageKey || null,
        },
      }
    })
  )
    .then((imageNodes) => {
      const nodes = imageNodes.filter(Boolean)

      if (!nodes.length || editor?.isDestroyed) {
        return
      }

      editor
        ?.chain()
        .focus()
        .insertContentAt(insertPosition, nodes)
        .run()
    })
    .catch((error) => {
      console.error("Failed to insert image into editor:", error)
    })
}

function getVideoFilesFromDataTransfer(dataTransfer) {
  if (!dataTransfer) {
    return []
  }

  const itemFiles = Array.from(dataTransfer.items || [])
    .filter((item) => item.kind === "file")
    .map((item) => item.getAsFile())
    .filter((file) => file && isLikelyVideoFile(file))

  if (itemFiles.length > 0) {
    return itemFiles
  }

  return Array.from(dataTransfer.files || [])
    .filter((file) => isLikelyVideoFile(file))
}

function insertVideoFiles({ editor, files, insertPosition, uploadVideo }) {
  return Promise.all(
    files.map(async (file) => {
      const video = await uploadVideo(file)

      if (!video?.src) {
        return null
      }

      return {
        type: "video",
        attrs: {
          src: video.src,
          title: video.title || file.name,
          storageKey: video.storageKey || null,
        },
      }
    })
  )
    .then((videoNodes) => {
      const nodes = videoNodes.filter(Boolean)

      if (!nodes.length || editor?.isDestroyed) {
        return
      }

      editor
        ?.chain()
        .focus()
        .insertContentAt(insertPosition, nodes)
        .run()
    })
    .catch((error) => {
      console.error("Failed to insert video into editor:", error)
    })
}

function getStandaloneYoutubeUrl(text) {
  const trimmedText = text?.trim()

  if (!trimmedText || /\s/.test(trimmedText)) {
    return null
  }

  return extractYoutubeVideoId(trimmedText) ? trimmedText : null
}

function insertYoutubeEmbed({ editor, url, insertPosition }) {
  if (!url || !editor || editor.isDestroyed) {
    return
  }

  editor
    .chain()
    .focus()
    .insertContentAt(insertPosition, [
      { type: "youtubeEmbed", attrs: { url, title: "YouTube video" } },
    ])
    .run()
}

function getLessonEditorAssetPayload(dataTransfer) {
  const rawPayload = dataTransfer?.getData(LESSON_EDITOR_ASSET_DRAG_TYPE)

  if (!rawPayload) {
    return null
  }

  try {
    const payload = JSON.parse(rawPayload)
    return isLessonEditorAssetDragPayload(payload) ? payload : null
  } catch {
    return null
  }
}

async function getPreviewUrlForPayload(payload) {
  if (payload.previewUrl) {
    return payload.previewUrl
  }

  if (payload.storageKey) {
    return fetchFilePreviewUrl(payload.storageKey)
  }

  return payload.url || ""
}

async function insertLessonEditorAsset({ editor, payload, insertPosition }) {
  if (!editor || editor.isDestroyed || !payload) {
    return
  }

  const kind = String(payload.kind || "").toLowerCase()
  const mimeType = String(payload.mimeType || "").toLowerCase()
  const title = payload.title || payload.name || "Asset"

  if (kind === "youtube" || (payload.url && extractYoutubeVideoId(payload.url))) {
    insertYoutubeEmbed({ editor, url: payload.url, insertPosition })
    return
  }

  if (kind === "link" && payload.url) {
    editor
      .chain()
      .focus()
      .insertContentAt(insertPosition, [{
        type: "paragraph",
        content: [{
          type: "text",
          text: title,
          marks: [{ type: "link", attrs: { href: payload.url } }],
        }],
      }])
      .run()
    return
  }

  const previewUrl = await getPreviewUrlForPayload(payload)

  if (!previewUrl) {
    return
  }

  if (kind === "image" || mimeType.startsWith("image/")) {
    editor
      .chain()
      .focus()
      .insertContentAt(insertPosition, [{
        type: "image",
        attrs: {
          src: previewUrl,
          alt: title,
          title,
          storageKey: payload.storageKey || null,
        },
      }])
      .run()
    return
  }

  if (kind === "video" || mimeType.startsWith("video/")) {
    editor
      .chain()
      .focus()
      .insertContentAt(insertPosition, [{
        type: "video",
        attrs: {
          src: previewUrl,
          title,
          storageKey: payload.storageKey || null,
        },
      }])
      .run()
    return
  }

  editor
    .chain()
    .focus()
    .insertContentAt(insertPosition, [{
      type: "paragraph",
      content: [{
        type: "text",
        text: title,
        marks: [{
          type: "link",
          attrs: {
            href: previewUrl,
            storageKey: payload.storageKey || null,
          },
        }],
      }],
    }])
    .run()
}

const MainToolbarContent = ({
  onHighlighterClick,
  onImageClick,
  canUploadImage,
  isUploadingImage,
  onVideoClick,
  canUploadVideo,
  isUploadingVideo,
  onLinkClick,
  isMobile,
}) => {
  return (
    <>
      <Spacer />
      <ToolbarGroup>
        <UndoRedoButton action="undo" />
        <UndoRedoButton action="redo" />
      </ToolbarGroup>
      <ToolbarSeparator />
      <ToolbarGroup>
        <HeadingDropdownMenu modal={false} levels={[1, 2, 3, 4]} />
        <ListDropdownMenu modal={false} types={["bulletList", "orderedList", "taskList"]} />
        <BlockquoteButton />
        <CodeBlockButton />
      </ToolbarGroup>
      <ToolbarSeparator />
      <ToolbarGroup>
        <MarkButton type="bold" />
        <MarkButton type="italic" />
        <MarkButton type="strike" />
        <MarkButton type="code" />
        <MarkButton type="underline" />
        {!isMobile ? (
          <ColorHighlightPopover />
        ) : (
          <ColorHighlightPopoverButton onClick={onHighlighterClick} />
        )}
        {!isMobile ? (
          <LinkPopover autoOpenOnLinkActive={false} />
        ) : (
          <LinkButton onClick={onLinkClick} />
        )}
        {canUploadImage && (
          <Button
            type="button"
            variant="ghost"
            tooltip="Insert image"
            disabled={isUploadingImage}
            aria-label="Insert image"
            onClick={onImageClick}
          >
            {isUploadingImage ? (
              <span className="simple-editor-image-upload-spinner" aria-hidden="true" />
            ) : (
              <ImagePlusIcon className="tiptap-button-icon" />
            )}
          </Button>
        )}
        {canUploadVideo && (
          <Button
            type="button"
            variant="ghost"
            tooltip="Insert video"
            disabled={isUploadingVideo}
            aria-label="Insert video"
            onClick={onVideoClick}
          >
            {isUploadingVideo ? (
              <span className="simple-editor-image-upload-spinner" aria-hidden="true" />
            ) : (
              <VideoPlusIcon className="tiptap-button-icon" />
            )}
          </Button>
        )}
      </ToolbarGroup>
      <ToolbarSeparator />
      <ToolbarGroup>
        <MarkButton type="superscript" />
        <MarkButton type="subscript" />
      </ToolbarGroup>
      <ToolbarSeparator />
      <ToolbarGroup>
        <TextAlignButton align="left" />
        <TextAlignButton align="center" />
        <TextAlignButton align="right" />
        <TextAlignButton align="justify" />
      </ToolbarGroup>
      <Spacer />
      {isMobile && <ToolbarSeparator />}
    </>
  );
}

const MobileToolbarContent = ({
  type,
  onBack
}) => (
  <>
    <ToolbarGroup>
      <Button variant="ghost" onClick={onBack}>
        <ArrowLeftIcon className="tiptap-button-icon" />
        {type === "highlighter" ? (
          <HighlighterIcon className="tiptap-button-icon" />
        ) : (
          <LinkIcon className="tiptap-button-icon" />
        )}
      </Button>
    </ToolbarGroup>

    <ToolbarSeparator />

    {type === "highlighter" ? (
      <ColorHighlightPopoverContent />
    ) : (
      <LinkContent />
    )}
  </>
)

export function SimpleEditor({
  content = "",
  editable = true,
  onChange,
  onImageUpload,
  onVideoUpload,
  className = "",
}) {
  const isMobile = useIsBreakpoint()
  const { height } = useWindowSize()
  const [mobileView, setMobileView] = useState("main")
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const [isUploadingVideo, setIsUploadingVideo] = useState(false)
  const toolbarRef = useRef(null)
  const imageFileInputRef = useRef(null)
  const videoFileInputRef = useRef(null)
  const latestContentRef = useRef(content)
  const editableRef = useRef(editable)
  const imageUploadRef = useRef(onImageUpload)
  const videoUploadRef = useRef(onVideoUpload)
  const insertPositionRef = useRef(null)

  useEffect(() => {
    imageUploadRef.current = onImageUpload
  }, [onImageUpload])

  useEffect(() => {
    videoUploadRef.current = onVideoUpload
  }, [onVideoUpload])

  const editor = useEditor({
    immediatelyRender: false,
    editable,
    editorProps: {
      attributes: {
        autocomplete: "off",
        autocorrect: "off",
        autocapitalize: "off",
        "aria-label": "Main content area, start typing to enter text.",
        class: "simple-editor",
      },
      handleClick: (_view, _pos, event) => {
        if (editableRef.current) {
          return false
        }

        const link = event.target?.closest?.("a[href]")

        if (!link) {
          return false
        }

        window.open(link.href, "_blank", "noopener,noreferrer")
        return true
      },
      handlePaste: (view, event) => {
        if (!editableRef.current) {
          return false
        }

        const insertPosition = view.state.selection.from

        if (imageUploadRef.current) {
          const imageFiles = getImageFilesFromDataTransfer(event.clipboardData)

          if (imageFiles.length > 0) {
            event.preventDefault()
            insertImageFiles({
              editor,
              files: imageFiles,
              insertPosition,
              uploadImage: imageUploadRef.current,
            })
            return true
          }
        }

        if (videoUploadRef.current) {
          const videoFiles = getVideoFilesFromDataTransfer(event.clipboardData)

          if (videoFiles.length > 0) {
            event.preventDefault()
            insertVideoFiles({
              editor,
              files: videoFiles,
              insertPosition,
              uploadVideo: videoUploadRef.current,
            })
            return true
          }
        }

        const youtubeUrl = getStandaloneYoutubeUrl(event.clipboardData?.getData("text/plain"))

        if (youtubeUrl) {
          event.preventDefault()
          insertYoutubeEmbed({ editor, url: youtubeUrl, insertPosition })
          return true
        }

        return false
      },
      handleDrop: (view, event) => {
        if (!editableRef.current) {
          return false
        }

        const coordinates = view.posAtCoords({
          left: event.clientX,
          top: event.clientY,
        })
        const insertPosition = coordinates?.pos ?? view.state.selection.from
        const lessonEditorAssetPayload = getLessonEditorAssetPayload(event.dataTransfer)

        if (lessonEditorAssetPayload) {
          event.preventDefault()
          insertLessonEditorAsset({
            editor,
            payload: lessonEditorAssetPayload,
            insertPosition,
          }).catch((error) => {
            console.error("Failed to insert lesson asset into editor:", error)
          })
          return true
        }

        if (imageUploadRef.current) {
          const imageFiles = getImageFilesFromDataTransfer(event.dataTransfer)

          if (imageFiles.length > 0) {
            event.preventDefault()
            insertImageFiles({
              editor,
              files: imageFiles,
              insertPosition,
              uploadImage: imageUploadRef.current,
            })
            return true
          }
        }

        if (videoUploadRef.current) {
          const videoFiles = getVideoFilesFromDataTransfer(event.dataTransfer)

          if (videoFiles.length > 0) {
            event.preventDefault()
            insertVideoFiles({
              editor,
              files: videoFiles,
              insertPosition,
              uploadVideo: videoUploadRef.current,
            })
            return true
          }
        }

        const youtubeUrl = getStandaloneYoutubeUrl(event.dataTransfer?.getData("text/plain"))

        if (youtubeUrl) {
          event.preventDefault()
          insertYoutubeEmbed({ editor, url: youtubeUrl, insertPosition })
          return true
        }

        return false
      },
    },
    extensions: [
      StarterKit.configure({
        horizontalRule: false,
        link: false,
      }),
      StorageAwareLink.configure({
        openOnClick: false,
        enableClickSelection: true,
      }),
      HorizontalRule,
      TextAlign.configure({ types: ["heading", "paragraph"] }),
      TaskList,
      TaskItem.configure({ nested: true }),
      Highlight.configure({ multicolor: true }),
      Image,
      Video,
      YoutubeEmbed,
      Typography,
      Superscript,
      Subscript,
      Selection,
    ],
    content,
    onUpdate: ({ editor }) => {
      const nextHtml = editor.getHTML()
      latestContentRef.current = nextHtml
      onChange?.(nextHtml, editor.getJSON())
    },
    onSelectionUpdate: ({ editor }) => {
      insertPositionRef.current = editor.state.selection.from
    },
  })

  const rect = useCursorVisibility({
    editor,
    overlayHeight: toolbarRef.current?.getBoundingClientRect().height ?? 0,
  })

  const handleImageButtonClick = () => {
    if (!editableRef.current || !imageUploadRef.current || isUploadingImage) {
      return
    }

    insertPositionRef.current = editor?.state.selection.from ?? insertPositionRef.current
    imageFileInputRef.current?.click()
  }

  const handleImageFileChange = (event) => {
    const files = Array.from(event.target.files || [])
      .filter((file) => file.type?.startsWith("image/"))

    if (!files.length || !editor || !imageUploadRef.current) {
      event.target.value = ""
      return
    }

    const insertPosition = insertPositionRef.current ?? editor.state.selection.from
    setIsUploadingImage(true)
    insertImageFiles({
      editor,
      files,
      insertPosition,
      uploadImage: imageUploadRef.current,
    })
      .catch((error) => {
        console.error("Failed to insert image into editor:", error)
      })
      .finally(() => {
        setIsUploadingImage(false)
        event.target.value = ""
      })
  }

  const handleVideoButtonClick = () => {
    if (!editableRef.current || !videoUploadRef.current || isUploadingVideo) {
      return
    }

    insertPositionRef.current = editor?.state.selection.from ?? insertPositionRef.current
    videoFileInputRef.current?.click()
  }

  const handleVideoFileChange = (event) => {
    const files = Array.from(event.target.files || [])
      .filter((file) => isLikelyVideoFile(file))

    if (!files.length || !editor || !videoUploadRef.current) {
      event.target.value = ""
      return
    }

    const insertPosition = insertPositionRef.current ?? editor.state.selection.from
    setIsUploadingVideo(true)
    insertVideoFiles({
      editor,
      files,
      insertPosition,
      uploadVideo: videoUploadRef.current,
    })
      .catch((error) => {
        console.error("Failed to insert video into editor:", error)
      })
      .finally(() => {
        setIsUploadingVideo(false)
        event.target.value = ""
      })
  }

  useEffect(() => {
    if (!isMobile && mobileView !== "main") {
      setMobileView("main")
    }
  }, [isMobile, mobileView])

  useEffect(() => {
    if (!editor) {
      return
    }

    editableRef.current = editable
    editor.setEditable(editable)
  }, [editor, editable])

  useEffect(() => {
    if (!editor || content === latestContentRef.current) {
      return
    }

    latestContentRef.current = content
    editor.commands.setContent(content || "", false)
  }, [editor, content])

  return (
    <div className={`simple-editor-wrapper ${className}`}>
      <EditorContext.Provider value={{ editor }}>
        {editable && (
          <Toolbar
            ref={toolbarRef}
            style={{
              ...(isMobile
                ? {
                    bottom: `calc(100% - ${height - rect.y}px)`,
                  }
                : {}),
            }}>
            {mobileView === "main" ? (
              <MainToolbarContent
                onHighlighterClick={() => setMobileView("highlighter")}
                onImageClick={handleImageButtonClick}
                canUploadImage={Boolean(onImageUpload)}
                isUploadingImage={isUploadingImage}
                onVideoClick={handleVideoButtonClick}
                canUploadVideo={Boolean(onVideoUpload)}
                isUploadingVideo={isUploadingVideo}
                onLinkClick={() => setMobileView("link")}
                isMobile={isMobile} />
            ) : (
              <MobileToolbarContent
                type={mobileView === "highlighter" ? "highlighter" : "link"}
                onBack={() => setMobileView("main")} />
            )}
          </Toolbar>
        )}

        <input
          ref={imageFileInputRef}
          className="simple-editor-image-input"
          type="file"
          accept="image/*"
          aria-hidden="true"
          tabIndex={-1}
          onChange={handleImageFileChange}
        />

        <input
          ref={videoFileInputRef}
          className="simple-editor-image-input"
          type="file"
          accept="video/*"
          aria-hidden="true"
          tabIndex={-1}
          onChange={handleVideoFileChange}
        />

        <div className="simple-editor-scroll">
          <EditorContent editor={editor} role="presentation" className="simple-editor-content" />
          <div className="simple-editor-bottom-spacer" aria-hidden="true" />
        </div>
      </EditorContext.Provider>
    </div>
  );
}
