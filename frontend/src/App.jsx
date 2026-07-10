import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import HomePage from './pages/HomePage';
import ProfilePage from './pages/ProfilePage';
import UpdateProfilePage from './pages/UpdateProfilePage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import NotificationCenterPage from './pages/NotificationCenterPage';
import SupportRequestPage from './pages/SupportRequestPage';
import SupportRequestManagementPage from './pages/SupportRequestManagementPage';
import CitizenPortalPage from './pages/CitizenPortalPage';
import PolicyDetailPage from './pages/PolicyDetailPage';
import DonationManagementPage from './pages/DonationManagementPage';
import SponsorListPage from './pages/SponsorListPage';
import DonationListPage from './pages/DonationListPage';
import InventoryPage from './pages/InventoryPage';
import DistributionPage from './pages/DistributionPage';

function App() {
  return (
    <Router>
      <AuthProvider>
        <NotificationProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/home" element={<HomePage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/profile/update" element={<UpdateProfilePage />} />
            <Route path="/profile/change-password" element={<ChangePasswordPage />} />
            <Route path="/notifications" element={<NotificationCenterPage />} />
            <Route path="/support" element={<SupportRequestPage />} />
            <Route path="/support/manage" element={<SupportRequestManagementPage />} />
            <Route path="/citizen-portal" element={<CitizenPortalPage />} />
            <Route path="/citizen-portal/policy/:id" element={<PolicyDetailPage />} />
            {/* Donation Management - CB_QUAN_LY */}
            <Route path="/donation-management" element={<DonationManagementPage />} />
            <Route path="/donation-management/sponsors" element={<SponsorListPage />} />
            <Route path="/donation-management/donations" element={<DonationListPage />} />
            <Route path="/donation-management/inventory" element={<InventoryPage />} />
            <Route path="/donation-management/distribution" element={<DistributionPage />} />
            <Route path="/" element={<Navigate to="/home" replace />} />
            <Route path="*" element={<Navigate to="/home" replace />} />
          </Routes>
        </NotificationProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
