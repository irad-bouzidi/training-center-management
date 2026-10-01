import { formatNumber } from '@/lib/format'

/**
 * Assessment kinds, mirroring com.tcm.grade.model.AssessmentType (TCM-23).
 */
export const ASSESSMENT_TYPES = ['EXAM', 'ASSIGNMENT', 'QUIZ', 'PROJECT']

/** A percentage, or "—" while nothing is graded - an ungraded student is not
 * a 0% student, which is why the backend sends null rather than 0. Formatted
 * in the UI language ("85.5%" / "85,5 %"). */
export function formatPercent(value) {
  return value === null || value === undefined
    ? '—'
    : formatNumber(Number(value) / 100, { style: 'percent', maximumFractionDigits: 1 })
}

/** Pass marks read as neutral, a strong result as primary, a fail as destructive. */
export function scoreBadgeVariant(percentage) {
  if (percentage === null || percentage === undefined) {
    return 'outline'
  }
  if (percentage >= 75) {
    return 'default'
  }
  return percentage >= 50 ? 'secondary' : 'destructive'
}

/**
 * Σ(score/maxScore × weight) / Σweight as a percentage - the same figure
 * GradeServiceImpl#weightedAverage computes, recomputed here only to break a
 * flat list of grades down by course (the API's averages are for the whole
 * list it returns, or for one course asked about by id).
 */
export function weightedAverage(grades) {
  if (grades.length === 0) {
    return null
  }
  let weighted = 0
  let totalWeight = 0
  for (const grade of grades) {
    weighted += (Number(grade.score) / Number(grade.maxScore)) * Number(grade.weight)
    totalWeight += Number(grade.weight)
  }
  return Math.round((weighted / totalWeight) * 1000) / 10
}

/** A flat grade list bucketed by course, courses in name order. */
export function groupByCourse(grades) {
  const byCourse = new Map()

  for (const grade of grades) {
    const bucket = byCourse.get(grade.course.id)
    if (bucket) {
      bucket.grades.push(grade)
    } else {
      byCourse.set(grade.course.id, { course: grade.course, grades: [grade] })
    }
  }

  return [...byCourse.values()].sort((a, b) => a.course.name.localeCompare(b.course.name))
}
