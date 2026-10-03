import { useState } from 'react'
import { Download, ExternalLink } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { currentLocale } from '@/lib/format'
import { formatIssuedAt } from './certificateDisplay'
import { CERTIFICATE_LANGUAGES, saveBlob, useCertificatePdf } from './hooks'

/**
 * Shows an issued certificate's PDF in the app, in either of the languages
 * it is issued in, with a download of whichever one is on screen. Opens on
 * the UI's own language.
 *
 * The PDF is fetched through the authorized client and shown from an object
 * URL - the browser's own viewer does the rendering, so what is reviewed
 * here is exactly the file a download gives.
 */
export function CertificatePreviewDialog({ certificate, open, onOpenChange }) {
  const { t } = useTranslation('certificates')
  const [lang, setLang] = useState(() => (currentLocale().startsWith('fr') ? 'fr' : 'en'))
  const pdf = useCertificatePdf(certificate?.id, lang, { enabled: open })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl">
        {certificate && (
          <>
            <DialogHeader>
              <DialogTitle>{t('preview.title', { number: certificate.certificateNumber })}</DialogTitle>
              <DialogDescription>
                {t('preview.description', {
                  course: certificate.course.name,
                  student: certificate.student.name,
                  date: formatIssuedAt(certificate.issuedAt),
                })}
              </DialogDescription>
            </DialogHeader>

            <ToggleGroup
              type="single"
              value={lang}
              // "" when the pressed item is clicked again - keep the current one.
              onValueChange={(value) => value && setLang(value)}
              aria-label={t('preview.language')}
            >
              {CERTIFICATE_LANGUAGES.map((code) => (
                <ToggleGroupItem key={code} value={code}>
                  {t(`languages.${code}`)}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>

            <div className="flex aspect-[842/595] max-h-[65vh] w-full items-center justify-center overflow-hidden rounded-md border bg-muted">
              {pdf.url ? (
                <iframe
                  key={pdf.url}
                  src={pdf.url}
                  title={t('preview.title', { number: certificate.certificateNumber })}
                  className="size-full"
                />
              ) : (
                <p className="text-sm text-muted-foreground">
                  {pdf.isError ? t('preview.failed') : t('preview.loading')}
                </p>
              )}
            </div>

            <DialogFooter>
              <Button variant="outline" asChild disabled={!pdf.url}>
                <a href={pdf.url} target="_blank" rel="noopener noreferrer">
                  <ExternalLink />
                  {t('preview.openInNewTab')}
                </a>
              </Button>
              <Button disabled={!pdf.blob} onClick={() => saveBlob(pdf.blob, pdf.filename ?? `${certificate.certificateNumber}-${lang}.pdf`)}>
                <Download />
                {t('preview.download')}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
