import { Archive, Send } from 'lucide-react'

export const STATUS_OPTIONS = ['DRAFT', 'PUBLISHED', 'ARCHIVED']

/** DRAFT -> "Draft", PUBLISHED -> "Published". */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function statusBadgeVariant(status) {
  switch (status) {
    case 'PUBLISHED':
      return 'secondary'
    case 'ARCHIVED':
      return 'destructive'
    default:
      return 'outline'
  }
}

/**
 * The status change available from each current status - see
 * docs/tasks/TCM-12-frontend-course-management.md step 6. ARCHIVED isn't a
 * dead end: it can be republished. Shared by CourseRowActions (list/detail
 * row menu) and CourseDetailPage (admin header actions).
 */
export const STATUS_TRANSITIONS = {
  DRAFT: { label: 'Publish', next: 'PUBLISHED', icon: Send },
  PUBLISHED: { label: 'Archive', next: 'ARCHIVED', icon: Archive },
  ARCHIVED: { label: 'Republish', next: 'PUBLISHED', icon: Send },
}

export function formatPrice(price) {
  return new Intl.NumberFormat(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price)
}

export function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
