import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatNumber } from '@/lib/format'
import { formatPercent, scoreBadgeVariant } from './gradeDisplay'

/**
 * One student's assessment entries. Shared by the gradebook's drill-in, the
 * student summary's Grades tab and the student's own page - the same columns
 * answer all three. `onEdit`/`onDelete` are what vary: whoever may change a
 * grade gets the actions, everyone else reads.
 */
export function GradeEntriesTable({ grades, onEdit, onDelete, emptyMessage }) {
  const { t } = useTranslation('grades')

  if (grades.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyMessage}</p>
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t('table.assessment')}</TableHead>
          <TableHead>{t('table.type')}</TableHead>
          <TableHead className="text-right">{t('table.score')}</TableHead>
          <TableHead className="text-right">{t('table.weight')}</TableHead>
          <TableHead className="text-right">{t('table.result')}</TableHead>
          <TableHead>{t('table.gradedBy')}</TableHead>
          {onEdit && <TableHead className="w-32" />}
        </TableRow>
      </TableHeader>
      <TableBody>
        {grades.map((grade) => (
          <TableRow key={grade.id}>
            <TableCell>
              <p className="font-medium">{grade.title}</p>
              {grade.comments && <p className="text-xs text-muted-foreground">{grade.comments}</p>}
            </TableCell>
            <TableCell>{t(`common:enums.assessmentType.${grade.assessmentType}`)}</TableCell>
            <TableCell className="text-right tabular-nums">
              {t('table.scoreOutOf', { score: formatNumber(grade.score), max: formatNumber(grade.maxScore) })}
            </TableCell>
            <TableCell className="text-right tabular-nums">{formatNumber(grade.weight)}</TableCell>
            <TableCell className="text-right">
              <Badge variant={scoreBadgeVariant(grade.percentage)}>{formatPercent(grade.percentage)}</Badge>
            </TableCell>
            <TableCell className="text-muted-foreground">{grade.gradedBy.name}</TableCell>
            {onEdit && (
              <TableCell>
                <div className="flex gap-1">
                  <Button variant="ghost" size="sm" onClick={() => onEdit(grade)}>
                    {t('common:actions.edit')}
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => onDelete(grade)}>
                    {t('common:actions.delete')}
                  </Button>
                </div>
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
