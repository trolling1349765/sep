import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import Sidebar from '../components/Sidebar';

const TYPE_ICONS = {
    SYSTEM: '🔔',
    APPLICATION: '📄',
    DOCUMENT: '📎',
    PAYMENT: '💰',
    REMINDER: '⏰',
    APPROVAL: '✅',
    VERIFICATION: '✓',
    SUPPLEMENT_REQUEST: '📋',
    FEEDBACK: '💬',
};

const DEFAULT_ICON = '🔔';

function formatTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hr ago`;
    if (diffDays < 7) return `${diffDays} day ago`;
    return date.toLocaleDateString('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
}

export default function NotificationCenterPage() {
    const { user, loading: authLoading } = useAuth();
    const {
        unreadCount,
        notifications,
        loading,
        fetchNotifications,
        markAsRead,
        markAllAsRead,
        deleteNotification,
    } = useNotifications();
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [currentPage, setCurrentPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [activeTab, setActiveTab] = useState('all');

    useEffect(() => {
        if (!authLoading && !user) {
            navigate('/login', { replace: true });
        }
    }, [user, authLoading, navigate]);

    useEffect(() => {
        if (user) {
            fetchNotifications(currentPage, 20).then(data => {
                if (data) {
                    setHasMore(!data.last);
                }
            });
        }
    }, [user, currentPage, fetchNotifications]);

    const handleLoadMore = useCallback(() => {
        if (!loading && hasMore) {
            setCurrentPage(prev => prev + 1);
        }
    }, [loading, hasMore]);

    const handleMarkAsRead = useCallback(async (e, id) => {
        e.stopPropagation();
        await markAsRead(id);
    }, [markAsRead]);

    const handleMarkAllAsRead = useCallback(async () => {
        await markAllAsRead();
    }, [markAllAsRead]);

    const handleDelete = useCallback(async (e, id) => {
        e.stopPropagation();
        await deleteNotification(id);
    }, [deleteNotification]);

    const handleNotificationClick = useCallback((notification) => {
        if (!notification.isRead) {
            markAsRead(notification.id);
        }
        if (notification.actionUrl) {
            navigate(notification.actionUrl);
        }
    }, [markAsRead, navigate]);

    const filteredNotifications = activeTab === 'unread'
        ? notifications.filter(n => !n.isRead)
        : notifications;

    if (authLoading) {
        return (
            <div className="home-loading">
                <div className="spinner" />
                <p>Loading...</p>
            </div>
        );
    }

    if (!user) return null;

    return (
        <div className="dashboard">
            <button
                className="hamburger-btn"
                onClick={() => setSidebarOpen(true)}
                aria-label="Open menu"
            >
                ☰
            </button>

            <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

            <main className="main-content">
                <div className="notification-page">
                    {/* Header */}
                    <div className="notification-header">
                        <div className="notification-header-left">
                            <h1 className="notification-title">Notifications</h1>
                            {unreadCount > 0 && (
                                <span className="notification-unread-badge">
                                    {unreadCount} unread
                                </span>
                            )}
                        </div>
                        <div className="notification-header-actions">
                            {unreadCount > 0 && (
                                <button
                                    className="notification-action-btn"
                                    onClick={handleMarkAllAsRead}
                                >
                                    ✓ Mark all as read
                                </button>
                            )}
                        </div>
                    </div>

                    {/* Tabs */}
                    <div className="notification-tabs">
                        <button
                            className={`notification-tab ${activeTab === 'all' ? 'notification-tab--active' : ''}`}
                            onClick={() => setActiveTab('all')}
                        >
                            All
                        </button>
                        <button
                            className={`notification-tab ${activeTab === 'unread' ? 'notification-tab--active' : ''}`}
                            onClick={() => setActiveTab('unread')}
                        >
                            Unread {unreadCount > 0 && `(${unreadCount})`}
                        </button>
                    </div>

                    {/* Notification List */}
                    <div className="notification-list">
                        {loading && currentPage === 0 ? (
                            <div className="notification-loading">
                                <div className="spinner" />
                                <p>Loading notifications...</p>
                            </div>
                        ) : filteredNotifications.length === 0 ? (
                            <div className="notification-empty">
                                <span className="notification-empty-icon">🔔</span>
                                <h3>No notifications yet</h3>
                                <p>You're all caught up! New notifications will appear here.</p>
                            </div>
                        ) : (
                            <>
                                {filteredNotifications.map((notification) => (
                                    <div
                                        key={notification.id}
                                        className={`notification-item ${!notification.isRead ? 'notification-item--unread' : ''}`}
                                        onClick={() => handleNotificationClick(notification)}
                                    >
                                        <div className="notification-item-icon">
                                            {TYPE_ICONS[notification.type] || DEFAULT_ICON}
                                        </div>

                                        <div className="notification-item-content">
                                            <div className="notification-item-header">
                                                <span className="notification-item-type">
                                                    {notification.type}
                                                </span>
                                                <span className="notification-item-time">
                                                    {formatTime(notification.createdAt)}
                                                </span>
                                            </div>
                                            <h4 className="notification-item-title">
                                                {notification.title}
                                            </h4>
                                            <p className="notification-item-message">
                                                {notification.message}
                                            </p>
                                        </div>

                                        <div className="notification-item-actions">
                                            {!notification.isRead && (
                                                <button
                                                    className="notification-read-btn"
                                                    onClick={(e) => handleMarkAsRead(e, notification.id)}
                                                    title="Mark as read"
                                                >
                                                    ✓
                                                </button>
                                            )}
                                            <button
                                                className="notification-delete-btn"
                                                onClick={(e) => handleDelete(e, notification.id)}
                                                title="Delete"
                                            >
                                                ✕
                                            </button>
                                        </div>
                                    </div>
                                ))}

                                {hasMore && (
                                    <div className="notification-load-more">
                                        <button
                                            className="notification-load-more-btn"
                                            onClick={handleLoadMore}
                                            disabled={loading}
                                        >
                                            {loading ? 'Loading...' : 'Load more'}
                                        </button>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}