import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/context/AuthContext'
import { EnrollButton } from './EnrollButton'
import { EnrollmentRowActions } from './EnrollmentRowActions'
import { formatDate, statusBadgeVariant, titleCase } from './enrollmentDisplay'
import { useEnrollmentsQuery, useMyEnrollmentsByCourseQuery } from './hooks'

// A course roster fits one page - the capacity this app models is a
// classroom, not a cohort of hundreds, so there's no paging here.
const ROSTER_SIZE = 100

/**
 * The "Enrollments" tab of CourseDetailPage. Who sees what follows the
 * backend's own split (EnrollmentController#search): an ADMIN sees the roster
 * and can decide pending requests from it, a TRAINER sees the roster of their
 * own courses read-only, and a STUDENT sees where their own registration
 * stands plus the enroll action.
 *
 * The catalog is shared, so a Trainer can open a course that isn't theirs -
 * the roster query is skipped there rather than left to 403, which the table
 * would otherwise render as an empty course.
 */
export function CourseEnrollmentsTab({ course }) {
  const { user } = useAuth()
  const isStudent = user.role === 'STUDENT'
  const isAdmin = user.role === 'ADMIN'
  const canSeeRoster = isAdmin || course.primaryTrainer?.id === user.id

  const { data, isLoading } = useEnrollmentsQuery(
    { courseId: course.id, size: ROSTER_SIZE, sort: 'enrolledAt,desc' },
    { enabled: !isStudent && canSeeRoster },
  )
  const { data: myEnrollments } = useMyEnrollmentsByCourseQuery(isStudent)

  if (isStudent) {
    const enrollment = myEnrollments?.get(course.id)

    return (
      <div className="flex items-center gap-3">
        {enrollment ? (
          <>
            <Badge variant={statusBadgeVariant(enrollment.status)}>{titleCase(enrollment.status)}</Badge>
            <span className="text-sm text-muted-foreground">
              Requested on {formatDate(enrollment.enrolledAt)}
            </span>
          </>
        ) : (
          <>
            <span className="text-sm text-muted-foreground">You aren't enrolled in this course.</span>
            <EnrollButton course={course} enrollment={enrollment} />
          </>
        )}
      </div>
    )
  }

  if (!canSeeRoster) {
    return (
      <p className="text-sm text-muted-foreground">
        Only this course's trainer and administrators can see who's enrolled.
      </p>
    )
  }

  const enrollments = data?.content ?? []

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Student</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Requested</TableHead>
          {isAdmin && <TableHead className="w-44" />}
        </TableRow>
      </TableHeader>
      <TableBody>
        {isLoading && (
          <TableRow>
            <TableCell colSpan={isAdmin ? 4 : 3} className="text-center text-muted-foreground">
              Loading…
            </TableCell>
          </TableRow>
        )}

        {!isLoading && enrollments.length === 0 && (
          <TableRow>
            <TableCell colSpan={isAdmin ? 4 : 3} className="text-center text-muted-foreground">
              No one has enrolled in this course yet.
            </TableCell>
          </TableRow>
        )}

        {enrollments.map((enrollment) => (
          <TableRow key={enrollment.id}>
            <TableCell>
              {enrollment.student.name}{' '}
              <span className="text-xs text-muted-foreground">{enrollment.student.email}</span>
            </TableCell>
            <TableCell>
              <Badge variant={statusBadgeVariant(enrollment.status)}>{titleCase(enrollment.status)}</Badge>
            </TableCell>
            <TableCell>{formatDate(enrollment.enrolledAt)}</TableCell>
            {isAdmin && (
              <TableCell>
                <EnrollmentRowActions enrollment={enrollment} />
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
