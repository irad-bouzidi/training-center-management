import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/context/AuthContext'
import { studentDetailPathForRole } from '@/lib/roleHomePaths'
import { useStudentsQuery } from './hooks'
import { fullName, STATUS_OPTIONS, statusBadgeVariant, titleCase } from './studentDisplay'

const PAGE_SIZE = 20
const ALL = 'ALL'

/**
 * Admin/Trainer-facing student directory, per
 * docs/tasks/TCM-15-frontend-student-directory.md. Mounted read-only under
 * both /admin/students and /trainer/students (see AdminLayout/TrainerLayout),
 * so row navigation is role-aware rather than hardcoded to one prefix.
 * Students themselves are managed via UsersListPage (TCM-10) - this page is
 * a summary directory only, no create/edit here.
 */
export function StudentsListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [status, setStatus] = useState(ALL)
  const [searchInput, setSearchInput] = useState('')
  const [name, setName] = useState('')
  const [page, setPage] = useState(0)

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

  function handleStatusChange(value) {
    setStatus(value)
    setPage(0)
  }

  const { data, isLoading } = useStudentsQuery({
    page,
    size: PAGE_SIZE,
    status: status === ALL ? undefined : status,
    name: name || undefined,
  })

  const students = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <Card>
      <CardHeader>
        <CardTitle>Students</CardTitle>
      </CardHeader>

      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <Input
            placeholder="Search by name…"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            className="max-w-56"
          />

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
              <TableHead>Active Enrollments</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            )}

            {!isLoading && students.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  No students found.
                </TableCell>
              </TableRow>
            )}

            {students.map(({ profile, activeEnrollments }) => (
              <TableRow
                key={profile.id}
                className="cursor-pointer"
                onClick={() => navigate(studentDetailPathForRole(user.role, profile.id))}
              >
                <TableCell>{fullName(profile)}</TableCell>
                <TableCell>{profile.email}</TableCell>
                <TableCell>{activeEnrollments}</TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(profile.status)}>{titleCase(profile.status)}</Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <p>{totalElements} student{totalElements === 1 ? '' : 's'}</p>
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
    </Card>
  )
}
