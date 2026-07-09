import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
    const { fetchProfile, user, loading: authLoading } = useAuth();
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);

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
            setProfile(data);
        } catch {
            // Error handled by interceptor (redirect to login)
        } finally {
            setLoading(false);
        }
    };

    if (loading || authLoading) {
        return (
            <div style={styles.container}>
                <div style={styles.card}>
                    <SkeletonLoader />
                </div>
            </div>
        );
    }

    if (!profile) {
        return (
            <div style={styles.container}>
                <div style={styles.card}>
                    <p style={styles.errorText}>Unable to load profile.</p>
                </div>
            </div>
        );
    }

    const avatarLetter = profile.name ? profile.name.charAt(0).toUpperCase() : '?';

    const statusColors = {
        ACTIVE: '#2e7d32',
        VERIFIED: '#1565c0',
        BANNED: '#b71c1c',
        INACTIVE: '#757575',
    };

    const formatDob = (dob) => {
        if (!dob) return 'Not set';
        const date = new Date(dob);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        return `${day}/${month}/${year}`;
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                {/* Header with avatar */}
                <div style={styles.header}>
                    <div style={styles.avatar}>
                        {profile.avatarUrl ? (
                            <img src={profile.avatarUrl} alt="Avatar" style={styles.avatarImg} />
                        ) : (
                            <span style={styles.avatarText}>{avatarLetter}</span>
                        )}
                    </div>
                    <h1 style={styles.name}>{profile.name || 'N/A'}</h1>
                    <span style={{
                        ...styles.statusBadge,
                        backgroundColor: statusColors[profile.status] || '#757575',
                    }}>
                        {profile.status || 'ACTIVE'}
                    </span>
                </div>

                {/* Information fields */}
                <div style={styles.infoSection}>
                    <div style={styles.infoRow}>
                        <span style={styles.label}>National ID</span>
                        <span style={styles.value}>
                            {profile.nationalId || 'N/A'}
                            {profile.nationalId && (
                                <span style={{
                                    ...styles.verifiedBadge,
                                    backgroundColor: profile.nationalIdVerified ? '#2e7d32' : '#f57c00',
                                }}>
                                    {profile.nationalIdVerified ? 'Verified via VNeID' : 'Unverified'}
                                </span>
                            )}
                        </span>
                    </div>
                    <div style={styles.infoRow}>
                        <span style={styles.label}>Date of Birth</span>
                        <span style={styles.value}>{formatDob(profile.dob)}</span>
                    </div>
                    <div style={styles.infoRow}>
                        <span style={styles.label}>Email</span>
                        <span style={styles.value}>{profile.email || 'N/A'}</span>
                    </div>
                    <div style={styles.infoRow}>
                        <span style={styles.label}>Phone</span>
                        <span style={styles.value}>{profile.phone || 'Not set'}</span>
                    </div>
                    <div style={styles.infoRow}>
                        <span style={styles.label}>Address</span>
                        <span style={styles.value}>{profile.fullAddress || 'Not set'}</span>
                    </div>
                    <div style={styles.infoRow}>
                        <span style={styles.label}>Role</span>
                        <span style={styles.value}>{profile.role || 'User'}</span>
                    </div>
                </div>

                {/* Navigation buttons */}
                <div style={styles.actions}>
                    {profile.role === 'CONG_DAN' && (
                        <button
                            onClick={() => navigate('/profile/update')}
                            style={styles.primaryButton}
                        >
                            Update Profile
                        </button>
                    )}
                    <button
                        onClick={() => navigate('/profile/change-password')}
                        style={styles.secondaryButton}
                    >
                        Change Password
                    </button>
                </div>

                <Link to="/home" style={styles.backLink}>Back to Home</Link>
            </div>
        </div>
    );
}

function SkeletonLoader() {
    return (
        <div style={styles.skeletonContainer}>
            <div style={{ ...styles.skeleton, ...styles.skeletonAvatar }} />
            <div style={{ ...styles.skeleton, ...styles.skeletonText, width: '60%' }} />
            <div style={{ ...styles.skeleton, ...styles.skeletonBadge }} />
            <div style={{ marginTop: '24px' }}>
                {[1, 2, 3, 4, 5, 6].map((i) => (
                    <div key={i} style={styles.skeletonRow}>
                        <div style={{ ...styles.skeleton, width: '30%', height: '14px' }} />
                        <div style={{ ...styles.skeleton, width: '50%', height: '14px' }} />
                    </div>
                ))}
            </div>
            <div style={{ marginTop: '24px' }}>
                <div style={{ ...styles.skeleton, width: '100%', height: '44px', marginBottom: '12px' }} />
                <div style={{ ...styles.skeleton, width: '100%', height: '44px' }} />
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
    header: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        marginBottom: '24px',
    },
    avatar: {
        width: '88px',
        height: '88px',
        borderRadius: '50%',
        backgroundColor: '#1976d2',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: '12px',
        overflow: 'hidden',
    },
    avatarImg: {
        width: '100%',
        height: '100%',
        objectFit: 'cover',
    },
    avatarText: {
        color: '#fff',
        fontSize: '36px',
        fontWeight: 'bold',
    },
    name: {
        margin: '0 0 8px 0',
        fontSize: '24px',
        textAlign: 'center',
    },
    statusBadge: {
        padding: '4px 14px',
        borderRadius: '12px',
        color: '#fff',
        fontSize: '13px',
        fontWeight: 'bold',
        textTransform: 'uppercase',
    },
    infoSection: {
        borderTop: '1px solid #eee',
        borderBottom: '1px solid #eee',
        padding: '16px 0',
        marginBottom: '24px',
    },
    infoRow: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '10px 0',
        alignItems: 'flex-start',
    },
    label: {
        color: '#666',
        fontSize: '14px',
        fontWeight: '500',
        minWidth: '100px',
    },
    value: {
        color: '#222',
        fontSize: '14px',
        textAlign: 'right',
        maxWidth: '60%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-end',
        gap: '4px',
    },
    verifiedBadge: {
        padding: '2px 8px',
        borderRadius: '10px',
        color: '#fff',
        fontSize: '11px',
        fontWeight: 'bold',
        whiteSpace: 'nowrap',
    },
    actions: {
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        marginBottom: '16px',
    },
    primaryButton: {
        padding: '12px',
        backgroundColor: '#1976d2',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        fontSize: '15px',
        cursor: 'pointer',
        fontWeight: '500',
    },
    secondaryButton: {
        padding: '12px',
        backgroundColor: '#fff',
        color: '#1976d2',
        border: '1px solid #1976d2',
        borderRadius: '4px',
        fontSize: '15px',
        cursor: 'pointer',
        fontWeight: '500',
    },
    backLink: {
        display: 'block',
        textAlign: 'center',
        color: '#1976d2',
        textDecoration: 'none',
        fontSize: '14px',
    },
    errorText: {
        color: '#b71c1c',
        textAlign: 'center',
        fontSize: '16px',
    },
    // Skeleton styles
    skeletonContainer: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
    },
    skeleton: {
        backgroundColor: '#e0e0e0',
        borderRadius: '4px',
        animation: 'skeleton-pulse 1.5s ease-in-out infinite',
    },
    skeletonAvatar: {
        width: '88px',
        height: '88px',
        borderRadius: '50%',
        marginBottom: '12px',
    },
    skeletonText: {
        height: '24px',
        marginBottom: '8px',
    },
    skeletonBadge: {
        width: '80px',
        height: '24px',
        borderRadius: '12px',
        marginBottom: '8px',
    },
    skeletonRow: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '10px 0',
    },
};