import { Archive, Send } from 'lucide-react'
import { formatDateTimeAsDate, formatDecimal } from '@/lib/format'

export const STATUS_OPTIONS = ['DRAFT', 'PUBLISHED', 'ARCHIVED']

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
 * row menu) and CourseDetailPage (admin header actions). `labelKey` /
 * `confirmTitleKey` are `courses:` i18n keys.
 */
export const STATUS_TRANSITIONS = {
  DRAFT: {
    labelKey: 'transitions.publish',
    confirmTitleKey: 'transitions.publishConfirm',
    next: 'PUBLISHED',
    icon: Send,
  },
  PUBLISHED: {
    labelKey: 'transitions.archive',
    confirmTitleKey: 'transitions.archiveConfirm',
    next: 'ARCHIVED',
    icon: Archive,
  },
  ARCHIVED: {
    labelKey: 'transitions.republish',
    confirmTitleKey: 'transitions.republishConfirm',
    next: 'PUBLISHED',
    icon: Send,
  },
}

// Both follow the UI language (see @/lib/format), not the browser's.
export function formatPrice(price) {
  return formatDecimal(price)
}

export function formatDate(isoString) {
  return formatDateTimeAsDate(isoString)
}
