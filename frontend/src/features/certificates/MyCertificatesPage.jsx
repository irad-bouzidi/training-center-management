import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { CertificatesTable } from './CertificatesTable'
import { useCertificatesQuery } from './hooks'

/**
 * The student's own certificates (/student/certificates) - see
 * docs/tasks/TCM-26-frontend-certificates.md step 3. Downloading goes
 * through the authorized client and lands as a real PDF file.
 */
export function MyCertificatesPage() {
  const { data: certificates = [], isLoading } = useCertificatesQuery(undefined)

  return (
    <Card>
      <CardHeader>
        <CardTitle>My Certificates</CardTitle>
        <CardDescription>Courses you’ve completed, and the certificates awarded for them.</CardDescription>
      </CardHeader>
      <CardContent>
        <CertificatesTable
          certificates={certificates}
          isLoading={isLoading}
          emptyMessage="You haven’t been awarded a certificate yet."
        />
      </CardContent>
    </Card>
  )
}
