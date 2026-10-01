import { Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
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
import { formatDate, fullName, ROLE_OPTIONS, STATUS_OPTIONS } from './userDisplay'

const PAGE_SIZE = 20
const ALL = 'ALL'

export function UsersListPage() {
  const { t } = useTranslation('users')
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
        <CardTitle>{t('list.title')}</CardTitle>
        <Button onClick={() => setFormDialog({ user: null })}>
          <Plus />
          {t('list.newUser')}
        </Button>
      </CardHeader>

      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <Input
            placeholder={t('list.searchPlaceholder')}
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            className="max-w-56"
          />

          <Select value={role} onValueChange={handleRoleChange}>
            <SelectTrigger className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{t('list.allRoles')}</SelectItem>
              {ROLE_OPTIONS.map((option) => (
                <SelectItem key={option} value={option}>
                  {t(`common:enums.role.${option}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={status} onValueChange={handleStatusChange}>
            <SelectTrigger className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{t('list.allStatuses')}</SelectItem>
              {STATUS_OPTIONS.map((option) => (
                <SelectItem key={option} value={option}>
                  {t(`common:enums.accountStatus.${option}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('list.columns.name')}</TableHead>
              <TableHead>{t('fields.email')}</TableHead>
              <TableHead>{t('fields.role')}</TableHead>
              <TableHead>{t('fields.status')}</TableHead>
              <TableHead>{t('fields.created')}</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  {t('common:states.loading')}
                </TableCell>
              </TableRow>
            )}

            {!isLoading && users.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  {t('list.empty')}
                </TableCell>
              </TableRow>
            )}

            {users.map((user) => (
              <TableRow key={user.id}>
                <TableCell>{fullName(user)}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  <Badge variant="outline">{t(`common:enums.role.${user.role}`)}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={user.status === 'ACTIVE' ? 'secondary' : 'destructive'}>
                    {t(`common:enums.accountStatus.${user.status}`)}
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
          <p>{t('list.count', { count: totalElements })}</p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((current) => current - 1)}
            >
              {t('common:actions.previous')}
            </Button>
            <span>{t('common:pagination.pageOf', { page: totalPages === 0 ? 0 : page + 1, total: totalPages })}</span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              {t('common:actions.next')}
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
