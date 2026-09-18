/**
 * Class session lifecycle, mirroring com.tcm.schedule.model.SessionStatus
 * (TCM-17): SCHEDULED on creation, an ADMIN may CANCELLED it, and an ADMIN or
 * the assigned trainer marks it COMPLETED. There is no way back to SCHEDULED.
 */
export const STATUS_OPTIONS = ['SCHEDULED', 'CANCELLED', 'COMPLETED']

/** SCHEDULED -> "Scheduled", CANCELLED -> "Cancelled". */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function statusBadgeVariant(status) {
  switch (status) {
    case 'COMPLETED':
      return 'default'
    case 'CANCELLED':
      return 'destructive'
    default:
      return 'secondary'
  }
}

/** The backend sends LocalTime as "09:00:00"; an `<input type="time">` wants
 * "09:00", and so does a reader. */
export function toTimeInputValue(time) {
  return time ? time.slice(0, 5) : ''
}

export function formatTimeRange(startTime, endTime) {
  return `${toTimeInputValue(startTime)} – ${toTimeInputValue(endTime)}`
}

/** "Mon, 2 Mar 2026" - the heading each agenda day is grouped under. */
export function formatSessionDate(isoDate) {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString(undefined, {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

/** Today as yyyy-MM-dd in the viewer's own timezone - `toISOString()` would
 * shift the date for anyone west of UTC. */
export function todayIsoDate() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

/**
 * The agenda view: sessions bucketed into days, days in date order and each
 * day's sessions in start-time order. The backend returns a flat page, so the
 * grouping is the frontend's - see
 * docs/tasks/TCM-18-frontend-scheduling.md step 3.
 *
 * @returns {{date: string, sessions: object[]}[]}
 */
export function groupByDate(sessions) {
  const byDate = new Map()

  for (const session of sessions) {
    const bucket = byDate.get(session.sessionDate)
    if (bucket) {
      bucket.push(session)
    } else {
      byDate.set(session.sessionDate, [session])
    }
  }

  return [...byDate.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, daySessions]) => ({
      date,
      sessions: [...daySessions].sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }))
}
