import { Button } from '@/components/ui/button'
import { enrollBlockedReason } from './enrollmentDisplay'
import { useRegisterMutation } from './hooks'

/**
 * The Student's "Enroll" action, shared by the catalog cards and the course
 * detail page - see docs/tasks/TCM-16-frontend-course-catalog-enrollment.md
 * step 2. When enrollment isn't available the button stays visible but
 * disabled, labelled with the reason ("Already enrolled" / "Pending approval"
 * / "Full"), so the card still says where the student stands.
 *
 * `enrollment` is this student's existing enrollment for the course, or
 * undefined if they have none.
 */
export function EnrollButton({ course, enrollment, size = 'sm' }) {
  const register = useRegisterMutation()
  const blockedReason = enrollBlockedReason(course, enrollment)

  return (
    <Button
      size={size}
      disabled={Boolean(blockedReason) || register.isPending}
      // The catalog card is itself a click target routing to the course
      // detail page - enrolling shouldn't also navigate away from the grid.
      onClick={(event) => {
        event.stopPropagation()
        register.mutate({ courseId: course.id })
      }}
    >
      {blockedReason ?? 'Enroll'}
    </Button>
  )
}
