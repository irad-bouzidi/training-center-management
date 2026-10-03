import { useState } from 'react'
import { Download, Eye } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { CertificatePreviewDialog } from './CertificatePreviewDialog'
import { formatIssuedAt } from './certificateDisplay'
import { CERTIFICATE_LANGUAGES, useDownloadCertificateMutation } from './hooks'

/**
 * Issued certificates with their preview and download actions - one
 * download per language, since every certificate is issued in English and
 * French. Shared by the student summary's Certificates tab, the student's
 * own page and the trainer's; `showStudent` adds the student column for
 * lists that span more than one.
 */
export function CertificatesTable({ certificates, isLoading, emptyMessage, showStudent = false }) {
  const { t } = useTranslation('certificates')
  const download = useDownloadCertificateMutation()
  const [previewed, setPreviewed] = useState(null)

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>
  }

  if (certificates.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyMessage}</p>
  }

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            {showStudent && <TableHead>{t('table.student')}</TableHead>}
            <TableHead>{t('table.course')}</TableHead>
            <TableHead>{t('table.number')}</TableHead>
            <TableHead>{t('table.issued')}</TableHead>
            <TableHead />
          </TableRow>
        </TableHeader>
        <TableBody>
          {certificates.map((certificate) => (
            <TableRow key={certificate.id}>
              {showStudent && (
                <TableCell>
                  {certificate.student.name}{' '}
                  <span className="text-xs text-muted-foreground">{certificate.student.email}</span>
                </TableCell>
              )}
              <TableCell>
                {certificate.course.name}{' '}
                <span className="text-xs text-muted-foreground">{certificate.course.code}</span>
              </TableCell>
              <TableCell className="font-mono text-xs">{certificate.certificateNumber}</TableCell>
              <TableCell>{formatIssuedAt(certificate.issuedAt)}</TableCell>
              <TableCell>
                <div className="flex justify-end gap-2">
                  <Button variant="outline" size="sm" onClick={() => setPreviewed(certificate)}>
                    <Eye />
                    {t('table.preview')}
                  </Button>
                  {CERTIFICATE_LANGUAGES.map((lang) => (
                    <Button
                      key={lang}
                      variant="outline"
                      size="sm"
                      disabled={download.isPending}
                      onClick={() =>
                        download.mutate({ id: certificate.id, certificateNumber: certificate.certificateNumber, lang })
                      }
                    >
                      <Download />
                      {t('table.download', { language: lang.toUpperCase() })}
                    </Button>
                  ))}
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <CertificatePreviewDialog
        // A fresh dialog per certificate, so it opens on the UI's language
        // rather than whichever one the last preview was left on.
        key={previewed?.id}
        certificate={previewed}
        open={Boolean(previewed)}
        onOpenChange={(open) => !open && setPreviewed(null)}
      />
    </>
  )
}
