import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const PROVINCES_CACHE_KEY = 'provinces_cache';
const WARDS_CACHE_PREFIX = 'wards_cache_';
const CACHE_TTL = 24 * 60 * 60 * 1000; // 24 hours

function getCachedData(key) {
    try {
        const cached = sessionStorage.getItem(key);
        if (cached) {
            const { data, timestamp } = JSON.parse(cached);
            if (Date.now() - timestamp < CACHE_TTL) {
                return data;
            }
        }
    } catch { }
    return null;
}

function setCachedData(key, data) {
    try {
        sessionStorage.setItem(key, JSON.stringify({ data, timestamp: Date.now() }));
    } catch { }
}

export default function RegisterPage() {
    const { registerUser } = useAuth();
    const navigate = useNavigate();

    const [fullName, setFullName] = useState('');
    const [nationalId, setNationalId] = useState('');
    const [dateOfBirth, setDateOfBirth] = useState('');
    const [email, setEmail] = useState('');
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [passwordConfirmation, setPasswordConfirmation] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [loading, setLoading] = useState(false);

    // Address dropdowns (2-tier: Province -> Ward)
    const [provinces, setProvinces] = useState([]);
    const [wards, setWards] = useState([]);
    const [selectedProvince, setSelectedProvince] = useState('');
    const [selectedWard, setSelectedWard] = useState('');
    const [selectedProvinceName, setSelectedProvinceName] = useState('');
    const [selectedWardName, setSelectedWardName] = useState('');
    const [specificAddress, setSpecificAddress] = useState('');
    const [apiError, setApiError] = useState(false);

    // Fetch provinces on mount with caching
    useEffect(() => {
        const cached = getCachedData(PROVINCES_CACHE_KEY);
        if (cached) {
            setProvinces(cached);
            return;
        }
        fetch('/api/provinces')
            .then(res => res.json())
            .then(data => {
                if (data?.code === 200 && data?.data) {
                    setProvinces(data.data);
                    setCachedData(PROVINCES_CACHE_KEY, data.data);
                }
            })
            .catch(() => {
                setApiError(true);
            });
    }, []);

    // Fetch wards when province changes with caching
    useEffect(() => {
        if (!selectedProvince) {
            setWards([]);
            setSelectedWard('');
            setSelectedWardName('');
            return;
        }
        const cacheKey = WARDS_CACHE_PREFIX + selectedProvince;
        const cached = getCachedData(cacheKey);
        if (cached) {
            setWards(cached);
            return;
        }
        fetch(`/api/provinces/${selectedProvince}/wards`)
            .then(res => res.json())
            .then(data => {
                if (data?.code === 200 && data?.data) {
                    setWards(data.data);
                    setCachedData(cacheKey, data.data);
                }
            })
            .catch(() => { });
        setSelectedWard('');
        setSelectedWardName('');
    }, [selectedProvince]);

    // Validation
    const nationalIdValid = /^[0-9]{12}$/.test(nationalId);
    const emailTouched = email.length > 0;
    const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    const phoneValid = /^(0[0-9]{9}|\+84[0-9]{9})$/.test(phone);
    const dobValid = /^\d{2}\/\d{2}\/\d{4}$/.test(dateOfBirth);
    const has8Chars = password.length >= 8;
    const hasUppercase = /[A-Z]/.test(password);
    const hasNumber = /[0-9]/.test(password);
    const hasSpecialChar = /[^A-Za-z0-9]/.test(password);
    const passwordLong = password.length > 12;
    const allPasswordRulesMet = has8Chars && hasUppercase && hasNumber;
    const passwordMaxOk = password.length <= 128;
    const passwordsMatch = passwordConfirmation.length > 0 && password === passwordConfirmation;

    const formValid = fullName.trim().length > 0
        && nationalIdValid
        && emailValid
        && phoneValid
        && dobValid
        && allPasswordRulesMet
        && passwordMaxOk
        && passwordsMatch;

    const getPasswordStrength = () => {
        if (!password) return { level: '', text: '', color: '', width: '0%' };
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
        setSuccessMessage('');

        if (!formValid) return;

        setLoading(true);
        try {
            const formData = {
                fullName: fullName.trim(),
                nationalId: nationalId.trim(),
                dateOfBirth: dateOfBirth.trim(),
                email: email.toLowerCase().trim(),
                phone: phone.trim(),
                password,
                passwordConfirmation,
                provinceCode: selectedProvince || null,
                provinceName: selectedProvinceName || null,
                wardCode: selectedWard || null,
                wardName: selectedWardName || null,
                specificAddress: specificAddress.trim() || null,
            };
            const result = await registerUser(formData);
            if (result.success && result.message) {
                setSuccessMessage(result.message);
            } else if (result.success) {
                navigate('/');
            }
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'An error occurred during registration.';
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    const CheckIcon = ({ active }) => (
        <span style={{ color: active ? '#2e7d32' : '#ccc', marginRight: 6, fontSize: 14 }}>
            {active ? '✓' : '○'}
        </span>
    );

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Create Account</h1>

                {error && <div style={styles.error}>{error}</div>}
                {apiError && (
                    <div style={styles.warning}>
                        Administrative data service is currently unavailable. Please try again later.
                    </div>
                )}
                {successMessage && (
                    <div style={styles.success}>
                        <p>{successMessage}</p>
                        <Link to="/login" style={styles.loginLink}>Go to Login</Link>
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    {/* Full Name */}
                    <div style={styles.field}>
                        <label htmlFor="fullName">Full Name *</label>
                        <input
                            id="fullName"
                            type="text"
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                            style={styles.input}
                            placeholder="Your full name"
                            autoComplete="name"
                        />
                    </div>

                    {/* National ID */}
                    <div style={styles.field}>
                        <label htmlFor="nationalId">National ID (CCCD) *</label>
                        <input
                            id="nationalId"
                            type="text"
                            value={nationalId}
                            onChange={(e) => {
                                const val = e.target.value.replace(/\D/g, '');
                                if (val.length <= 12) setNationalId(val);
                            }}
                            style={{
                                ...styles.input,
                                borderColor: nationalId.length > 0
                                    ? (nationalIdValid ? '#2e7d32' : '#d32f2f') : '#ddd',
                            }}
                            placeholder="12-digit National ID"
                            autoComplete="off"
                            maxLength={12}
                        />
                        {nationalId.length > 0 && !nationalIdValid && (
                            <span style={styles.inlineError}>Must be exactly 12 digits.</span>
                        )}
                    </div>

                    {/* Date of Birth */}
                    <div style={styles.field}>
                        <label htmlFor="dateOfBirth">Date of Birth *</label>
                        <input
                            id="dateOfBirth"
                            type="text"
                            value={dateOfBirth}
                            onChange={(e) => setDateOfBirth(e.target.value)}
                            style={{
                                ...styles.input,
                                borderColor: dateOfBirth.length > 0
                                    ? (dobValid ? '#2e7d32' : '#d32f2f') : '#ddd',
                            }}
                            placeholder="DD/MM/YYYY"
                            autoComplete="bday"
                        />
                        {dateOfBirth.length > 0 && !dobValid && (
                            <span style={styles.inlineError}>Use format DD/MM/YYYY.</span>
                        )}
                    </div>

                    {/* Email */}
                    <div style={styles.field}>
                        <label htmlFor="email">Email *</label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            style={{
                                ...styles.input,
                                borderColor: emailTouched ? (emailValid ? '#2e7d32' : '#d32f2f') : '#ddd',
                            }}
                            autoComplete="email"
                            placeholder="you@example.com"
                        />
                        {emailTouched && !emailValid && (
                            <span style={styles.inlineError}>Please enter a valid email address.</span>
                        )}
                    </div>

                    {/* Phone */}
                    <div style={styles.field}>
                        <label htmlFor="phone">Phone Number *</label>
                        <input
                            id="phone"
                            type="tel"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            style={{
                                ...styles.input,
                                borderColor: phone.length > 0
                                    ? (phoneValid ? '#2e7d32' : '#d32f2f') : '#ddd',
                            }}
                            placeholder="e.g., 0912345678"
                            autoComplete="tel"
                        />
                        {phone.length > 0 && !phoneValid && (
                            <span style={styles.inlineError}>Must be 10 digits starting with 0 or +84.</span>
                        )}
                    </div>

                    {/* Address - Province */}
                    <div style={styles.field}>
                        <label htmlFor="province">Province/City *</label>
                        <select
                            id="province"
                            value={selectedProvince}
                            onChange={(e) => {
                                const idx = e.target.selectedIndex;
                                setSelectedProvince(e.target.value);
                                setSelectedProvinceName(idx > 0 ? e.target.options[idx].text : '');
                            }}
                            style={styles.input}
                            disabled={apiError}
                        >
                            <option value="">-- Select Province --</option>
                            {provinces.map((p) => (
                                <option key={p.code} value={p.code}>{p.name}</option>
                            ))}
                        </select>
                    </div>

                    {/* Address - Ward (2-tier: directly under province) */}
                    <div style={styles.field}>
                        <label htmlFor="ward">Ward/Commune *</label>
                        <select
                            id="ward"
                            value={selectedWard}
                            disabled={!selectedProvince || apiError}
                            onChange={(e) => {
                                const idx = e.target.selectedIndex;
                                setSelectedWard(e.target.value);
                                setSelectedWardName(idx > 0 ? e.target.options[idx].text : '');
                            }}
                            style={{ ...styles.input, opacity: !selectedProvince ? 0.5 : 1 }}
                        >
                            <option value="">-- Select Ward --</option>
                            {wards.length === 0 && selectedProvince ? (
                                <option value="" disabled>No data available</option>
                            ) : (
                                wards.map((w) => (
                                    <option key={w.code} value={w.code}>{w.name}</option>
                                ))
                            )}
                        </select>
                    </div>

                    {/* Specific Address */}
                    <div style={styles.field}>
                        <label htmlFor="specificAddress">Specific Address</label>
                        <input
                            id="specificAddress"
                            type="text"
                            value={specificAddress}
                            onChange={(e) => setSpecificAddress(e.target.value)}
                            style={styles.input}
                            placeholder="House number, street name"
                            autoComplete="street-address"
                        />
                    </div>

                    {/* Password */}
                    <div style={styles.field}>
                        <label htmlFor="password">Password *</label>
                        <div style={styles.passwordWrapper}>
                            <input
                                id="password"
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                style={styles.input}
                                autoComplete="new-password"
                                placeholder="Min 8 chars, 1 uppercase, 1 number"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                style={styles.toggleButton}
                                tabIndex={-1}
                            >
                                {showPassword ? '🙈' : '👁'}
                            </button>
                        </div>

                        {password.length > 0 && (
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

                        <div style={styles.rules}>
                            <div style={styles.rule}>
                                <CheckIcon active={has8Chars} />
                                At least 8 characters
                            </div>
                            <div style={styles.rule}>
                                <CheckIcon active={password.length <= 128 && password.length > 0} />
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
                        </div>
                    </div>

                    {/* Confirm Password */}
                    <div style={styles.field}>
                        <label htmlFor="confirmPassword">Confirm Password *</label>
                        <div style={styles.passwordWrapper}>
                            <input
                                id="confirmPassword"
                                type={showConfirmPassword ? 'text' : 'password'}
                                value={passwordConfirmation}
                                onChange={(e) => setPasswordConfirmation(e.target.value)}
                                style={{
                                    ...styles.input,
                                    borderColor: passwordConfirmation.length > 0
                                        ? (passwordsMatch ? '#2e7d32' : '#d32f2f') : '#ddd',
                                }}
                                autoComplete="new-password"
                                placeholder="Re-enter your password"
                            />
                            <button
                                type="button"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                style={styles.toggleButton}
                                tabIndex={-1}
                            >
                                {showConfirmPassword ? '🙈' : '👁'}
                            </button>
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
                            opacity: formValid && !loading ? 1 : 0.6,
                            cursor: formValid && !loading ? 'pointer' : 'not-allowed',
                        }}
                        disabled={!formValid || loading || apiError}
                    >
                        {loading ? (
                            <span style={styles.loadingRow}>
                                <span style={styles.spinner} />
                                Creating account...
                            </span>
                        ) : (
                            'Create Account'
                        )}
                    </button>
                </form>

                <p style={styles.footer}>
                    Already have an account? <Link to="/login" style={styles.link}>Sign In</Link>
                </p>
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
        maxWidth: '520px',
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
    warning: {
        backgroundColor: '#fff3e0',
        color: '#e65100',
        padding: '12px',
        borderRadius: '4px',
        marginBottom: '16px',
        fontSize: '14px',
        border: '1px solid #ffcc80',
    },
    success: {
        backgroundColor: '#e8f5e9',
        color: '#2e7d32',
        padding: '16px',
        borderRadius: '4px',
        marginBottom: '16px',
        fontSize: '14px',
        textAlign: 'center',
    },
    inlineError: {
        color: '#d32f2f',
        fontSize: '12px',
        marginTop: '4px',
        display: 'block',
    },
    footer: {
        textAlign: 'center',
        fontSize: '14px',
        color: '#666',
        marginTop: '16px',
    },
    link: {
        color: '#1976d2',
        textDecoration: 'none',
    },
    loginLink: {
        color: '#1976d2',
        textDecoration: 'none',
        fontWeight: 'bold',
        display: 'inline-block',
        marginTop: '8px',
    },
};