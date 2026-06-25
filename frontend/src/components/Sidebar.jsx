import { useLocation, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';

const citizenMenuItems = [
    { path: '/home', label: 'Home', icon: '🏠' },
    { path: '/profile', label: 'User Profile', icon: '👤' },
    { path: '/citizen-portal', label: 'Citizen Portal', icon: '🏛️' },
    { path: '/my-applications', label: 'My Applications', icon: '📁' },
    { path: '/support', label: 'Support Request', icon: '🎧' },
];

const officerMenuItems = [
    { path: '/support/manage', label: 'Manage Support', icon: '📋' },
];

export default function Sidebar({ isOpen, onClose }) {
    const { user, logout } = useAuth();
    const { unreadCount } = useNotifications();
    const location = useLocation();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    const handleNotificationClick = () => {
        navigate('/notifications');
    };

    const handleProfileClick = () => {
        navigate('/profile');
    };

    const getInitial = (name) => {
        if (!name) return 'U';
        return name.charAt(0).toUpperCase();
    };

    return (
        <>
            {/* Overlay for mobile */}
            {isOpen && <div className="sidebar-overlay" onClick={onClose} />}

            <aside className={`sidebar ${isOpen ? 'sidebar--open' : ''}`}>
                {/* Top-Left Header: Notification + Mini Profile */}
                <div className="sidebar-header">
                    <button
                        className="notification-btn"
                        onClick={handleNotificationClick}
                        title="Notifications"
                    >
                        <span className="notification-icon">🔔</span>
                        {unreadCount > 0 && (
                            <span className="notification-badge">
                                {unreadCount > 99 ? '99+' : unreadCount}
                            </span>
                        )}
                    </button>

                    <button
                        className="mini-profile"
                        onClick={handleProfileClick}
                        title="View Profile"
                    >
                        <div className="mini-profile-avatar">
                            {user?.avatarUrl ? (
                                <img
                                    src={user.avatarUrl}
                                    alt={user?.fullName || user?.name || 'User'}
                                    className="mini-profile-img"
                                />
                            ) : (
                                <span className="mini-profile-initial">
                                    {getInitial(user?.fullName || user?.name)}
                                </span>
                            )}
                        </div>
                        <div className="mini-profile-info">
                            <span className="mini-profile-name">
                                {user?.fullName || user?.name || 'User'}
                            </span>
                            <span className="mini-profile-role">
                                {user?.role || 'Citizen'}
                            </span>
                        </div>
                    </button>
                </div>

                {/* Menu Bar */}
                <nav className="sidebar-menu">
                    {citizenMenuItems.map((item) => {
                        const isActive = location.pathname === item.path;
                        return (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`sidebar-menu-item ${isActive ? 'sidebar-menu-item--active' : ''}`}
                            >
                                <span className="sidebar-menu-icon">{item.icon}</span>
                                <span className="sidebar-menu-label">{item.label}</span>
                            </Link>
                        );
                    })}
                    {(user?.role === 'RECEPTION_OFFICER' || user?.role === 'SOCIAL_AFFAIRS_OFFICER') && (
                        <div className="sidebar-divider">
                            <span className="sidebar-divider-label">Officer Tools</span>
                        </div>
                    )}
                    {officerMenuItems.map((item) => {
                        const isOfficer = user?.role === 'RECEPTION_OFFICER' || user?.role === 'SOCIAL_AFFAIRS_OFFICER';
                        if (!isOfficer) return null;
                        const isActive = location.pathname === item.path;
                        return (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`sidebar-menu-item ${isActive ? 'sidebar-menu-item--active' : ''}`}
                            >
                                <span className="sidebar-menu-icon">{item.icon}</span>
                                <span className="sidebar-menu-label">{item.label}</span>
                            </Link>
                        );
                    })}
                </nav>

                {/* Logout button pinned at bottom */}
                <div className="sidebar-footer">
                    <button className="sidebar-logout-btn" onClick={handleLogout}>
                        <span className="sidebar-menu-icon">🚪</span>
                        <span className="sidebar-menu-label">Logout</span>
                    </button>
                </div>
            </aside>
        </>
    );
}
