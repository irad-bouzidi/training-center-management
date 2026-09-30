import { Download } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatIssuedAt } from './certificateDisplay'
import { useDownloadCertificateMutation } from './hooks'

/**
 * Issued certificates with their download action. Shared by the student
 * summary's Certificates tab and the student's own page.
 */
export function CertificatesTable({ certificates, isLoading, emptyMessage }) {
  const { t } = useTranslation('certificates')
  const download = useDownloadCertificateMutation()

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>
  }

  if (certificates.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyMessage}</p>
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t('table.course')}</TableHead>
          <TableHead>{t('table.number')}</TableHead>
          <TableHead>{t('table.issued')}</TableHead>
          <TableHead className="w-36" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {certificates.map((certificate) => (
          <TableRow key={certificate.id}>
            <TableCell>
              {certificate.course.name}{' '}
              <span className="text-xs text-muted-foreground">{certificate.course.code}</span>
            </TableCell>
            <TableCell className="font-mono text-xs">{certificate.certificateNumber}</TableCell>
            <TableCell>{formatIssuedAt(certificate.issuedAt)}</TableCell>
            <TableCell>
              <Button
                variant="outline"
                size="sm"
                disabled={download.isPending}
                onClick={() =>
                  download.mutate({ id: certificate.id, certificateNumber: certificate.certificateNumber })
                }
              >
                <Download />
                {t('table.download')}
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
