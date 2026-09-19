import { useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { CertificatesTable } from './CertificatesTable'
import { blockingReason } from './certificateDisplay'
import { errorMessage, useCertificatesQuery, useGenerateCertificateMutation } from './hooks'

/**
 * The "Certificates" tab of StudentSummaryPage - see
 * docs/tasks/TCM-26-frontend-certificates.md step 2. Two halves: what has
 * been issued, and what could be.
 *
 * Eligibility has a client-visible half (the enrollment must be COMPLETED)
 * and a server-only half (attendance at or above the configured threshold).
 * The first disables the button with the reason on it; the second can only
 * be found out by asking, so a refusal is shown inline on the row that was
 * tried, in the server's own words.
 */
export function StudentCertificatesTab({ studentId, enrollments }) {
  const { data: certificates = [], isLoading } = useCertificatesQuery(studentId)
  const generate = useGenerateCertificateMutation()
  const [refusal, setRefusal] = useState(null)

  const certifiedCourseIds = new Set(certificates.map((certificate) => certificate.course.id))
  const candidates = enrollments.filter((enrollment) => !certifiedCourseIds.has(enrollment.course.id))

  function issue(enrollment) {
    setRefusal(null)
    generate.mutate(
      { studentId, courseId: enrollment.course.id },
      { onError: (error) => setRefusal({
          courseId: enrollment.course.id,
          message: errorMessage(error, 'This certificate could not be issued'),
        }) },
    )
  }

  return (
    <div className="space-y-6">
      <section className="space-y-2">
        <h2 className="text-sm font-semibold">Issued</h2>
        <CertificatesTable
          certificates={certificates}
          isLoading={isLoading}
          emptyMessage="No certificates have been issued to this student yet."
        />
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-semibold">Eligible for certification</h2>
        {candidates.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            Every course this student is enrolled in has been certified.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Course</TableHead>
                <TableHead>Enrollment</TableHead>
                <TableHead className="w-48" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {candidates.map((enrollment) => {
                const blocked = blockingReason(enrollment)

                return (
                  <TableRow key={enrollment.id}>
                    <TableCell>
                      {enrollment.course.name}{' '}
                      <span className="text-xs text-muted-foreground">{enrollment.course.code}</span>
                      {refusal?.courseId === enrollment.course.id && (
                        <p className="text-xs text-destructive">{refusal.message}</p>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={enrollment.status === 'COMPLETED' ? 'default' : 'outline'}>
                        {enrollment.status.charAt(0) + enrollment.status.slice(1).toLowerCase()}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Button
                        size="sm"
                        disabled={Boolean(blocked) || generate.isPending}
                        title={blocked ?? undefined}
                        onClick={() => issue(enrollment)}
                      >
                        Generate certificate
                      </Button>
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        )}
      </section>
    </div>
  )
}
