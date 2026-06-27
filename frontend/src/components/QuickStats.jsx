export default function QuickStats({ loading = false }) {
    if (loading) return null; // skeleton handled by parent

    const stats = [
        { label: 'Pending Applications', value: '2', icon: '📄', color: '#1976d2' },
        { label: 'Unread Messages', value: '5', icon: '💬', color: '#388e3c' },
        { label: 'Eligible Policies', value: '1', icon: '🛡️', color: '#f57c00' },
    ];

    return (
        <div className="quick-stats">
            {stats.map((stat) => (
                <div key={stat.label} className="quick-stat-card">
                    <div className="quick-stat-icon" style={{ backgroundColor: stat.color }}>
                        {stat.icon}
                    </div>
                    <div className="quick-stat-info">
                        <span className="quick-stat-value">{stat.value}</span>
                        <span className="quick-stat-label">{stat.label}</span>
                    </div>
                </div>
            ))}
        </div>
    );
}