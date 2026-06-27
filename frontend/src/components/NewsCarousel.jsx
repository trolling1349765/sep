import { useState, useEffect, useRef, useCallback } from 'react';

const slides = [
    {
        id: 1,
        title: 'New Policy Updates 2026',
        description: 'Important policy changes have been announced. Review the latest updates to stay compliant.',
        color: '#1976d2',
        icon: '📋',
    },
    {
        id: 2,
        title: 'How to Submit an Application',
        description: 'Learn the step-by-step process for submitting your applications online through our portal.',
        color: '#388e3c',
        icon: '📝',
    },
    {
        id: 3,
        title: 'Scheduled Maintenance',
        description: 'The system will undergo scheduled maintenance on Sunday, 2:00 AM - 6:00 AM. Some services may be unavailable.',
        color: '#f57c00',
        icon: '🔧',
    },
    {
        id: 4,
        title: 'Welcome to Citizen Portal',
        description: 'Access government services, track your applications, and stay informed about the latest announcements.',
        color: '#7b1fa2',
        icon: '🏛️',
    },
    {
        id: 5,
        title: 'New Features Available',
        description: 'Check out our new dashboard features including real-time notifications and improved application tracking.',
        color: '#c62828',
        icon: '✨',
    },
];

export default function NewsCarousel({ loading = false }) {
    const [current, setCurrent] = useState(0);
    const [isPaused, setIsPaused] = useState(false);
    const intervalRef = useRef(null);
    const len = slides.length;

    const goTo = useCallback((index) => {
        setCurrent((index + len) % len);
    }, [len]);

    const goNext = useCallback(() => goTo(current + 1), [current, goTo]);
    const goPrev = useCallback(() => goTo(current - 1), [current, goTo]);

    // Auto-play
    useEffect(() => {
        if (loading || isPaused) {
            clearInterval(intervalRef.current);
            return;
        }
        intervalRef.current = setInterval(() => {
            setCurrent((prev) => (prev + 1) % len);
        }, 4000);
        return () => clearInterval(intervalRef.current);
    }, [loading, isPaused, len]);

    if (loading) {
        return null; // Skeleton handled by parent
    }

    const slide = slides[current];

    return (
        <div
            className="carousel"
            onMouseEnter={() => setIsPaused(true)}
            onMouseLeave={() => setIsPaused(false)}
        >
            <div className="carousel-slide" style={{ backgroundColor: slide.color }}>
                <div className="carousel-content">
                    <span className="carousel-icon">{slide.icon}</span>
                    <h2 className="carousel-title">{slide.title}</h2>
                    <p className="carousel-description">{slide.description}</p>
                </div>
            </div>

            <button className="carousel-arrow carousel-arrow--left" onClick={goPrev} aria-label="Previous slide">
                ‹
            </button>
            <button className="carousel-arrow carousel-arrow--right" onClick={goNext} aria-label="Next slide">
                ›
            </button>

            <div className="carousel-dots">
                {slides.map((_, idx) => (
                    <button
                        key={idx}
                        className={`carousel-dot ${idx === current ? 'carousel-dot--active' : ''}`}
                        onClick={() => goTo(idx)}
                        aria-label={`Go to slide ${idx + 1}`}
                    />
                ))}
            </div>
        </div>
    );
}