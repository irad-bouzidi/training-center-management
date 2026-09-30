import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useCoursesQuery } from '@/features/courses/hooks'
import { useStudentsQuery } from '@/features/students/hooks'
import { PaymentsTable } from './PaymentsTable'
import { RecordPaymentDialog } from './RecordPaymentDialog'
import { STATUS_OPTIONS, formatAmount, totalOutstanding } from './paymentDisplay'
import { usePaymentsQuery } from './hooks'

const PAGE_SIZE = 20
const PICKER_SIZE = 200
const ALL = 'ALL'

/**
 * The admin's invoice ledger (/admin/payments) - see
 * docs/tasks/TCM-22-frontend-payment.md step 2. Fetching the list also runs
 * the server's overdue sweep (PaymentServiceImpl#search), so an invoice that
 * fell due overnight reads as OVERDUE here without anyone asking for it.
 */
export function PaymentsListPage() {
  const { t } = useTranslation('payments')
  const [status, setStatus] = useState(ALL)
  const [courseId, setCourseId] = useState(ALL)
  const [studentId, setStudentId] = useState(ALL)
  const [page, setPage] = useState(0)
  const [recording, setRecording] = useState(null)

  const { data: coursesPage } = useCoursesQuery({ size: PICKER_SIZE })
  const { data: studentsPage } = useStudentsQuery({ size: PICKER_SIZE })

  const { data, isLoading } = usePaymentsQuery({
    page,
    size: PAGE_SIZE,
    sort: 'dueDate,asc',
    status: status === ALL ? undefined : status,
    courseId: courseId === ALL ? undefined : courseId,
    studentId: studentId === ALL ? undefined : studentId,
  })

  const payments = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  // Any filter change can shrink the result set, so never leave the viewer
  // stranded on a page that no longer exists.
  function updateFilter(setter, value) {
    setter(value)
    setPage(0)
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>{t('list.title')}</CardTitle>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-end gap-3">
            <div className="space-y-2">
              <Label htmlFor="statusFilter">{t('list.filters.status')}</Label>
              <Select value={status} onValueChange={(value) => updateFilter(setStatus, value)}>
                <SelectTrigger id="statusFilter" className="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>{t('list.filters.allStatuses')}</SelectItem>
                  {STATUS_OPTIONS.map((option) => (
                    <SelectItem key={option} value={option}>
                      {t(`common:enums.paymentStatus.${option}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="courseFilter">{t('list.filters.course')}</Label>
              <Select value={courseId} onValueChange={(value) => updateFilter(setCourseId, value)}>
                <SelectTrigger id="courseFilter" className="w-56">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>{t('list.filters.allCourses')}</SelectItem>
                  {coursesPage?.content?.map((course) => (
                    <SelectItem key={course.id} value={course.id}>
                      {t('list.filters.courseOption', { name: course.name, code: course.code })}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="studentFilter">{t('list.filters.student')}</Label>
              <Select value={studentId} onValueChange={(value) => updateFilter(setStudentId, value)}>
                <SelectTrigger id="studentFilter" className="w-56">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>{t('list.filters.allStudents')}</SelectItem>
                  {studentsPage?.content?.map(({ profile }) => (
                    <SelectItem key={profile.id} value={profile.id}>
                      {profile.firstName} {profile.lastName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <PaymentsTable
            payments={payments}
            isLoading={isLoading}
            onRecordPayment={setRecording}
            emptyMessage={t('list.empty')}
          />

          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <p>
              {t('list.summary', { count: totalElements, amount: formatAmount(totalOutstanding(payments)) })}
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
              >
                {t('common:actions.previous')}
              </Button>
              <span>{t('common:pagination.pageOf', { page: totalPages === 0 ? 0 : page + 1, total: totalPages })}</span>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((current) => current + 1)}
              >
                {t('common:actions.next')}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {recording && <RecordPaymentDialog onOpenChange={() => setRecording(null)} payment={recording} />}
    </>
  )
}
