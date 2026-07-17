// @ts-nocheck
import { cn } from "@/shared/editor/lib/tiptap-utils"
import "@/shared/editor/tiptap-ui-primitive/input/input.css"

function Input({
  className,
  type,
  ...props
}) {
  return (
    <input
      type={type}
      data-slot="tiptap-input"
      className={cn("tiptap-input", className)}
      {...props} />
  );
}

export { Input }
