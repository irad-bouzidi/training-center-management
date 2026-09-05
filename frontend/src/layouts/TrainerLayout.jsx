import { AppShell } from './AppShell'

// Disabled items are features later tasks build (TCM-18, 20, 24) -
// stubbed here per docs/tasks/TCM-9-frontend-auth.md.
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/trainer', enabled: true },
  { label: 'My Courses', enabled: false },
  { label: 'Course Catalog', to: '/trainer/courses', enabled: true },
  { label: 'Scheduling', enabled: false },
  { label: 'Attendance', enabled: false },
  { label: 'Grades', enabled: false },
]

export function TrainerLayout() {
  return <AppShell title="Trainer" navItems={NAV_ITEMS} />
}
