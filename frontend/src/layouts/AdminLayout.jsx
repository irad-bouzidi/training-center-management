import { AppShell } from './AppShell'

// Disabled items are features later tasks build (TCM-24, 26, 29) -
// stubbed here per docs/tasks/TCM-9-frontend-auth.md. Attendance has no entry
// of its own: a report is always about one course, so it lives as a tab on
// the course (TCM-20), and rosters are marked from the schedule.
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/admin', enabled: true },
  { label: 'Users', to: '/admin/users', enabled: true },
  { label: 'Courses', to: '/admin/courses', enabled: true },
  { label: 'Students', to: '/admin/students', enabled: true },
  { label: 'Enrollments', to: '/admin/enrollments', enabled: true },
  { label: 'Schedule', to: '/admin/schedule', enabled: true },
  { label: 'Payments', to: '/admin/payments', enabled: true },
  { label: 'Grades', enabled: false },
  { label: 'Certificates', enabled: false },
  { label: 'Reports', enabled: false },
]

export function AdminLayout() {
  return <AppShell title="Admin" navItems={NAV_ITEMS} />
}
