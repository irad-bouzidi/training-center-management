import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/context/AuthContext'
import { useMyCoursesQuery } from '@/features/courses/hooks'
import { CompleteEnrollmentButton } from '@/features/enrollments/CompleteEnrollmentButton'
import { CertificatesTable } from './CertificatesTable'
import { blockingReason } from './certificateDisplay'
import { useCertificatesQuery, useGenerateCertificateMutation } from './hooks'
import { apiErrorMessage } from '@/api/serverErrors'

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
 *
 * Who may issue is a third rule: an ADMIN, or the course's own trainer
 * (CertificateServiceImpl#requireTeaches). A trainer's button is disabled,
 * with the reason shown, on courses they don't teach - judged against their
 * own course list, since an enrollment only carries the course's id. An
 * ADMIN also gets "Mark completed" on APPROVED rows, the step that unblocks
 * the button.
 */
export function StudentCertificatesTab({ studentId, enrollments }) {
  const { t } = useTranslation('certificates')
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const isTrainer = user.role === 'TRAINER'
  const { data: taughtCourseIds } = useMyCoursesQuery(
    { size: 200 },
    { enabled: isTrainer, select: (data) => new Set(data.content.map((course) => course.id)) },
  )
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
          // Translated at render, so it follows a language switch.
          error,
        }) },
    )
  }

  return (
    <div className="space-y-6">
      <section className="space-y-2">
        <h2 className="text-sm font-semibold">{t('studentTab.issued')}</h2>
        <CertificatesTable
          certificates={certificates}
          isLoading={isLoading}
          emptyMessage={t('studentTab.issuedEmpty')}
        />
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-semibold">{t('studentTab.eligible')}</h2>
        {candidates.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('studentTab.allCertified')}</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('studentTab.course')}</TableHead>
                <TableHead>{t('studentTab.enrollment')}</TableHead>
                <TableHead className="w-80" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {candidates.map((enrollment) => {
                // Until a trainer's own course list arrives, nothing is known
                // to be theirs - the button waits, disabled, without claiming
                // a reason it can't back up yet.
                const waiting = isTrainer && !taughtCourseIds
                const notTheirs =
                  isTrainer && taughtCourseIds && !taughtCourseIds.has(enrollment.course.id)
                    ? t('studentTab.notTheirs')
                    : null
                const blocked = notTheirs ?? blockingReason(enrollment)

                return (
                  <TableRow key={enrollment.id}>
                    <TableCell>
                      {enrollment.course.name}{' '}
                      <span className="text-xs text-muted-foreground">{enrollment.course.code}</span>
                      {refusal?.courseId === enrollment.course.id ? (
                        <p className="text-xs text-destructive">{apiErrorMessage(refusal.error, t('studentTab.issueFailed'))}</p>
                      ) : (
                        blocked && <p className="text-xs text-muted-foreground">{blocked}</p>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={enrollment.status === 'COMPLETED' ? 'default' : 'outline'}>
                        {t(`common:enums.enrollmentStatus.${enrollment.status}`)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-2">
                        {isAdmin && <CompleteEnrollmentButton enrollment={enrollment} />}
                        <Button
                          size="sm"
                          disabled={Boolean(blocked) || waiting || generate.isPending}
                          title={blocked || undefined}
                          onClick={() => issue(enrollment)}
                        >
                          {t('studentTab.generate')}
                        </Button>
                      </div>
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
