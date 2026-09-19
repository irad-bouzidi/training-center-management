import { AppShell } from './AppShell'

// "My Attendance" is the one stub left: the attendance API (TCM-19) is
// trainer/admin-facing, with no endpoint yet for a student to read their own
// record. Everything else here is live.
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/student', enabled: true },
  { label: 'Course Catalog', to: '/student/courses', enabled: true },
  { label: 'My Enrollments', to: '/student/enrollments', enabled: true },
  { label: 'Schedule', to: '/student/schedule', enabled: true },
  { label: 'My Payments', to: '/student/payments', enabled: true },
  { label: 'My Attendance', enabled: false },
  { label: 'My Grades', to: '/student/grades', enabled: true },
  { label: 'My Certificates', to: '/student/certificates', enabled: true },
]

export function StudentLayout() {
  return <AppShell title="Student" navItems={NAV_ITEMS} />
}
