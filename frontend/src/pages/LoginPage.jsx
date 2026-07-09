import { useState, useRef, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../api/client';

export default function LoginPage() {
    const { login, requestPasswordReset, confirmPasswordReset } = useAuth();
    const navigate = useNavigate();

    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    // Captcha state
    const [captchaId, setCaptchaId] = useState('');
    const [captchaCode, setCaptchaCode] = useState('');
    const [captchaSrc, setCaptchaSrc] = useState('');
    const captchaTimestampRef = useRef(0);

    // Forgot password state
    const [showForgotPassword, setShowForgotPassword] = useState(false);
    const [resetEmail, setResetEmail] = useState('');
    const [resetSent, setResetSent] = useState(false);
    const [resetToken, setResetToken] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [resetSuccess, setResetSuccess] = useState(false);

    // Captcha base URL
    const CAPTCHA_BASE = '/api/auth/captcha';

    const loadCaptcha = () => {
        const ts = Date.now();
        captchaTimestampRef.current = ts;
        setCaptchaSrc(`${CAPTCHA_BASE}?_t=${ts}`);
        setCaptchaCode('');
    };

    useEffect(() => {
        loadCaptcha();
    }, []);

    const handleCaptchaImageLoad = (e) => {
        const captchaIdHeader = e.target?.getAttribute('data-captcha-id');
        // We'll get captchaId from response headers via a separate fetch
        setCaptchaId(captchaIdHeader || '');
    };

    // Better approach: fetch captcha via API and get Captcha-Id header
    const fetchCaptcha = async () => {
        try {
            const response = await fetch(CAPTCHA_BASE + '?_t=' + Date.now());
            const captchaIdHeader = response.headers.get('Captcha-Id');
            if (captchaIdHeader) {
                setCaptchaId(captchaIdHeader);
            }
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            setCaptchaSrc(url);
        } catch (err) {
            console.error('Failed to load captcha:', err);
        }
    };

    // Override initial load with proper fetch
    useEffect(() => {
        fetchCaptcha();
    }, []);

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        if (!loginId || !password) {
            setError('Email/Phone and password are required.');
            return;
        }
        if (!captchaCode) {
            setError('Please enter the captcha code.');
            return;
        }
        if (!captchaId) {
            setError('Captcha not loaded. Please refresh.');
            return;
        }
        setLoading(true);
        try {
            const result = await login(loginId, password, captchaId, captchaCode);
            if (result.success) {
                navigate('/');
            } else {
                setError(result.message || 'Login failed. Please try again.');
                fetchCaptcha(); // Refresh captcha on failure
            }
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'An error occurred.';
            setError(msg);
            fetchCaptcha(); // Refresh captcha on error
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
                            <label htmlFor="login">Email or Phone Number</label>
                            <input
                                id="login"
                                type="text"
                                value={loginId}
                                onChange={(e) => setLoginId(e.target.value)}
                                style={styles.input}
                                autoComplete="username"
                                placeholder="you@example.com or 0912345678"
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
                        {/* Captcha */}
                        <div style={styles.field}>
                            <label>Captcha Verification</label>
                            <div style={styles.captchaRow}>
                                {captchaSrc && (
                                    <img
                                        src={captchaSrc}
                                        alt="Captcha"
                                        style={styles.captchaImage}
                                        onClick={fetchCaptcha}
                                    />
                                )}
                                <button
                                    type="button"
                                    style={styles.refreshCaptchaBtn}
                                    onClick={fetchCaptcha}
                                    title="Refresh captcha"
                                >
                                    ↻
                                </button>
                            </div>
                            <input
                                type="text"
                                value={captchaCode}
                                onChange={(e) => setCaptchaCode(e.target.value)}
                                style={styles.input}
                                placeholder="Enter captcha code"
                                autoComplete="off"
                                maxLength={4}
                            />
                            <span style={styles.captchaHint}>
                                Click the image to refresh captcha
                            </span>
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
                            A reset token has been sent to your email. Enter it below with your new password.
                        </p>
                        <div style={styles.field}>
                            <label htmlFor="resetToken">Reset Token</label>
                            <input
                                id="resetToken"
                                type="text"
                                value={resetToken}
                                onChange={(e) => setResetToken(e.target.value)}
                                style={styles.input}
                                placeholder="Enter reset token from email"
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
    captchaRow: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        marginBottom: '8px',
    },
    captchaImage: {
        height: '48px',
        borderRadius: '4px',
        cursor: 'pointer',
        border: '1px solid #ddd',
        flex: 1,
        objectFit: 'contain',
    },
    refreshCaptchaBtn: {
        padding: '8px 12px',
        fontSize: '18px',
        backgroundColor: '#f0f0f0',
        border: '1px solid #ddd',
        borderRadius: '4px',
        cursor: 'pointer',
    },
    captchaHint: {
        fontSize: '11px',
        color: '#999',
        marginTop: '4px',
        display: 'block',
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