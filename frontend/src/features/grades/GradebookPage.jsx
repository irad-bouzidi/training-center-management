import { ArrowLeft, ChevronDown, ChevronRight } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { GradeEntriesTable } from './GradeEntriesTable'
import { GradeFormDialog } from './GradeFormDialog'
import { formatPercent, scoreBadgeVariant } from './gradeDisplay'
import { useCourseGradebookQuery, useDeleteGradeMutation } from './hooks'

/**
 * The course gradebook (/trainer/courses/:courseId/grades, and the same under
 * /admin) - see docs/tasks/TCM-24-frontend-grades.md step 2. Students are
 * listed with their weighted average; expanding one shows its assessments,
 * which is where they're added, edited and removed.
 *
 * Who may write is the backend's call (the course's trainer or an admin, and
 * only the original grader may amend); a rejection shows inline in the
 * dialog rather than being predicted here.
 */
export function GradebookPage() {
  const { courseId } = useParams()
  const navigate = useNavigate()
  const { data: gradebook, isLoading, isError, error } = useCourseGradebookQuery(courseId)
  const deleteGrade = useDeleteGradeMutation()

  const [expanded, setExpanded] = useState(() => new Set())
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading…</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-muted-foreground">
        {error.response?.status === 403
          ? 'The gradebook is open to administrators and the course’s own trainer.'
          : 'This gradebook could not be loaded.'}
      </p>
    )
  }

  function toggle(studentId) {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(studentId)) {
        next.delete(studentId)
      } else {
        next.add(studentId)
      }
      return next
    })
  }

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <ArrowLeft />
        Back
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>Gradebook</CardTitle>
          <CardDescription>
            {gradebook.courseName} ({gradebook.courseCode})
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-2">
          {gradebook.students.length === 0 && (
            <p className="text-sm text-muted-foreground">Nobody is approved on this course yet.</p>
          )}

          {gradebook.students.map((student) => {
            const isOpen = expanded.has(student.studentId)

            return (
              <section key={student.studentId} className="rounded-md border">
                <div className="flex flex-wrap items-center gap-3 px-4 py-3">
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    aria-label={`${isOpen ? 'Collapse' : 'Expand'} ${student.studentName}`}
                    onClick={() => toggle(student.studentId)}
                  >
                    {isOpen ? <ChevronDown /> : <ChevronRight />}
                  </Button>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{student.studentName}</p>
                    <p className="truncate text-xs text-muted-foreground">{student.email}</p>
                  </div>
                  <span className="text-xs text-muted-foreground">
                    {student.grades.length} assessment{student.grades.length === 1 ? '' : 's'}
                  </span>
                  <Badge variant={scoreBadgeVariant(student.weightedAverage)}>
                    {formatPercent(student.weightedAverage)}
                  </Badge>
                  <Button size="sm" onClick={() => setEditing({ student, grade: null })}>
                    Add assessment
                  </Button>
                </div>

                {isOpen && (
                  <div className="border-t px-4 py-3">
                    <GradeEntriesTable
                      grades={student.grades}
                      onEdit={(grade) => setEditing({ student, grade })}
                      onDelete={(grade) => setDeleting({ student, grade })}
                      emptyMessage="Nothing recorded for this student yet."
                    />
                  </div>
                )}
              </section>
            )
          })}
        </CardContent>
      </Card>

      {editing && (
        <GradeFormDialog
          onOpenChange={() => setEditing(null)}
          courseId={courseId}
          student={editing.student}
          grade={editing.grade}
        />
      )}

      <AlertDialog open={Boolean(deleting)} onOpenChange={(open) => !open && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove this assessment?</AlertDialogTitle>
            <AlertDialogDescription>
              {deleting?.grade.title} for {deleting?.student.studentName} will be deleted, and their average
              recomputed without it. This can’t be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Keep it</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleteGrade.isPending}
              onClick={(event) => {
                event.preventDefault()
                deleteGrade.mutate(deleting.grade.id, { onSuccess: () => setDeleting(null) })
              }}
            >
              Remove
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
