import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ChangePasswordPage() {
    const { changePassword, user, loading: authLoading } = useAuth();
    const navigate = useNavigate();

    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmNewPassword, setConfirmNewPassword] = useState('');

    const [showCurrent, setShowCurrent] = useState(false);
    const [showNew, setShowNew] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);

    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [submitting, setSubmitting] = useState(false);

    // Auth guard
    if (!authLoading && !user) {
        navigate('/login');
        return null;
    }

    // Password validation rules
    const has8Chars = newPassword.length >= 8;
    const max128Chars = newPassword.length <= 128;
    const hasUppercase = /[A-Z]/.test(newPassword);
    const hasNumber = /[0-9]/.test(newPassword);
    const hasSpecialChar = /[^A-Za-z0-9]/.test(newPassword);
    const passwordLong = newPassword.length > 12;
    const allPasswordRulesMet = has8Chars && hasUppercase && hasNumber && max128Chars;

    // Confirm match
    const passwordsMatch = confirmNewPassword.length > 0 && newPassword === confirmNewPassword;
    const notSameAsCurrent = newPassword.length > 0 && newPassword !== currentPassword;

    const formValid = currentPassword.length > 0 && allPasswordRulesMet && passwordsMatch && notSameAsCurrent && max128Chars;

    // Password strength
    const getPasswordStrength = () => {
        if (!newPassword) return { level: '', text: '', color: '', width: '0%' };
        let score = 0;
        if (has8Chars) score++;
        if (hasUppercase) score++;
        if (hasNumber) score++;
        if (hasSpecialChar || passwordLong) score++;

        if (score <= 2) return { level: 'Weak', text: 'Weak', color: '#d32f2f', width: '25%' };
        if (score === 3) return { level: 'Medium', text: 'Medium', color: '#f57c00', width: '60%' };
        return { level: 'Strong', text: 'Strong', color: '#2e7d32', width: '100%' };
    };

    const strength = getPasswordStrength();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (!formValid) return;

        setSubmitting(true);
        try {
            const result = await changePassword(currentPassword, newPassword, confirmNewPassword);
            if (result.success) {
                setSuccess('Password changed successfully. You will be redirected to login.');
                setCurrentPassword('');
                setNewPassword('');
                setConfirmNewPassword('');
                setTimeout(() => {
                    navigate('/login');
                }, 2000);
            } else {
                setError(result.message || 'Failed to change password.');
            }
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'An error occurred.';
            setError(msg);
        } finally {
            setSubmitting(false);
        }
    };

    const CheckIcon = ({ active }) => (
        <span style={{ color: active ? '#2e7d32' : '#ccc', marginRight: 6, fontSize: 14 }}>
            {active ? '✓' : '○'}
        </span>
    );

    const EyeButton = ({ show, onClick }) => (
        <button
            type="button"
            onClick={onClick}
            style={styles.toggleButton}
            tabIndex={-1}
        >
            {show ? '🙈' : '👁'}
        </button>
    );

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Change Password</h1>

                {error && <div style={styles.error}>{error}</div>}
                {success && <div style={styles.success}>{success}</div>}

                <form onSubmit={handleSubmit}>
                    {/* Current Password */}
                    <div style={styles.field}>
                        <label htmlFor="currentPassword">Current Password</label>
                        <div style={styles.passwordWrapper}>
                            <input
                                id="currentPassword"
                                type={showCurrent ? 'text' : 'password'}
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                                style={styles.input}
                                autoComplete="current-password"
                                placeholder="Enter current password"
                            />
                            <EyeButton show={showCurrent} onClick={() => setShowCurrent(!showCurrent)} />
                        </div>
                    </div>

                    {/* New Password */}
                    <div style={styles.field}>
                        <label htmlFor="newPassword">New Password</label>
                        <div style={styles.passwordWrapper}>
                            <input
                                id="newPassword"
                                type={showNew ? 'text' : 'password'}
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                style={styles.input}
                                autoComplete="new-password"
                                placeholder="Min 8 chars, 1 uppercase, 1 number"
                            />
                            <EyeButton show={showNew} onClick={() => setShowNew(!showNew)} />
                        </div>

                        {/* Password strength meter */}
                        {newPassword.length > 0 && (
                            <div style={styles.strengthMeter}>
                                <div style={styles.strengthBarBg}>
                                    <div style={{
                                        ...styles.strengthBarFill,
                                        width: strength.width,
                                        backgroundColor: strength.color,
                                    }} />
                                </div>
                                <span style={{ ...styles.strengthText, color: strength.color }}>
                                    {strength.text}
                                </span>
                            </div>
                        )}

                        {/* Rule checks */}
                        <div style={styles.rules}>
                            <div style={styles.rule}>
                                <CheckIcon active={has8Chars} />
                                At least 8 characters
                            </div>
                            <div style={styles.rule}>
                                <CheckIcon active={max128Chars} />
                                Max 128 characters
                            </div>
                            <div style={styles.rule}>
                                <CheckIcon active={hasUppercase} />
                                At least 1 uppercase letter
                            </div>
                            <div style={styles.rule}>
                                <CheckIcon active={hasNumber} />
                                At least 1 number
                            </div>
                            <div style={styles.rule}>
                                <CheckIcon active={notSameAsCurrent} />
                                Different from current password
                            </div>
                        </div>
                    </div>

                    {/* Confirm New Password */}
                    <div style={styles.field}>
                        <label htmlFor="confirmNewPassword">Confirm New Password</label>
                        <div style={styles.passwordWrapper}>
                            <input
                                id="confirmNewPassword"
                                type={showConfirm ? 'text' : 'password'}
                                value={confirmNewPassword}
                                onChange={(e) => setConfirmNewPassword(e.target.value)}
                                style={{
                                    ...styles.input,
                                    borderColor: confirmNewPassword.length > 0
                                        ? (passwordsMatch ? '#2e7d32' : '#d32f2f') : '#ddd',
                                }}
                                autoComplete="new-password"
                                placeholder="Re-enter new password"
                            />
                            <EyeButton show={showConfirm} onClick={() => setShowConfirm(!showConfirm)} />
                        </div>
                        <div style={styles.rule}>
                            <CheckIcon active={passwordsMatch} />
                            Passwords match
                        </div>
                    </div>

                    <button
                        type="submit"
                        style={{
                            ...styles.button,
                            opacity: formValid && !submitting ? 1 : 0.6,
                            cursor: formValid && !submitting ? 'pointer' : 'not-allowed',
                        }}
                        disabled={!formValid || submitting}
                    >
                        {submitting ? (
                            <span style={styles.loadingRow}>
                                <span style={styles.spinner} />
                                Changing password...
                            </span>
                        ) : (
                            'Change Password'
                        )}
                    </button>
                </form>

                <div style={styles.links}>
                    <Link to="/profile" style={styles.link}>Back to Profile</Link>
                </div>
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
        padding: '20px',
    },
    card: {
        backgroundColor: '#fff',
        padding: '32px',
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        width: '100%',
        maxWidth: '480px',
    },
    title: {
        margin: '0 0 24px 0',
        fontSize: '24px',
        textAlign: 'center',
    },
    field: {
        marginBottom: '16px',
    },
    label: {
        display: 'block',
        marginBottom: '4px',
        fontSize: '14px',
        fontWeight: '500',
        color: '#333',
    },
    input: {
        width: '100%',
        padding: '10px 12px',
        fontSize: '14px',
        border: '1px solid #ddd',
        borderRadius: '4px',
        boxSizing: 'border-box',
    },
    passwordWrapper: {
        position: 'relative',
    },
    toggleButton: {
        position: 'absolute',
        right: '8px',
        top: '50%',
        transform: 'translateY(-50%)',
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        fontSize: '18px',
        padding: '4px',
        lineHeight: 1,
    },
    strengthMeter: {
        marginTop: '8px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
    },
    strengthBarBg: {
        flex: 1,
        height: '6px',
        backgroundColor: '#e0e0e0',
        borderRadius: '3px',
        overflow: 'hidden',
    },
    strengthBarFill: {
        height: '100%',
        borderRadius: '3px',
        transition: 'width 0.3s ease, background-color 0.3s ease',
    },
    strengthText: {
        fontSize: '12px',
        fontWeight: 'bold',
        minWidth: '50px',
        textAlign: 'right',
    },
    rules: {
        marginTop: '8px',
    },
    rule: {
        display: 'flex',
        alignItems: 'center',
        fontSize: '13px',
        color: '#555',
        padding: '2px 0',
    },
    button: {
        width: '100%',
        padding: '12px',
        backgroundColor: '#1976d2',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        marginBottom: '8px',
        marginTop: '8px',
    },
    loadingRow: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '10px',
    },
    spinner: {
        width: '18px',
        height: '18px',
        border: '3px solid rgba(255,255,255,0.3)',
        borderTop: '3px solid #fff',
        borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
        display: 'inline-block',
    },
    error: {
        backgroundColor: '#fdecea',
        color: '#b71c1c',
        padding: '12px',
        borderRadius: '4px',
        marginBottom: '16px',
        fontSize: '14px',
    },
    success: {
        backgroundColor: '#e8f5e9',
        color: '#2e7d32',
        padding: '12px',
        borderRadius: '4px',
        marginBottom: '16px',
        fontSize: '14px',
    },
    links: {
        textAlign: 'center',
        marginTop: '12px',
    },
    link: {
        color: '#1976d2',
        textDecoration: 'none',
        fontSize: '14px',
    },
};