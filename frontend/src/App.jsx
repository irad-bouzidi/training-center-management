import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Toaster } from '@/components/ui/sonner'
import { AuthProvider } from '@/context/AuthContext'
import { AttendanceReportPage } from '@/features/attendance/AttendanceReportPage'
import { MarkAttendancePage } from '@/features/attendance/MarkAttendancePage'
import { QrCheckinPage } from '@/features/attendance/QrCheckinPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { MyCertificatesPage } from '@/features/certificates/MyCertificatesPage'
import { CourseCatalogPage } from '@/features/courses/CourseCatalogPage'
import { CourseDetailPage } from '@/features/courses/CourseDetailPage'
import { CoursesListPage } from '@/features/courses/CoursesListPage'
import { EnrollmentApprovalsPage } from '@/features/enrollments/EnrollmentApprovalsPage'
import { ScheduleListPage } from '@/features/schedule/ScheduleListPage'
import { GradebookPage } from '@/features/grades/GradebookPage'
import { MyGradesPage } from '@/features/grades/MyGradesPage'
import { MyPaymentsPage } from '@/features/payments/MyPaymentsPage'
import { PaymentsListPage } from '@/features/payments/PaymentsListPage'
import { MyEnrollmentsPage } from '@/features/students/MyEnrollmentsPage'
import { StudentSummaryPage } from '@/features/students/StudentSummaryPage'
import { StudentsListPage } from '@/features/students/StudentsListPage'
import { UsersListPage } from '@/features/users/UsersListPage'
import { AdminLayout } from '@/layouts/AdminLayout'
import { StudentLayout } from '@/layouts/StudentLayout'
import { TrainerLayout } from '@/layouts/TrainerLayout'
import { HomePage } from './routes/HomePage'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { RoleRoute } from './routes/RoleRoute'
import { RootRedirect } from './routes/RootRedirect'

const queryClient = new QueryClient()

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />

            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<RootRedirect />} />

              {/* Where a scanned QR code lands (TCM-28). Outside the role
                  layouts on purpose: it is opened from a phone's camera app,
                  not navigated to, and only needs the student to be signed
                  in. */}
              <Route path="/attend" element={<QrCheckinPage />} />
              <Route path="/attend/:sessionId" element={<QrCheckinPage />} />

              <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
                <Route path="/admin" element={<AdminLayout />}>
                  <Route index element={<HomePage />} />
                  <Route path="users" element={<UsersListPage />} />
                  <Route path="courses" element={<CoursesListPage />} />
                  <Route path="courses/:id" element={<CourseDetailPage />} />
                  <Route path="courses/:courseId/attendance" element={<AttendanceReportPage />} />
                  <Route path="courses/:courseId/grades" element={<GradebookPage />} />
                  <Route path="students" element={<StudentsListPage />} />
                  <Route path="students/:id" element={<StudentSummaryPage />} />
                  <Route path="enrollments" element={<EnrollmentApprovalsPage />} />
                  <Route path="schedule" element={<ScheduleListPage />} />
                  <Route path="sessions/:sessionId/attendance" element={<MarkAttendancePage />} />
                  <Route path="payments" element={<PaymentsListPage />} />
                </Route>
              </Route>

              <Route element={<RoleRoute allowedRoles={['TRAINER']} />}>
                <Route path="/trainer" element={<TrainerLayout />}>
                  <Route index element={<HomePage />} />
                  <Route path="courses" element={<CourseCatalogPage />} />
                  <Route path="courses/:id" element={<CourseDetailPage />} />
                  <Route path="students" element={<StudentsListPage />} />
                  <Route path="students/:id" element={<StudentSummaryPage />} />
                  <Route path="schedule" element={<ScheduleListPage />} />
                  <Route path="sessions/:sessionId/attendance" element={<MarkAttendancePage />} />
                  <Route path="courses/:courseId/grades" element={<GradebookPage />} />
                </Route>
              </Route>

              <Route element={<RoleRoute allowedRoles={['STUDENT']} />}>
                <Route path="/student" element={<StudentLayout />}>
                  <Route index element={<HomePage />} />
                  <Route path="courses" element={<CourseCatalogPage />} />
                  <Route path="courses/:id" element={<CourseDetailPage />} />
                  <Route path="enrollments" element={<MyEnrollmentsPage />} />
                  <Route path="schedule" element={<ScheduleListPage />} />
                  <Route path="payments" element={<MyPaymentsPage />} />
                  <Route path="grades" element={<MyGradesPage />} />
                  <Route path="certificates" element={<MyCertificatesPage />} />
                </Route>
              </Route>
            </Route>
          </Routes>
          <Toaster />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
