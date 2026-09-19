import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { downloadCertificate, generateCertificate, listCertificates } from '@/api/certificateApi'
import { studentsKeys } from '@/features/students/hooks'

export const certificatesKeys = {
  all: ['certificates'],
  lists: () => [...certificatesKeys.all, 'list'],
  list: (studentId) => [...certificatesKeys.lists(), studentId ?? 'mine'],
}

// The backend never has a message body it can't produce (see
// GlobalExceptionHandler) - falling back to a generic string only covers a
// network-level failure (no response at all).
export function errorMessage(error, fallback) {
  return error.response?.data?.message ?? fallback
}

export function useCertificatesQuery(studentId, options) {
  return useQuery({
    queryKey: certificatesKeys.list(studentId),
    queryFn: () => listCertificates(studentId),
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
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: generateCertificate,
    onSuccess: (certificate) => {
      queryClient.invalidateQueries({ queryKey: certificatesKeys.all })
      queryClient.invalidateQueries({ queryKey: studentsKeys.all })
      toast.success(`Certificate ${certificate.certificateNumber} issued`)
    },
  })
}

/**
 * Downloads the PDF and hands it to the browser through an in-memory object
 * URL, which is what lets an authorized XHR end up as a saved file. The URL
 * is revoked straight after the click - it only has to survive one.
 */
export function useDownloadCertificateMutation() {
  return useMutation({
    mutationFn: ({ id, certificateNumber }) => downloadCertificate(id, `${certificateNumber}.pdf`),
    onSuccess: ({ blob, filename }) => {
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to download the certificate')),
  })
}
