import { AppShell } from './AppShell'

// Every item is live. "Check In" is where a student scans a session's QR
// code (TCM-28); "My Attendance" reads back what was recorded, by a trainer
// or by a scan (GET /attendance/mine).
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/student', enabled: true },
  { label: 'Course Catalog', to: '/student/courses', enabled: true },
  { label: 'My Enrollments', to: '/student/enrollments', enabled: true },
  { label: 'Schedule', to: '/student/schedule', enabled: true },
  { label: 'My Payments', to: '/student/payments', enabled: true },
  { label: 'Check In', to: '/attend', enabled: true },
  { label: 'My Attendance', to: '/student/attendance', enabled: true },
  { label: 'My Grades', to: '/student/grades', enabled: true },
  { label: 'My Certificates', to: '/student/certificates', enabled: true },
]

export function StudentLayout() {
  return <AppShell title="Student" navItems={NAV_ITEMS} />
}
