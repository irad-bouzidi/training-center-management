import { AppShell } from './AppShell'

// Every item here is live. Attendance, grades and certificates have no entry
// of their own: a report and a gradebook are always about one course, so they
// live on the course (TCM-20, TCM-24); rosters are marked from the schedule,
// and certificates are issued from a student's summary (TCM-26). The
// cross-cutting figures are the dashboard itself (TCM-29).
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/admin', enabled: true },
  { label: 'Users', to: '/admin/users', enabled: true },
  { label: 'Courses', to: '/admin/courses', enabled: true },
  { label: 'Students', to: '/admin/students', enabled: true },
  { label: 'Enrollments', to: '/admin/enrollments', enabled: true },
  { label: 'Schedule', to: '/admin/schedule', enabled: true },
  { label: 'Payments', to: '/admin/payments', enabled: true },
]

export function AdminLayout() {
  return <AppShell title="Admin" navItems={NAV_ITEMS} />
}
