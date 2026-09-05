import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/context/AuthContext'

/** Placeholder role home page, rendered inside each of AdminLayout /
 * TrainerLayout / StudentLayout. Real dashboards land in later tasks
 * (TCM-29 for the reports dashboard; each domain's own pages before that). */
export function HomePage() {
  const { user } = useAuth()

  return (
    <Card>
      <CardHeader>
        <CardTitle>Welcome, {user.name}</CardTitle>
        <CardDescription>
          <Badge variant="secondary">{user.role}</Badge>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          This is a placeholder home page. Real features land incrementally
          as later tasks build them out - use the sidebar to see what's
          coming.
        </p>
      </CardContent>
    </Card>
  )
}
