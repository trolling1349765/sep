import { useNavigate } from 'react-router-dom';

export default function QuickActions({ loading = false }) {
    const navigate = useNavigate();

    if (loading) return null; // skeleton handled by parent

    return (
        <div className="quick-actions">
            <h3 className="section-title">Quick Actions</h3>
            <div className="quick-actions-grid">
                <button
                    className="quick-action-btn quick-action-btn--primary"
                    onClick={() => navigate('/my-applications')}
                >
                    <span className="quick-action-icon">➕</span>
                    <span className="quick-action-text">Create New Application</span>
                </button>
                <button
                    className="quick-action-btn quick-action-btn--secondary"
                    onClick={() => navigate('/citizen-portal')}
                >
                    <span className="quick-action-icon">🔍</span>
                    <span className="quick-action-text">Check Policy Eligibility</span>
                </button>
            </div>
        </div>
    );
}