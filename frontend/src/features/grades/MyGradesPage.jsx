import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/context/AuthContext'
import { CourseGradesList } from './CourseGradesList'
import { useStudentGradesQuery } from './hooks'

/**
 * The student's own results (/student/grades) - see
 * docs/tasks/TCM-24-frontend-grades.md step 4. Grouped by course, each with
 * its own weighted average.
 */
export function MyGradesPage() {
  const { user } = useAuth()
  const { data, isLoading } = useStudentGradesQuery(user.id)

  return (
    <Card>
      <CardHeader>
        <CardTitle>My Grades</CardTitle>
        <CardDescription>Every assessment recorded for you, course by course.</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : (
          <CourseGradesList
            grades={data?.grades ?? []}
            overall={data?.weightedAverage}
            emptyMessage="Nothing has been graded for you yet."
          />
        )}
      </CardContent>
    </Card>
  )
}
