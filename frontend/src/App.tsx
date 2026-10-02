import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { AppShell } from './components/layout/AppShell';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { EmployeesPage } from './pages/EmployeesPage';
import { AssetsPage } from './pages/AssetsPage';
import { AssetDetailPage } from './pages/AssetDetailPage';
import { AssignmentsPage } from './pages/AssignmentsPage';
import { LicensesPage } from './pages/LicensesPage';
import { MaintenancePage } from './pages/MaintenancePage';
import { TicketsPage } from './pages/TicketsPage';
import { TicketDetailPage } from './pages/TicketDetailPage';
import { LicenseDetailPage } from './pages/LicenseDetailPage';
import { DepartmentsPage } from './pages/DepartmentsPage';
import { ProfilePage } from './pages/ProfilePage';
import { NotFoundPage } from './pages/NotFoundPage';

export default function App() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/login" element={<LoginPage />} />

      {/* Everything under here is auth-gated behind the same shell */}
      <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
        <Route path="/"             element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard"    element={<DashboardPage />} />
        {/* Directory data is ADMIN / IT_MANAGER only on the backend — guard the routes too */}
        <Route path="/employees"    element={<ProtectedRoute roles={['ADMIN', 'IT_MANAGER']}><EmployeesPage /></ProtectedRoute>} />
        <Route path="/departments"  element={<ProtectedRoute roles={['ADMIN', 'IT_MANAGER']}><DepartmentsPage /></ProtectedRoute>} />
        <Route path="/assets"       element={<AssetsPage />} />
        <Route path="/assets/:id"   element={<AssetDetailPage />} />
        <Route path="/assignments"  element={<AssignmentsPage />} />
        <Route path="/licenses"     element={<LicensesPage />} />
        <Route path="/licenses/:id" element={<LicenseDetailPage />} />
        <Route path="/maintenance"  element={<MaintenancePage />} />
        <Route path="/tickets"      element={<TicketsPage />} />
        <Route path="/tickets/:id"  element={<TicketDetailPage />} />
        <Route path="/profile"      element={<ProfilePage />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
