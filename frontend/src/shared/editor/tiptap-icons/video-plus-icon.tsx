// @ts-nocheck
import { memo, type SVGProps } from "react"

export const VideoPlusIcon = memo(({ className, ...props }: SVGProps<SVGSVGElement>) => {
  return (
    <svg
      width="24"
      height="24"
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      {...props}>
      <rect x="2" y="6" width="14" height="12" rx="2" stroke="currentColor" strokeWidth="2" />
      <path d="M16 10.5L21 8V16L16 13.5V10.5Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
      <path d="M10 9.5V14.5L14 12L10 9.5Z" fill="currentColor" />
    </svg>
  );
})

VideoPlusIcon.displayName = "VideoPlusIcon"
