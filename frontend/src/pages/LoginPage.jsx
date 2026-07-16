import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
    const { login, requestPasswordReset, confirmPasswordReset } = useAuth();
    const navigate = useNavigate();

    const [credential, setCredential] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const [showForgotPassword, setShowForgotPassword] = useState(false);
    const [resetEmail, setResetEmail] = useState('');
    const [resetSent, setResetSent] = useState(false);
    const [resetToken, setResetToken] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [resetSuccess, setResetSuccess] = useState(false);

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        if (!credential || !password) {
            setError('Email or phone number and password are required.');
            return;
        }
        const trimmed = credential.trim();
        const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed);
        const isPhone = /^0\d{9}$/.test(trimmed);
        if (!isEmail && !isPhone) {
            setError('Please enter a valid email or a 10-digit phone number starting with 0.');
            return;
        }
        setLoading(true);
        try {
            const result = await login(credential, password);
            if (result.success) {
                const userRole = result.role || result.user?.role || result.user?.data?.role;
                if (userRole === 'Reception') {
                    navigate('/application/receipt');
                } else {
                    navigate('/');
                }
            } else {
                setError(result.message || 'Đăng nhập thất bại. Xin vui lòng thử lại sau.');
            }
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'An error occurred.';
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    const handleRequestReset = async (e) => {
        e.preventDefault();
        if (!resetEmail) {
            setError('Please enter your email.');
            return;
        }
        setLoading(true);
        try {
            await requestPasswordReset(resetEmail);
            setResetSent(true);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to request password reset.');
        } finally {
            setLoading(false);
        }
    };

    const handleConfirmReset = async (e) => {
        e.preventDefault();
        if (!resetToken || !newPassword) {
            setError('Reset token and new password are required.');
            return;
        }
        setLoading(true);
        try {
            const result = await confirmPasswordReset(resetToken, newPassword);
            if (result.success) {
                setResetSuccess(true);
            } else {
                setError(result.message || 'Password reset failed.');
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Password reset failed.');
        } finally {
            setLoading(false);
        }
    };

    const toggleForgotPassword = () => {
        setShowForgotPassword(!showForgotPassword);
        setError('');
        setResetSent(false);
        setResetToken('');
        setNewPassword('');
        setResetSuccess(false);
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Login</h1>

                {error && <div style={styles.error}>{error}</div>}

                {!showForgotPassword ? (
                    <form onSubmit={handleLogin}>
                        <div style={styles.field}>
                            <label htmlFor="credential">Email or Phone Number</label>
                            <input
                                id="credential"
                                type="text"
                                value={credential}
                                onChange={(e) => setCredential(e.target.value)}
                                style={styles.input}
                                autoComplete="username"
                                placeholder="Enter your email or phone number"
                            />
                        </div>
                        <div style={styles.field}>
                            <label htmlFor="password">Password</label>
                            <input
                                id="password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                style={styles.input}
                                autoComplete="current-password"
                                placeholder="Enter your password"
                            />
                        </div>
                        <button type="submit" style={styles.button} disabled={loading}>
                            {loading ? 'Signing in...' : 'Sign In'}
                        </button>
                        <button
                            type="button"
                            style={styles.linkButton}
                            onClick={toggleForgotPassword}
                        >
                            Forgot password?
                        </button>
                        <p style={styles.footer}>
                            Don't have an account?{' '}
                            <Link to="/register" style={styles.registerLink}>Create one</Link>
                        </p>
                    </form>
                ) : resetSuccess ? (
                    <div>
                        <p style={styles.successText}>
                            Password reset successfully! You can now sign in with your new password.
                        </p>
                        <button
                            type="button"
                            style={styles.button}
                            onClick={toggleForgotPassword}
                        >
                            Back to Login
                        </button>
                    </div>
                ) : !resetSent ? (
                    <form onSubmit={handleRequestReset}>
                        <p style={styles.helpText}>
                            Enter your email to receive a password reset token.
                        </p>
                        <div style={styles.field}>
                            <label htmlFor="resetEmail">Email</label>
                            <input
                                id="resetEmail"
                                type="email"
                                value={resetEmail}
                                onChange={(e) => setResetEmail(e.target.value)}
                                style={styles.input}
                                autoComplete="email"
                                placeholder="you@example.com"
                            />
                        </div>
                        <button type="submit" style={styles.button} disabled={loading}>
                            {loading ? 'Sending...' : 'Send Reset Token'}
                        </button>
                        <button
                            type="button"
                            style={styles.linkButton}
                            onClick={toggleForgotPassword}
                        >
                            Back to Login
                        </button>
                    </form>
                ) : (
                    <form onSubmit={handleConfirmReset}>
                        <p style={styles.helpText}>
                            Một mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra email và nhập mã OTP cùng mật khẩu mới bên dưới.
                        </p>
                        <div style={styles.field}>
                            <label htmlFor="resetToken">Reset Token</label>
                            <input
                                id="resetToken"
                                type="text"
                                value={resetToken}
                                onChange={(e) => setResetToken(e.target.value)}
                                style={styles.input}
                                placeholder="Enter reset token"
                            />
                        </div>
                        <div style={styles.field}>
                            <label htmlFor="newPassword">New Password</label>
                            <input
                                id="newPassword"
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                style={styles.input}
                                autoComplete="new-password"
                                placeholder="Min 8 chars, 1 uppercase, 1 number"
                            />
                        </div>
                        <button type="submit" style={styles.button} disabled={loading}>
                            {loading ? 'Resetting...' : 'Reset Password'}
                        </button>
                        <button
                            type="button"
                            style={styles.linkButton}
                            onClick={toggleForgotPassword}
                        >
                            Back to Login
                        </button>
                    </form>
                )}
            </div>
        </div>
    );
}

const styles = {
    container: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        backgroundColor: '#f5f5f5',
    },
    card: {
        backgroundColor: '#fff',
        padding: '32px',
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        width: '100%',
        maxWidth: '400px',
    },
    title: {
        margin: '0 0 24px 0',
        fontSize: '24px',
        textAlign: 'center',
    },
    field: {
        marginBottom: '16px',
    },
    input: {
        width: '100%',
        padding: '10px 12px',
        fontSize: '14px',
        border: '1px solid #ddd',
        borderRadius: '4px',
        boxSizing: 'border-box',
    },
    button: {
        width: '100%',
        padding: '12px',
        backgroundColor: '#1976d2',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        cursor: 'pointer',
        marginBottom: '8px',
    },
    linkButton: {
        width: '100%',
        padding: '8px',
        backgroundColor: 'transparent',
        color: '#1976d2',
        border: 'none',
        borderRadius: '4px',
        fontSize: '14px',
        cursor: 'pointer',
    },
    error: {
        backgroundColor: '#fdecea',
        color: '#b71c1c',
        padding: '12px',
        borderRadius: '4px',
        marginBottom: '16px',
        fontSize: '14px',
    },
    helpText: {
        fontSize: '14px',
        color: '#666',
        marginBottom: '16px',
    },
    successText: {
        color: '#2e7d32',
        fontSize: '14px',
        marginBottom: '16px',
    },
    footer: {
        textAlign: 'center',
        fontSize: '14px',
        color: '#666',
        marginTop: '12px',
    },
    registerLink: {
        color: '#1976d2',
        textDecoration: 'none',
        fontWeight: 'bold',
    },
};