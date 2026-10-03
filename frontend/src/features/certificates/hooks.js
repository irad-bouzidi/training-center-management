import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import {
  downloadCertificate,
  generateCertificate,
  listCertificates,
  listTaughtCertificates,
} from '@/api/certificateApi'
import { studentsKeys } from '@/features/students/hooks'
import { apiErrorMessage } from '@/api/serverErrors'

export const certificatesKeys = {
  all: ['certificates'],
  lists: () => [...certificatesKeys.all, 'list'],
  list: (studentId) => [...certificatesKeys.lists(), studentId ?? 'mine'],
  taught: () => [...certificatesKeys.lists(), 'taught'],
  pdf: (id, lang) => [...certificatesKeys.all, 'pdf', id, lang],
}

/** Every certificate is issued in both; the order here is the order shown. */
export const CERTIFICATE_LANGUAGES = ['en', 'fr']

/**
 * How long a downloaded file's object URL outlives the click. Revoking it
 * straight away is what made certificates "fail to open": the save itself
 * starts, but a browser that then opens the file from that URL (Firefox's
 * "Open in Firefox", Safari) finds it already gone.
 */
const OBJECT_URL_LIFETIME_MS = 60_000

export function useCertificatesQuery(studentId, options) {
  return useQuery({
    queryKey: certificatesKeys.list(studentId),
    queryFn: () => listCertificates(studentId),
    ...options,
  })
}

/** Certificates on the courses the signed-in trainer teaches. */
export function useTaughtCertificatesQuery(options) {
  return useQuery({
    queryKey: certificatesKeys.taught(),
    queryFn: listTaughtCertificates,
    ...options,
  })
}

/**
 * Issuing a certificate adds it to the student's summary as well as to this
 * list, so both go stale together. No error toast: the refusal explains
 * which rule the student fails, which belongs next to the course it's about
 * (see StudentCertificatesTab).
 */
export function useGenerateCertificateMutation() {
  const { t } = useTranslation('certificates')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: generateCertificate,
    onSuccess: (certificate) => {
      queryClient.invalidateQueries({ queryKey: certificatesKeys.all })
      queryClient.invalidateQueries({ queryKey: studentsKeys.all })
      toast.success(t('toasts.issued', { number: certificate.certificateNumber }))
    },
  })
}

/**
 * Downloads one language's PDF and hands it to the browser through an
 * in-memory object URL, which is what lets an authorized XHR end up as a
 * saved file. The URL is kept alive for a while after the click - see
 * OBJECT_URL_LIFETIME_MS.
 */
export function useDownloadCertificateMutation() {
  const { t } = useTranslation('certificates')

  return useMutation({
    mutationFn: ({ id, certificateNumber, lang }) =>
      downloadCertificate(id, lang, `${certificateNumber}-${lang}.pdf`),
    onSuccess: ({ blob, filename }) => saveBlob(blob, filename),
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.downloadFailed'))),
  })
}

/**
 * One language's PDF, as an object URL the preview can put in an <iframe>.
 * Each fetched blob gets its own URL, revoked when the preview moves on to
 * another one or closes. Kept in the query cache, so flipping back to a
 * language already seen doesn't fetch it again.
 *
 * @returns {{url: string|undefined, blob: Blob|undefined, filename: string|undefined, isLoading: boolean, isError: boolean}}
 */
export function useCertificatePdf(id, lang, { enabled = true } = {}) {
  const query = useQuery({
    queryKey: certificatesKeys.pdf(id, lang),
    queryFn: () => downloadCertificate(id, lang),
    enabled: enabled && Boolean(id),
    staleTime: Infinity,
    gcTime: 5 * 60_000,
  })
  const blob = query.data?.blob
  const [current, setCurrent] = useState(null)

  // Made and revoked in the same effect, so StrictMode's mount-unmount-mount
  // never leaves the iframe pointing at a URL that was already revoked. The
  // URL is an external resource, which is what the effect synchronizes.
  useEffect(() => {
    if (!blob) {
      return undefined
    }
    const objectUrl = URL.createObjectURL(blob)
    // oxlint-disable-next-line react/set-state-in-effect
    setCurrent({ blob, url: objectUrl })
    return () => URL.revokeObjectURL(objectUrl)
  }, [blob])

  // Until the effect has caught up with a new blob, the old URL is not shown.
  const url = blob && current?.blob === blob ? current.url : undefined

  return {
    url,
    blob,
    filename: query.data?.filename,
    isLoading: query.isLoading,
    isError: query.isError,
  }
}

/** Saves a blob under `filename` through a throwaway link. */
export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), OBJECT_URL_LIFETIME_MS)
}
