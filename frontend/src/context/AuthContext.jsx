import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import apiClient from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    const fetchUser = useCallback(async () => {
        try {
            const response = await apiClient.get('/auth/me');
            if (response.data?.code === 200 && response.data?.data) {
                setUser(response.data.data);
            } else {
                setUser(null);
            }
        } catch {
            setUser(null);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchUser();
    }, [fetchUser]);

    useEffect(() => {
        const handleLogout = () => setUser(null);
        window.addEventListener('auth:logout', handleLogout);
        return () => window.removeEventListener('auth:logout', handleLogout);
    }, []);

    const registerUser = async (formData) => {
        const response = await apiClient.post('/auth/register', formData, { withCredentials: true });

        if (response.data?.code === 200 && response.data?.data) {
            setUser(response.data.data);
            return { success: true };
        }
        return {
            success: true,
            message: response.data?.message || 'Please check your email to verify your account.',
        };
    };

    const login = async (credential, password) => {
        const response = await apiClient.post('/auth/login', {
            credential: credential.trim(),
            password,
        }, { withCredentials: true });

        if (response.data?.code === 200 && response.data?.data) {
            const userData = response.data.data;
            setUser(userData);
            return {
                success: true,
                user: userData,
                role: userData.role || userData?.user?.role,
            };
        }
        return { success: false, message: response.data?.message };
    };

    const logout = async () => {
        try {
            await apiClient.post('/auth/logout', {}, { withCredentials: true });
        } catch {
            // ignore
        }
        setUser(null);
    };

    const logoutAll = async () => {
        try {
            await apiClient.post('/auth/logout-all', {}, { withCredentials: true });
        } catch {
            // ignore
        }
        setUser(null);
    };

    const requestPasswordReset = async (email) => {
        const response = await apiClient.post('/auth/password-reset/request', {
            email: email.toLowerCase().trim(),
        });
        return { success: response.data?.code === 200, message: response.data?.message };
    };

    const confirmPasswordReset = async (resetToken, newPassword) => {
        const response = await apiClient.post('/auth/password-reset/confirm', {
            resetToken,
            newPassword,
        });
        return { success: response.data?.code === 200, message: response.data?.message };
    };

    const changePassword = async (currentPassword, newPassword, confirmNewPassword) => {
        const response = await apiClient.post('/auth/change-password', {
            currentPassword,
            newPassword,
            confirmNewPassword,
        });
        return { success: response.data?.code === 200, message: response.data?.message };
    };

    const fetchProfile = async () => {
        const response = await apiClient.get('/users/profile');
        if (response.data?.code === 200 && response.data?.data) {
            return response.data.data;
        }
        return null;
    };

    const updateProfile = async (data) => {
        const response = await apiClient.put('/users/profile', data);
        if (response.data?.code === 200 && response.data?.data) {
            return { success: true, data: response.data.data };
        }
        return { success: false, message: response.data?.message };
    };

    return (
        <AuthContext.Provider value={{
            user,
            loading,
            login,
            registerUser,
            logout,
            logoutAll,
            requestPasswordReset,
            confirmPasswordReset,
            changePassword,
            fetchUser,
            fetchProfile,
            updateProfile,
        }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}

export default AuthContext;