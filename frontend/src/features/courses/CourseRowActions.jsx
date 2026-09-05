import { MoreHorizontal, Pencil } from 'lucide-react'
import { useState } from 'react'
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
          <Button variant="ghost" size="icon-sm" aria-label={`Actions for ${course.name}`}>
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {onView && <DropdownMenuItem onSelect={() => onView(course)}>View details</DropdownMenuItem>}
          <DropdownMenuItem onSelect={() => onEdit(course)}>
            <Pencil />
            Edit
          </DropdownMenuItem>
          {transition && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={() => setConfirmOpen(true)}>
                <transition.icon />
                {transition.label}
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      {transition && (
        <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>{transition.label} course?</AlertDialogTitle>
              <AlertDialogDescription>
                {transition.next === 'PUBLISHED'
                  ? `${course.name} will become visible in the shared catalog.`
                  : `${course.name} will no longer appear in the shared catalog.`}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction
                disabled={setCourseStatus.isPending}
                onClick={(event) => {
                  event.preventDefault()
                  confirmStatusChange()
                }}
              >
                {transition.label}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </>
  )
}
