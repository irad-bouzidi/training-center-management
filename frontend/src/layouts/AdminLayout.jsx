import { AppShell } from './AppShell'

// Disabled items are features later tasks build (TCM-12, 15, 16, 18,
// 20, 22, 24, 26, 29) - stubbed here per docs/tasks/TCM-9-frontend-auth.md.
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/admin', enabled: true },
  { label: 'Users', to: '/admin/users', enabled: true },
  { label: 'Courses', enabled: false },
  { label: 'Students', enabled: false },
  { label: 'Enrollments', enabled: false },
  { label: 'Scheduling', enabled: false },
  { label: 'Attendance', enabled: false },
  { label: 'Payments', enabled: false },
  { label: 'Grades', enabled: false },
  { label: 'Certificates', enabled: false },
  { label: 'Reports', enabled: false },
]

export function AdminLayout() {
  return <AppShell title="Admin" navItems={NAV_ITEMS} />
}
