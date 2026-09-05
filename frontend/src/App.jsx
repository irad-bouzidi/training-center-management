import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Toaster } from '@/components/ui/sonner'
import { AuthProvider } from '@/context/AuthContext'
import { LoginPage } from '@/features/auth/LoginPage'
import { CourseCatalogPage } from '@/features/courses/CourseCatalogPage'
import { CourseDetailPage } from '@/features/courses/CourseDetailPage'
import { CoursesListPage } from '@/features/courses/CoursesListPage'
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

              <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
                <Route path="/admin" element={<AdminLayout />}>
                  <Route index element={<HomePage />} />
                  <Route path="users" element={<UsersListPage />} />
                  <Route path="courses" element={<CoursesListPage />} />
                  <Route path="courses/:id" element={<CourseDetailPage />} />
                </Route>
              </Route>

              <Route element={<RoleRoute allowedRoles={['TRAINER']} />}>
                <Route path="/trainer" element={<TrainerLayout />}>
                  <Route index element={<HomePage />} />
                  <Route path="courses" element={<CourseCatalogPage />} />
                  <Route path="courses/:id" element={<CourseDetailPage />} />
                </Route>
              </Route>

              <Route element={<RoleRoute allowedRoles={['STUDENT']} />}>
                <Route path="/student" element={<StudentLayout />}>
                  <Route index element={<HomePage />} />
                  <Route path="courses" element={<CourseCatalogPage />} />
                  <Route path="courses/:id" element={<CourseDetailPage />} />
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
