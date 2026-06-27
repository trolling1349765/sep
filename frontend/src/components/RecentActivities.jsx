export default function RecentActivities({ loading = false }) {
    if (loading) return null; // skeleton handled by parent

    const activities = [
        { id: 1, text: 'You updated your profile', time: '2 days ago', icon: '👤' },
        { id: 2, text: 'Application #1234 was approved', time: '3 days ago', icon: '✅' },
        { id: 3, text: 'New policy update available', time: '5 days ago', icon: '📋' },
        { id: 4, text: 'You submitted a support request', time: '1 week ago', icon: '🎧' },
        { id: 5, text: 'Password changed successfully', time: '2 weeks ago', icon: '🔒' },
    ];

    return (
        <div className="recent-activities">
            <h3 className="section-title">Recent Activities</h3>
            <ul className="activity-list">
                {activities.map((activity) => (
                    <li key={activity.id} className="activity-item">
                        <span className="activity-icon">{activity.icon}</span>
                        <div className="activity-info">
                            <span className="activity-text">{activity.text}</span>
                            <span className="activity-time">{activity.time}</span>
                        </div>
                    </li>
                ))}
            </ul>
        </div>
    );
}