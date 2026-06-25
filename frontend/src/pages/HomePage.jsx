import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import NewsCarousel from '../components/NewsCarousel';
import QuickStats from '../components/QuickStats';
import RecentActivities from '../components/RecentActivities';
import QuickActions from '../components/QuickActions';
import { SkeletonCarousel, SkeletonCard, SkeletonActivityList } from '../components/SkeletonLoader';

export default function HomePage() {
    const { user, loading } = useAuth();
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [contentLoading, setContentLoading] = useState(true);

    // Simulate API fetch for dashboard widgets
    useEffect(() => {
        if (!loading && user) {
            const timer = setTimeout(() => setContentLoading(false), 1200);
            return () => clearTimeout(timer);
        }
    }, [loading, user]);

    // Auth Guard
    if (loading) {
        return (
            <div className="home-loading">
                <div className="spinner" />
                <p>Loading...</p>
            </div>
        );
    }

    if (!user) {
        navigate('/login', { replace: true });
        return null;
    }

    return (
        <div className="dashboard">
            {/* Hamburger Menu Button (visible on mobile only) */}
            <button
                className="hamburger-btn"
                onClick={() => setSidebarOpen(true)}
                aria-label="Open menu"
            >
                ☰
            </button>

            {/* Left Sidebar */}
            <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

            {/* Main Content Area */}
            <main className="main-content">
                {/* News Carousel */}
                <section className="main-section">
                    {contentLoading ? (
                        <SkeletonCarousel />
                    ) : (
                        <NewsCarousel />
                    )}
                </section>

                {/* Demo Content Widgets */}
                <section className="main-section main-section--demo">
                    {/* Quick Stats */}
                    {contentLoading ? (
                        <div className="quick-stats">
                            <SkeletonCard />
                            <SkeletonCard />
                            <SkeletonCard />
                        </div>
                    ) : (
                        <QuickStats />
                    )}

                    {/* Quick Actions */}
                    <QuickActions loading={contentLoading} />

                    {/* Recent Activities */}
                    {contentLoading ? (
                        <SkeletonActivityList />
                    ) : (
                        <RecentActivities />
                    )}
                </section>
            </main>
        </div>
    );
}