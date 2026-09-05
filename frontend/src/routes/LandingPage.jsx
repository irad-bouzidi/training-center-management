import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

export function LandingPage() {
  return (
    <div className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Training Center Management</CardTitle>
          <CardDescription>
            Frontend bootstrap is up and running.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            React + Vite + Tailwind CSS + shadcn/ui are wired together.
            Real pages land in later tasks.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
