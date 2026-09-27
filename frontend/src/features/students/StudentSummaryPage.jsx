import { ArrowLeft } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAuth } from '@/context/AuthContext'
import { StudentAttendanceTab } from '@/features/attendance/StudentAttendanceTab'
import { formatRate } from '@/features/attendance/attendanceDisplay'
import { StudentCertificatesTab } from '@/features/certificates/StudentCertificatesTab'
import { CompleteEnrollmentButton } from '@/features/enrollments/CompleteEnrollmentButton'
import { statusBadgeVariant as enrollmentStatusBadgeVariant } from '@/features/enrollments/enrollmentDisplay'
import { CourseGradesList } from '@/features/grades/CourseGradesList'
import { formatPercent } from '@/features/grades/gradeDisplay'
import { StudentPaymentsTab } from '@/features/payments/StudentPaymentsTab'
import { formatAmount } from '@/features/payments/paymentDisplay'
import { StudentScheduleTab } from '@/features/schedule/StudentScheduleTab'
import { useStudentSummaryQuery } from './hooks'
import { formatDate, fullName, statusBadgeVariant, titleCase } from './studentDisplay'

function Field({ label, value }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value}</p>
    </div>
  )
}

function Stat({ label, value }) {
  return (
    <div className="rounded-lg border p-4">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-2xl font-semibold">{value}</p>
    </div>
  )
}

/**
 * Per-student profile/summary, per
 * docs/tasks/TCM-15-frontend-student-directory.md. "Overview" and
 * "Enrollments" show real data (profile from TCM-13, enrollments from
 * TCM-14), as do "Attendance" (TCM-19/20), "Payments" (TCM-21/22), "Grades"
 * (TCM-23/24) and "Certificates" (TCM-25/26). Every tab is real data now;
 * the stub fields TCM-13 reserved on StudentSummaryResponse have all been
 * filled in, exactly as they were meant to be. "Schedule" (TCM-18 step 4)
 * is assembled from the enrolled courses' sessions - see StudentScheduleTab.
 *
 * An ADMIN can mark an APPROVED enrollment completed from the Enrollments
 * tab, which is the step that makes the Certificates tab's button live.
 */
export function StudentSummaryPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { data: summary, isLoading } = useStudentSummaryQuery(id)
  const isAdmin = user.role === 'ADMIN'

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading…</p>
  }

  if (!summary) {
    return <p className="text-sm text-muted-foreground">Student not found.</p>
  }

  const { profile, enrollments } = summary
  const activeEnrollments = enrollments.filter((enrollment) => enrollment.status === 'APPROVED').length

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <ArrowLeft />
        Back
      </Button>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {fullName(profile)}
            <Badge variant={statusBadgeVariant(profile.status)}>{titleCase(profile.status)}</Badge>
          </CardTitle>
          <CardDescription>{profile.email}</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="overview">
            <TabsList>
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="enrollments">Enrollments</TabsTrigger>
              <TabsTrigger value="schedule">Schedule</TabsTrigger>
              <TabsTrigger value="attendance">Attendance</TabsTrigger>
              <TabsTrigger value="grades">Grades</TabsTrigger>
              <TabsTrigger value="payments">Payments</TabsTrigger>
              <TabsTrigger value="certificates">Certificates</TabsTrigger>
            </TabsList>

            <TabsContent value="overview" className="space-y-6">
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                <Field label="Phone" value={profile.phone || '—'} />
                <Field label="Member since" value={formatDate(profile.createdAt)} />
              </div>

              <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                <Stat label="Total Enrollments" value={enrollments.length} />
                <Stat label="Active Enrollments" value={activeEnrollments} />
                <Stat label="Attendance Rate" value={formatRate(summary.attendanceRate)} />
                <Stat label="Overall Grade" value={formatPercent(summary.overallGrade)} />
                <Stat label="Payment Balance" value={formatAmount(summary.paymentBalance ?? 0)} />
              </div>
            </TabsContent>

            <TabsContent value="enrollments">
              {enrollments.length === 0 ? (
                <p className="text-sm text-muted-foreground">No enrollments yet.</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Course</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Enrolled</TableHead>
                      <TableHead>Decided</TableHead>
                      <TableHead>Decided By</TableHead>
                      {isAdmin && <TableHead className="w-36" />}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {enrollments.map((enrollment) => (
                      <TableRow key={enrollment.id}>
                        <TableCell>
                          {enrollment.course.name}{' '}
                          <span className="text-xs text-muted-foreground">{enrollment.course.code}</span>
                        </TableCell>
                        <TableCell>
                          <Badge variant={enrollmentStatusBadgeVariant(enrollment.status)}>
                            {titleCase(enrollment.status)}
                          </Badge>
                        </TableCell>
                        <TableCell>{formatDate(enrollment.enrolledAt)}</TableCell>
                        <TableCell>{enrollment.decidedAt ? formatDate(enrollment.decidedAt) : '—'}</TableCell>
                        <TableCell>{enrollment.decidedBy?.name ?? '—'}</TableCell>
                        {isAdmin && (
                          <TableCell className="text-right">
                            <CompleteEnrollmentButton enrollment={enrollment} />
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </TabsContent>

            <TabsContent value="schedule">
              <StudentScheduleTab enrollments={enrollments} />
            </TabsContent>
            <TabsContent value="attendance">
              <StudentAttendanceTab
                studentId={profile.id}
                enrollments={enrollments}
                overallRate={summary.attendanceRate}
              />
            </TabsContent>
            <TabsContent value="grades">
              <CourseGradesList
                grades={summary.grades}
                overall={summary.overallGrade}
                emptyMessage="Nothing has been graded for this student yet."
              />
            </TabsContent>
            <TabsContent value="payments">
              <StudentPaymentsTab studentId={profile.id} isAdmin={isAdmin} />
            </TabsContent>
            <TabsContent value="certificates">
              <StudentCertificatesTab studentId={profile.id} enrollments={enrollments} />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  )
}
