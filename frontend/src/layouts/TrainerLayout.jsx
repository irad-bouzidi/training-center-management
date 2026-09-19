import { AppShell } from './AppShell'

// The one disabled item is a feature a later task builds - stubbed here per
// docs/tasks/TCM-9-frontend-auth.md. Attendance is taken against a session
// rather than browsed on its own, so it hangs off Schedule (TCM-20), and a
// gradebook is always one course's, so it hangs off the course (TCM-24).
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/trainer', enabled: true },
  { label: 'My Courses', enabled: false },
  { label: 'Course Catalog', to: '/trainer/courses', enabled: true },
  { label: 'Students', to: '/trainer/students', enabled: true },
  { label: 'Schedule', to: '/trainer/schedule', enabled: true },
]

export function TrainerLayout() {
  return <AppShell title="Trainer" navItems={NAV_ITEMS} />
}
