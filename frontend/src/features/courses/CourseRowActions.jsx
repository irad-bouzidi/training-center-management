import { MoreHorizontal, Pencil } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { STATUS_TRANSITIONS } from './courseDisplay'
import { useSetCourseStatusMutation } from './hooks'

/**
 * Dropdown menu of per-row actions (view/edit/publish-archive), plus the
 * confirm dialog the status change triggers - see
 * docs/tasks/TCM-12-frontend-course-management.md step 6.
 *
 * `onView` is optional: CourseDetailPage reuses this menu for its own header
 * actions, where a "view details" item would just point at the page already
 * showing.
 */
export function CourseRowActions({ course, onView, onEdit }) {
  const { t } = useTranslation('courses')
  const [confirmOpen, setConfirmOpen] = useState(false)
  const setCourseStatus = useSetCourseStatusMutation()
  const transition = STATUS_TRANSITIONS[course.status]

  function confirmStatusChange() {
    setCourseStatus.mutate(
      { id: course.id, status: transition.next },
      { onSuccess: () => setConfirmOpen(false) },
    )
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon-sm" aria-label={t('rowActions.menuLabel', { name: course.name })}>
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {onView && <DropdownMenuItem onSelect={() => onView(course)}>{t('rowActions.viewDetails')}</DropdownMenuItem>}
          <DropdownMenuItem onSelect={() => onEdit(course)}>
            <Pencil />
            {t('common:actions.edit')}
          </DropdownMenuItem>
          {transition && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={() => setConfirmOpen(true)}>
                <transition.icon />
                {t(transition.labelKey)}
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      {transition && (
        <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>{t(transition.confirmTitleKey)}</AlertDialogTitle>
              <AlertDialogDescription>
                {transition.next === 'PUBLISHED'
                  ? t('rowActions.publishDescription', { name: course.name })
                  : t('rowActions.unpublishDescription', { name: course.name })}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>{t('common:actions.cancel')}</AlertDialogCancel>
              <AlertDialogAction
                disabled={setCourseStatus.isPending}
                onClick={(event) => {
                  event.preventDefault()
                  confirmStatusChange()
                }}
              >
                {t(transition.labelKey)}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </>
  )
}
