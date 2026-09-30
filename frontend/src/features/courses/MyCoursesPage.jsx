import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { statusBadgeVariant } from './courseDisplay'
import { useMyCoursesQuery } from './hooks'

const PAGE_SIZE = 20

/**
 * The trainer's own courses (/trainer/my-courses), every status - unlike the
 * catalog, which only lists PUBLISHED ones, a trainer needs to see a DRAFT
 * they've been assigned before it opens, and an ARCHIVED one they still owe
 * grades on. Each row leads to the course's detail page (roster, schedule)
 * and straight to its gradebook.
 */
export function MyCoursesPage() {
  const { t } = useTranslation('courses')
  const [page, setPage] = useState(0)
  const { data, isLoading } = useMyCoursesQuery({ page, size: PAGE_SIZE, sort: 'name,asc' })

  const courses = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('mine.title')}</CardTitle>
        <CardDescription>{t('mine.description')}</CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('mine.columns.code')}</TableHead>
              <TableHead>{t('mine.columns.name')}</TableHead>
              <TableHead>{t('mine.columns.category')}</TableHead>
              <TableHead className="text-right">{t('mine.columns.enrolled')}</TableHead>
              <TableHead>{t('mine.columns.status')}</TableHead>
              <TableHead className="w-28" />
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

            {!isLoading && courses.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  {t('mine.empty')}
                </TableCell>
              </TableRow>
            )}

            {courses.map((course) => (
              <TableRow key={course.id}>
                <TableCell className="font-mono text-xs">{course.code}</TableCell>
                <TableCell>
                  <Link to={`/trainer/courses/${course.id}`} className="font-medium hover:underline">
                    {course.name}
                  </Link>
                </TableCell>
                <TableCell>{course.category || '—'}</TableCell>
                <TableCell className="text-right tabular-nums">
                  {course.approvedCount} / {course.capacity}
                </TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(course.status)}>{t(`common:enums.courseStatus.${course.status}`)}</Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button asChild variant="outline" size="sm">
                    <Link to={`/trainer/courses/${course.id}/grades`}>{t('shared.gradebook')}</Link>
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <p>{t('shared.count', { count: totalElements })}</p>
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
    </Card>
  )
}
