import { AppShell } from './AppShell'

// Disabled items are features later tasks build (TCM-20, 24, 26) - stubbed
// here per docs/tasks/TCM-9-frontend-auth.md.
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/student', enabled: true },
  { label: 'Course Catalog', to: '/student/courses', enabled: true },
  { label: 'My Enrollments', to: '/student/enrollments', enabled: true },
  { label: 'My Attendance', enabled: false },
  { label: 'My Grades', enabled: false },
  { label: 'My Certificates', enabled: false },
]

export function StudentLayout() {
  return <AppShell title="Student" navItems={NAV_ITEMS} />
}
