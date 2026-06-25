export default function SkeletonLoader({ width = '100%', height = '20px', borderRadius = '4px', style = {} }) {
    return (
        <div
            className="skeleton"
            style={{
                width,
                height,
                borderRadius,
                ...style,
            }}
        />
    );
}

export function SkeletonCard({ style = {} }) {
    return (
        <div className="skeleton-card" style={style}>
            <SkeletonLoader width="40px" height="40px" borderRadius="8px" />
            <SkeletonLoader width="60%" height="14px" style={{ marginTop: '12px' }} />
            <SkeletonLoader width="80%" height="24px" style={{ marginTop: '8px' }} />
        </div>
    );
}

export function SkeletonCarousel() {
    return (
        <div className="skeleton-carousel">
            <SkeletonLoader width="100%" height="220px" borderRadius="12px" />
        </div>
    );
}

export function SkeletonActivityList() {
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', padding: '16px' }}>
            {[1, 2, 3].map((i) => (
                <div key={i} style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                    <SkeletonLoader width="32px" height="32px" borderRadius="50%" />
                    <div style={{ flex: 1 }}>
                        <SkeletonLoader width="70%" height="14px" />
                        <SkeletonLoader width="40%" height="12px" style={{ marginTop: '6px' }} />
                    </div>
                </div>
            ))}
        </div>
    );
}