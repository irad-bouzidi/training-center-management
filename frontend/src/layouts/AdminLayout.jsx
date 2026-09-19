import { AppShell } from './AppShell'

// Disabled items are features later tasks build (TCM-26, 29) - stubbed here
// per docs/tasks/TCM-9-frontend-auth.md. Attendance and grades have no entry
// of their own: a report and a gradebook are always about one course, so they
// live on the course (TCM-20, TCM-24), and rosters are marked from the
// schedule.
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/admin', enabled: true },
  { label: 'Users', to: '/admin/users', enabled: true },
  { label: 'Courses', to: '/admin/courses', enabled: true },
  { label: 'Students', to: '/admin/students', enabled: true },
  { label: 'Enrollments', to: '/admin/enrollments', enabled: true },
  { label: 'Schedule', to: '/admin/schedule', enabled: true },
  { label: 'Payments', to: '/admin/payments', enabled: true },
  { label: 'Certificates', enabled: false },
  { label: 'Reports', enabled: false },
]

export function AdminLayout() {
  return <AppShell title="Admin" navItems={NAV_ITEMS} />
}
