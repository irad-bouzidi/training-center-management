import { formatDateTimeAsDate } from '@/lib/format'

export const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE']

export function fullName(profile) {
  return `${profile.firstName} ${profile.lastName}`
}

export function statusBadgeVariant(status) {
  return status === 'ACTIVE' ? 'secondary' : 'destructive'
}

export function formatDate(isoString) {
  return formatDateTimeAsDate(isoString)
}
