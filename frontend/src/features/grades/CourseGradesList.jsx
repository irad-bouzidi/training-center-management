import { Badge } from '@/components/ui/badge'
import { GradeEntriesTable } from './GradeEntriesTable'
import { formatPercent, groupByCourse, scoreBadgeVariant, weightedAverage } from './gradeDisplay'

/**
 * One student's grades, grouped by course with a weighted average per course.
 * Shared by the student summary's Grades tab and the student's own page.
 *
 * The API returns a flat list with one average over the whole of it, so the
 * per-course figures are recomputed here with the same formula (see
 * gradeDisplay#weightedAverage) rather than fetched course by course.
 */
export function CourseGradesList({ grades, overall, emptyMessage }) {
  const courses = groupByCourse(grades)

  if (courses.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyMessage}</p>
  }

  return (
    <div className="space-y-6">
      <p className="text-sm text-muted-foreground">
        Overall: <span className="font-medium text-foreground">{formatPercent(overall)}</span> — each assessment
        weighted by its share of the final mark.
      </p>

      {courses.map(({ course, grades: courseGrades }) => {
        const average = weightedAverage(courseGrades)

        return (
          <section key={course.id} className="space-y-2">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-semibold">
                {course.name} <span className="text-xs font-normal text-muted-foreground">{course.code}</span>
              </h2>
              <Badge variant={scoreBadgeVariant(average)}>{formatPercent(average)}</Badge>
            </div>
            <GradeEntriesTable grades={courseGrades} emptyMessage="Nothing recorded yet." />
          </section>
        )
      })}
    </div>
  )
}
