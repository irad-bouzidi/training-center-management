import { useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/context/AuthContext'
import { ScheduleFormDialog } from './ScheduleFormDialog'
import { SessionRowActions } from './SessionRowActions'
import { formatSessionDate, formatTimeRange, statusBadgeVariant, titleCase } from './scheduleDisplay'
import { useSessionsQuery } from './hooks'

// A course's own timetable is short enough to show whole.
const COURSE_SCHEDULE_SIZE = 100

/**
 * The "Schedule" tab of CourseDetailPage - this course's sittings, in date
 * order. What each role sees is the backend's own scoping applied to a
 * courseId filter (ClassSessionController#search): an ADMIN and the course's
 * TRAINER see its sessions, a STUDENT sees them once approved on the course
 * and an empty tab before then.
 */
export function CourseScheduleTab({ course }) {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const { data, isLoading } = useSessionsQuery({
    courseId: course.id,
    size: COURSE_SCHEDULE_SIZE,
    sort: 'sessionDate,asc',
  })
  const sessions = data?.content ?? []

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }

  function openEdit(session) {
    setEditing(session)
    setFormOpen(true)
  }

  return (
    <div className="space-y-3">
      {isAdmin && (
        <div className="flex justify-end">
          <Button size="sm" onClick={openCreate}>
            New Session
          </Button>
        </div>
      )}

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Date</TableHead>
            <TableHead>Time</TableHead>
            <TableHead>Classroom</TableHead>
            <TableHead>Trainer</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="w-12" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading && (
            <TableRow>
              <TableCell colSpan={6} className="text-center text-muted-foreground">
                Loading…
              </TableCell>
            </TableRow>
          )}

          {!isLoading && sessions.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="text-center text-muted-foreground">
                No sessions scheduled for this course yet.
              </TableCell>
            </TableRow>
          )}

          {sessions.map((session) => (
            <TableRow key={session.id}>
              <TableCell>{formatSessionDate(session.sessionDate)}</TableCell>
              <TableCell className="font-mono">
                {formatTimeRange(session.startTime, session.endTime)}
              </TableCell>
              <TableCell>{session.classroom}</TableCell>
              <TableCell>{session.trainer.name}</TableCell>
              <TableCell>
                <Badge variant={statusBadgeVariant(session.status)}>{titleCase(session.status)}</Badge>
              </TableCell>
              <TableCell>
                <SessionRowActions session={session} onEdit={openEdit} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {isAdmin && (
        <ScheduleFormDialog
          open={formOpen}
          onOpenChange={setFormOpen}
          session={editing}
          defaultCourseId={course.id}
        />
      )}
    </div>
  )
}
