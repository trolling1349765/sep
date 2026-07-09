import { useState, useEffect, useRef } from 'react';
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

export default function UpdateProfilePage() {
    const { fetchProfile, updateProfile, user, loading: authLoading } = useAuth();
    const navigate = useNavigate();
    const originalRef = useRef(null);

    const [name, setName] = useState('');
    const [nationalId, setNationalId] = useState('');
    const [dob, setDob] = useState('');
    const [email, setEmail] = useState('');
    const [gender, setGender] = useState('');
    const [phone, setPhone] = useState('');
    const [address, setAddress] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [loadingProfile, setLoadingProfile] = useState(true);
    const [apiError, setApiError] = useState(false);
    const [profileRole, setProfileRole] = useState('');

    // 2-tier: Province -> Ward
    const [provinces, setProvinces] = useState([]);
    const [wards, setWards] = useState([]);
    const [selectedProvince, setSelectedProvince] = useState('');
    const [selectedWard, setSelectedWard] = useState('');
    const [selectedProvinceName, setSelectedProvinceName] = useState('');
    const [selectedWardName, setSelectedWardName] = useState('');
    const [specificAddress, setSpecificAddress] = useState('');

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

    useEffect(() => {
        if (!authLoading && !user) {
            navigate('/login');
            return;
        }
        if (!authLoading && user) {
            loadProfile();
        }
    }, [authLoading, user]);

    const loadProfile = async () => {
        try {
            const data = await fetchProfile();
            if (data) {
                setProfileRole(data.role || '');
                setName(data.name || '');
                setNationalId(data.nationalId || '');
                setDob(data.dob || '');
                setEmail(data.email || '');
                setGender(data.gender !== undefined && data.gender !== null ? String(data.gender) : '');
                setPhone(data.phone || '');
                setAddress(data.address || data.fullAddress || '');
                setSpecificAddress(data.specificAddress || '');
                setSelectedProvince(data.provinceCode || '');
                setSelectedProvinceName(data.provinceName || '');
                // Pre-fill: fetch wards for the saved province
                if (data.provinceCode) {
                    const cacheKey = WARDS_CACHE_PREFIX + data.provinceCode;
                    const cached = getCachedData(cacheKey);
                    if (cached) {
                        setWards(cached);
                        setSelectedWard(data.wardCode || '');
                        setSelectedWardName(data.wardName || '');
                    } else {
                        fetch(`/api/provinces/${data.provinceCode}/wards`)
                            .then(res => res.json())
                            .then(resData => {
                                if (resData?.code === 200 && resData?.data) {
                                    setWards(resData.data);
                                    setCachedData(cacheKey, resData.data);
                                    setSelectedWard(data.wardCode || '');
                                    setSelectedWardName(data.wardName || '');
                                }
                            })
                            .catch(() => { });
                    }
                }
                originalRef.current = {
                    name: data.name || '',
                    nationalId: data.nationalId || '',
                    dob: data.dob || '',
                    email: data.email || '',
                    gender: data.gender !== undefined && data.gender !== null ? String(data.gender) : '',
                    phone: data.phone || '',
                    address: data.address || data.fullAddress || '',
                    provinceCode: data.provinceCode || '',
                    wardCode: data.wardCode || '',
                    specificAddress: data.specificAddress || '',
                };
            }
        } catch {
            // handled by interceptor
        } finally {
            setLoadingProfile(false);
        }
    };

    // Validate phone: 10 digits starting with 0 or +84
    const phoneRegex = /^(0[0-9]{9}|\+84[0-9]{9})$/;
    const phoneValid = phone.length === 0 || phoneRegex.test(phone);

    // Validate email format (basic)
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const emailValid = email.length === 0 || emailRegex.test(email);

    // Name validation
    const nameValid = name.trim().length > 0;

    // Dirty checking
    const isCitizen = profileRole === 'CONG_DAN';
    const original = originalRef.current || {};
    const isDirty = isCitizen && (
        name !== original.name
        || nationalId !== original.nationalId
        || dob !== original.dob
        || email !== original.email
        || gender !== original.gender
        || phone !== original.phone
        || selectedProvince !== original.provinceCode
        || selectedWard !== original.wardCode
        || specificAddress !== original.specificAddress
    );

    const formValid = isCitizen && nameValid && phoneValid && emailValid && isDirty;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (!formValid) return;

        setSubmitting(true);
        try {
            const result = await updateProfile({
                name: name.trim() || null,
                nationalId: nationalId.trim() || null,
                dob: dob || null,
                email: email.trim().toLowerCase() || null,
                gender: gender === '' ? null : gender === 'true',
                phone: phone.trim() || null,
                provinceCode: selectedProvince || null,
                provinceName: selectedProvinceName || null,
                wardCode: selectedWard || null,
                wardName: selectedWardName || null,
                specificAddress: specificAddress.trim() || null,
            });
            if (result.success) {
                setSuccess('Profile updated successfully.');
                originalRef.current = {
                    name: name.trim(),
                    nationalId: nationalId.trim(),
                    dob: dob,
                    email: email.trim().toLowerCase(),
                    gender: gender,
                    phone: phone.trim() || '',
                    address: address.trim() || '',
                    provinceCode: selectedProvince || '',
                    wardCode: selectedWard || '',
                    specificAddress: specificAddress.trim() || '',
                };
            } else {
                setError(result.message || 'Failed to update profile.');
            }
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'An error occurred.';
            setError(msg);
        } finally {
            setSubmitting(false);
        }
    };

    if (loadingProfile || authLoading) {
        return (
            <div style={styles.container}>
                <div style={styles.card}>
                    <p style={styles.loadingText}>Loading profile...</p>
                </div>
            </div>
        );
    }

    // If not a citizen, show restricted message
    if (!isCitizen) {
        return (
            <div style={styles.container}>
                <div style={styles.card}>
                    <h1 style={styles.title}>Update Profile</h1>
                    <div style={styles.warning}>
                        You do not have permission to edit profile. Only users with role "Công dân" can update their profile information.
                    </div>
                    <div style={styles.links}>
                        <Link to="/profile" style={styles.link}>Back to Profile</Link>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Update Profile</h1>

                {error && <div style={styles.error}>{error}</div>}
                {apiError && (
                    <div style={styles.warning}>
                        Administrative data service is currently unavailable. Please try again later.
                    </div>
                )}
                {success && <div style={styles.success}>{success}</div>}

                <form onSubmit={handleSubmit}>
                    {/* Name */}
                    <div style={styles.field}>
                        <label htmlFor="name" style={styles.label}>Full Name</label>
                        <input
                            id="name"
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            style={{
                                ...styles.input,
                                borderColor: name.length > 0 && !nameValid ? '#d32f2f' : '#ddd',
                            }}
                            placeholder="Enter your full name"
                            autoComplete="name"
                        />
                        {name.length > 0 && !nameValid && (
                            <span style={styles.inlineError}>Name cannot be empty.</span>
                        )}
                    </div>

                    {/* National ID */}
                    <div style={styles.field}>
                        <label htmlFor="nationalId" style={styles.label}>National ID</label>
                        <input
                            id="nationalId"
                            type="text"
                            value={nationalId}
                            onChange={(e) => setNationalId(e.target.value)}
                            style={styles.input}
                            placeholder="Enter your national ID"
                            autoComplete="off"
                        />
                    </div>

                    {/* Date of Birth */}
                    <div style={styles.field}>
                        <label htmlFor="dob" style={styles.label}>Date of Birth</label>
                        <input
                            id="dob"
                            type="date"
                            value={dob}
                            onChange={(e) => setDob(e.target.value)}
                            style={styles.input}
                        />
                    </div>

                    {/* Email */}
                    <div style={styles.field}>
                        <label htmlFor="email" style={styles.label}>Email</label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            style={{
                                ...styles.input,
                                borderColor: email.length > 0 && !emailValid ? '#d32f2f' : '#ddd',
                            }}
                            placeholder="Enter your email"
                            autoComplete="email"
                        />
                        {email.length > 0 && !emailValid && (
                            <span style={styles.inlineError}>Please enter a valid email address.</span>
                        )}
                    </div>

                    {/* Gender */}
                    <div style={styles.field}>
                        <label htmlFor="gender" style={styles.label}>Gender</label>
                        <select
                            id="gender"
                            value={gender}
                            onChange={(e) => setGender(e.target.value)}
                            style={styles.input}
                        >
                            <option value="">-- Select Gender --</option>
                            <option value="false">Female</option>
                            <option value="true">Male</option>
                        </select>
                    </div>

                    {/* Phone */}
                    <div style={styles.field}>
                        <label htmlFor="phone" style={styles.label}>Phone Number</label>
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
                            placeholder="e.g., 0912345678 or +84912345678"
                            autoComplete="tel"
                        />
                        {phone.length > 0 && !phoneValid && (
                            <span style={styles.inlineError}>Phone must be 10 digits starting with 0 or +84.</span>
                        )}
                    </div>

                    {/* Address - Province */}
                    <div style={styles.field}>
                        <label htmlFor="province" style={styles.label}>Province/City</label>
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

                    {/* Address - Ward (2-tier) */}
                    <div style={styles.field}>
                        <label htmlFor="ward" style={styles.label}>Ward/Commune</label>
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
                        <label htmlFor="specificAddress" style={styles.label}>Specific Address</label>
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

                    <button
                        type="submit"
                        style={{
                            ...styles.button,
                            opacity: formValid && !submitting ? 1 : 0.6,
                            cursor: formValid && !submitting ? 'pointer' : 'not-allowed',
                        }}
                        disabled={!formValid || submitting || apiError}
                    >
                        {submitting ? (
                            <span style={styles.loadingRow}>
                                <span style={styles.spinner} />
                                Saving...
                            </span>
                        ) : (
                            'Save Changes'
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
    inputDisabled: {
        backgroundColor: '#f5f5f5',
        color: '#999',
        cursor: 'not-allowed',
    },
    helpText: {
        fontSize: '12px',
        color: '#888',
        marginTop: '4px',
        display: 'block',
    },
    inlineError: {
        color: '#d32f2f',
        fontSize: '12px',
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
    loadingText: {
        textAlign: 'center',
        color: '#666',
        fontSize: '16px',
    },
};