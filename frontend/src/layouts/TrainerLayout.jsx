import { AppShell } from './AppShell'

// Every item is live. "My Courses" is the trainer's own courses in any
// status; "Course Catalog" is the shared, PUBLISHED-only one. Attendance is taken against a session
// rather than browsed on its own, so it hangs off Schedule (TCM-20), and a
// gradebook is always one course's, so it hangs off the course (TCM-24).
// "Certificates" lists those issued on the trainer's own courses.
const NAV_ITEMS = [
  { labelKey: 'nav.dashboard', to: '/trainer', enabled: true },
  { labelKey: 'nav.myCourses', to: '/trainer/my-courses', enabled: true },
  { labelKey: 'nav.courseCatalog', to: '/trainer/courses', enabled: true },
  { labelKey: 'nav.students', to: '/trainer/students', enabled: true },
  { labelKey: 'nav.schedule', to: '/trainer/schedule', enabled: true },
  { labelKey: 'nav.certificates', to: '/trainer/certificates', enabled: true },
]

export function TrainerLayout() {
  return <AppShell titleKey="roles.trainer" navItems={NAV_ITEMS} />
}
