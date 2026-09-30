import { ArrowLeft, ChevronDown, ChevronRight } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation('grades')
  const { courseId } = useParams()
  const navigate = useNavigate()
  const { data: gradebook, isLoading, isError, error } = useCourseGradebookQuery(courseId)
  const deleteGrade = useDeleteGradeMutation()

  const [expanded, setExpanded] = useState(() => new Set())
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-muted-foreground">
        {error.response?.status === 403
          ? t('gradebook.forbidden')
          : t('gradebook.loadFailed')}
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
        {t('common:actions.back')}
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>{t('gradebook.title')}</CardTitle>
          <CardDescription>
            {t('gradebook.course', { name: gradebook.courseName, code: gradebook.courseCode })}
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-2">
          {gradebook.students.length === 0 && (
            <p className="text-sm text-muted-foreground">{t('gradebook.noStudents')}</p>
          )}

          {gradebook.students.map((student) => {
            const isOpen = expanded.has(student.studentId)

            return (
              <section key={student.studentId} className="rounded-md border">
                <div className="flex flex-wrap items-center gap-3 px-4 py-3">
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    aria-label={
                      isOpen
                        ? t('gradebook.collapse', { name: student.studentName })
                        : t('gradebook.expand', { name: student.studentName })
                    }
                    onClick={() => toggle(student.studentId)}
                  >
                    {isOpen ? <ChevronDown /> : <ChevronRight />}
                  </Button>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{student.studentName}</p>
                    <p className="truncate text-xs text-muted-foreground">{student.email}</p>
                  </div>
                  <span className="text-xs text-muted-foreground">
                    {t('gradebook.assessmentCount', { count: student.grades.length })}
                  </span>
                  <Badge variant={scoreBadgeVariant(student.weightedAverage)}>
                    {formatPercent(student.weightedAverage)}
                  </Badge>
                  <Button size="sm" onClick={() => setEditing({ student, grade: null })}>
                    {t('form.addTitle')}
                  </Button>
                </div>

                {isOpen && (
                  <div className="border-t px-4 py-3">
                    <GradeEntriesTable
                      grades={student.grades}
                      onEdit={(grade) => setEditing({ student, grade })}
                      onDelete={(grade) => setDeleting({ student, grade })}
                      emptyMessage={t('gradebook.studentEmpty')}
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
            <AlertDialogTitle>{t('gradebook.delete.title')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('gradebook.delete.description', {
                title: deleting?.grade.title,
                name: deleting?.student.studentName,
              })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('gradebook.delete.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleteGrade.isPending}
              onClick={(event) => {
                event.preventDefault()
                deleteGrade.mutate(deleting.grade.id, { onSuccess: () => setDeleting(null) })
              }}
            >
              {t('gradebook.delete.confirm')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
