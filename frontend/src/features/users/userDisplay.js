import { formatDateTimeAsDate } from '@/lib/format'

export const ROLE_OPTIONS = ['ADMIN', 'TRAINER', 'STUDENT']
export const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE']

export function fullName(user) {
  return `${user.firstName} ${user.lastName}`
}

export function formatDate(isoString) {
  return formatDateTimeAsDate(isoString)
}
