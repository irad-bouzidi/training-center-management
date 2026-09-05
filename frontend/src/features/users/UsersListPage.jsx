import { Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useUsersQuery } from './hooks'
import { UserDetailSheet } from './UserDetailSheet'
import { UserFormDialog } from './UserFormDialog'
import { UserRowActions } from './UserRowActions'
import { formatDate, fullName, ROLE_OPTIONS, STATUS_OPTIONS, titleCase } from './userDisplay'

const PAGE_SIZE = 20
const ALL = 'ALL'

export function UsersListPage() {
  const [role, setRole] = useState(ALL)
  const [status, setStatus] = useState(ALL)
  const [searchInput, setSearchInput] = useState('')
  const [name, setName] = useState('')
  const [page, setPage] = useState(0)

  const [formDialog, setFormDialog] = useState(null) // null | { user: null | object }
  const [detailUserId, setDetailUserId] = useState(null)

  // Debounce the search box so every keystroke doesn't fire a request. Once
  // the debounced value actually changes the result set, hop back to page 0
  // rather than staying on a possibly out-of-range page.
  useEffect(() => {
    const handle = setTimeout(() => {
      setName(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(handle)
  }, [searchInput])

  function handleRoleChange(value) {
    setRole(value)
    setPage(0)
  }

  function handleStatusChange(value) {
    setStatus(value)
    setPage(0)
  }

  const { data, isLoading } = useUsersQuery({
    page,
    size: PAGE_SIZE,
    role: role === ALL ? undefined : role,
    status: status === ALL ? undefined : status,
    name: name || undefined,
  })

  const users = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle>Users</CardTitle>
        <Button onClick={() => setFormDialog({ user: null })}>
          <Plus />
          New User
        </Button>
      </CardHeader>

      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <Input
            placeholder="Search by name…"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            className="max-w-56"
          />

          <Select value={role} onValueChange={handleRoleChange}>
            <SelectTrigger className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All roles</SelectItem>
              {ROLE_OPTIONS.map((option) => (
                <SelectItem key={option} value={option}>
                  {titleCase(option)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={status} onValueChange={handleStatusChange}>
            <SelectTrigger className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All statuses</SelectItem>
              {STATUS_OPTIONS.map((option) => (
                <SelectItem key={option} value={option}>
                  {titleCase(option)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Created</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            )}

            {!isLoading && users.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  No users found.
                </TableCell>
              </TableRow>
            )}

            {users.map((user) => (
              <TableRow key={user.id}>
                <TableCell>{fullName(user)}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  <Badge variant="outline">{titleCase(user.role)}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={user.status === 'ACTIVE' ? 'secondary' : 'destructive'}>
                    {titleCase(user.status)}
                  </Badge>
                </TableCell>
                <TableCell>{formatDate(user.createdAt)}</TableCell>
                <TableCell>
                  <UserRowActions
                    user={user}
                    onView={(u) => setDetailUserId(u.id)}
                    onEdit={(u) => setFormDialog({ user: u })}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <p>{totalElements} user{totalElements === 1 ? '' : 's'}</p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((current) => current - 1)}
            >
              Previous
            </Button>
            <span>
              Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      </CardContent>

      {formDialog && (
        <UserFormDialog
          key={formDialog.user?.id ?? 'new'}
          open={Boolean(formDialog)}
          onOpenChange={(next) => !next && setFormDialog(null)}
          user={formDialog.user}
        />
      )}

      <UserDetailSheet
        open={Boolean(detailUserId)}
        onOpenChange={(next) => !next && setDetailUserId(null)}
        userId={detailUserId}
      />
    </Card>
  )
}
