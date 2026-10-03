import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { CertificatesTable } from './CertificatesTable'
import { useTaughtCertificatesQuery } from './hooks'

/**
 * Every certificate issued on the courses the trainer teaches
 * (/trainer/certificates), to review and download in either language
 * without going through each student's summary.
 */
export function TrainerCertificatesPage() {
  const { t } = useTranslation('certificates')
  const { data: certificates = [], isLoading } = useTaughtCertificatesQuery()

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('taught.title')}</CardTitle>
        <CardDescription>{t('taught.description')}</CardDescription>
      </CardHeader>
      <CardContent>
        <CertificatesTable
          certificates={certificates}
          isLoading={isLoading}
          emptyMessage={t('taught.empty')}
          showStudent
        />
      </CardContent>
    </Card>
  )
}
