import { Badge } from '@/components/ui/badge'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { useUserQuery } from './hooks'
import { formatDate, fullName, titleCase } from './userDisplay'

function Field({ label, value }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value}</p>
    </div>
  )
}

/** Read-only user detail, opened from a row's "View" action. */
export function UserDetailSheet({ open, onOpenChange, userId }) {
  const { data: user, isLoading } = useUserQuery(userId)

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{user ? fullName(user) : 'User details'}</SheetTitle>
          <SheetDescription>Account information</SheetDescription>
        </SheetHeader>

        <div className="space-y-4 px-4">
          {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

          {user && (
            <>
              <div className="flex gap-2">
                <Badge variant="outline">{titleCase(user.role)}</Badge>
                <Badge variant={user.status === 'ACTIVE' ? 'secondary' : 'destructive'}>
                  {titleCase(user.status)}
                </Badge>
              </div>

              <Field label="Email" value={user.email} />
              <Field label="Phone" value={user.phone || '—'} />
              <Field label="Created" value={formatDate(user.createdAt)} />
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
