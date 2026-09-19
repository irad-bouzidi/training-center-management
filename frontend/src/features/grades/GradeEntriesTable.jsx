import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatPercent, scoreBadgeVariant, titleCase } from './gradeDisplay'

/**
 * One student's assessment entries. Shared by the gradebook's drill-in, the
 * student summary's Grades tab and the student's own page - the same columns
 * answer all three. `onEdit`/`onDelete` are what vary: whoever may change a
 * grade gets the actions, everyone else reads.
 */
export function GradeEntriesTable({ grades, onEdit, onDelete, emptyMessage }) {
  if (grades.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyMessage}</p>
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Assessment</TableHead>
          <TableHead>Type</TableHead>
          <TableHead className="text-right">Score</TableHead>
          <TableHead className="text-right">Weight</TableHead>
          <TableHead className="text-right">Result</TableHead>
          <TableHead>Graded by</TableHead>
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
            <TableCell>{titleCase(grade.assessmentType)}</TableCell>
            <TableCell className="text-right tabular-nums">
              {grade.score} / {grade.maxScore}
            </TableCell>
            <TableCell className="text-right tabular-nums">{grade.weight}</TableCell>
            <TableCell className="text-right">
              <Badge variant={scoreBadgeVariant(grade.percentage)}>{formatPercent(grade.percentage)}</Badge>
            </TableCell>
            <TableCell className="text-muted-foreground">{grade.gradedBy.name}</TableCell>
            {onEdit && (
              <TableCell>
                <div className="flex gap-1">
                  <Button variant="ghost" size="sm" onClick={() => onEdit(grade)}>
                    Edit
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => onDelete(grade)}>
                    Delete
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
