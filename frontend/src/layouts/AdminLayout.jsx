import { AppShell } from './AppShell'

// Every item here is live. Attendance, grades and certificates have no entry
// of their own: a report and a gradebook are always about one course, so they
// live on the course (TCM-20, TCM-24); rosters are marked from the schedule,
// and certificates are issued from a student's summary (TCM-26). The
// cross-cutting figures are the dashboard itself (TCM-29).
const NAV_ITEMS = [
  { labelKey: 'nav.dashboard', to: '/admin', enabled: true },
  { labelKey: 'nav.users', to: '/admin/users', enabled: true },
  { labelKey: 'nav.courses', to: '/admin/courses', enabled: true },
  { labelKey: 'nav.students', to: '/admin/students', enabled: true },
  { labelKey: 'nav.enrollments', to: '/admin/enrollments', enabled: true },
  { labelKey: 'nav.schedule', to: '/admin/schedule', enabled: true },
  { labelKey: 'nav.payments', to: '/admin/payments', enabled: true },
]

export function AdminLayout() {
  return <AppShell titleKey="roles.admin" navItems={NAV_ITEMS} />
}
