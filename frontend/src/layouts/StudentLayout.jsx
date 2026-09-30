import { AppShell } from './AppShell'

// Every item is live. "Check In" is where a student scans a session's QR
// code (TCM-28); "My Attendance" reads back what was recorded, by a trainer
// or by a scan (GET /attendance/mine).
const NAV_ITEMS = [
  { labelKey: 'nav.dashboard', to: '/student', enabled: true },
  { labelKey: 'nav.courseCatalog', to: '/student/courses', enabled: true },
  { labelKey: 'nav.myEnrollments', to: '/student/enrollments', enabled: true },
  { labelKey: 'nav.schedule', to: '/student/schedule', enabled: true },
  { labelKey: 'nav.myPayments', to: '/student/payments', enabled: true },
  { labelKey: 'nav.checkIn', to: '/attend', enabled: true },
  { labelKey: 'nav.myAttendance', to: '/student/attendance', enabled: true },
  { labelKey: 'nav.myGrades', to: '/student/grades', enabled: true },
  { labelKey: 'nav.myCertificates', to: '/student/certificates', enabled: true },
]

export function StudentLayout() {
  return <AppShell titleKey="roles.student" navItems={NAV_ITEMS} />
}
