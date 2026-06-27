import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import apiClient from '../api/client';
import { useAuth } from './AuthContext';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
    const { user } = useAuth();
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(false);
    const pollingRef = useRef(null);

    const fetchUnreadCount = useCallback(async () => {
        if (!user) {
            setUnreadCount(0);
            return;
        }
        try {
            const response = await apiClient.get('/notifications/unread/count');
            if (response.data?.code === 200) {
                setUnreadCount(response.data.data);
            }
        } catch {
            // ignore
        }
    }, [user]);

    const fetchNotifications = useCallback(async (page = 0, size = 20) => {
        if (!user) {
            setNotifications([]);
            return;
        }
        setLoading(true);
        try {
            const response = await apiClient.get(`/notifications?page=${page}&size=${size}`);
            if (response.data?.code === 200 && response.data?.data) {
                setNotifications(response.data.data.content || []);
                return response.data.data;
            }
        } catch {
            // ignore
        } finally {
            setLoading(false);
        }
    }, [user]);

    const fetchUnreadList = useCallback(async () => {
        if (!user) return [];
        try {
            const response = await apiClient.get('/notifications/unread');
            if (response.data?.code === 200) {
                return response.data.data || [];
            }
        } catch {
            // ignore
        }
        return [];
    }, [user]);

    const markAsRead = useCallback(async (id) => {
        try {
            await apiClient.put(`/notifications/${id}/read`);
            setNotifications(prev =>
                prev.map(n => n.id === id ? { ...n, isRead: true } : n)
            );
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch {
            // ignore
        }
    }, []);

    const markAllAsRead = useCallback(async () => {
        try {
            await apiClient.put('/notifications/read-all');
            setNotifications(prev =>
                prev.map(n => ({ ...n, isRead: true }))
            );
            setUnreadCount(0);
        } catch {
            // ignore
        }
    }, []);

    const deleteNotification = useCallback(async (id) => {
        try {
            await apiClient.delete(`/notifications/${id}`);
            setNotifications(prev => {
                const removed = prev.find(n => n.id === id);
                if (removed && !removed.isRead) {
                    setUnreadCount(c => Math.max(0, c - 1));
                }
                return prev.filter(n => n.id !== id);
            });
        } catch {
            // ignore
        }
    }, []);

    // Poll for unread count every 30 seconds
    useEffect(() => {
        if (user) {
            fetchUnreadCount();
            pollingRef.current = setInterval(fetchUnreadCount, 30000);
        } else {
            if (pollingRef.current) {
                clearInterval(pollingRef.current);
                pollingRef.current = null;
            }
            setUnreadCount(0);
            setNotifications([]);
        }

        return () => {
            if (pollingRef.current) {
                clearInterval(pollingRef.current);
                pollingRef.current = null;
            }
        };
    }, [user, fetchUnreadCount]);

    return (
        <NotificationContext.Provider value={{
            unreadCount,
            notifications,
            loading,
            fetchNotifications,
            fetchUnreadList,
            markAsRead,
            markAllAsRead,
            deleteNotification,
            refreshUnreadCount: fetchUnreadCount,
        }}>
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotifications() {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within a NotificationProvider');
    }
    return context;
}

export default NotificationContext;