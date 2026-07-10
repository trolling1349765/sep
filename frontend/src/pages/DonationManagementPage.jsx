import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import donationApi from '../api/donationApi';

const DonationManagementPage = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [dashboard, setDashboard] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user || user.role !== 'CB_QUAN_LY') {
            navigate('/home');
            return;
        }
        fetchDashboard();
    }, [user, navigate]);

    const fetchDashboard = async () => {
        try {
            const res = await donationApi.getDonationDashboard();
            if (res.data?.code === 200) {
                setDashboard(res.data.data);
            }
        } catch (err) {
            console.error('Failed to load dashboard', err);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div className="loading">Đang tải...</div>;

    return (
        <div className="donation-management">
            <div className="page-header">
                <h1>Quản lý tài trợ & Nguồn lực</h1>
                <p>Quản lý nhà tài trợ, tiếp nhận tài trợ, kho vật phẩm và phân phát</p>
            </div>

            <div className="dashboard-stats">
                <div className="stat-card" onClick={() => navigate('/donation-management/sponsors')}>
                    <div className="stat-icon">🏢</div>
                    <div className="stat-info">
                        <h3>{dashboard?.totalSponsors || 0}</h3>
                        <p>Nhà tài trợ</p>
                    </div>
                </div>
                <div className="stat-card" onClick={() => navigate('/donation-management/donations')}>
                    <div className="stat-icon">💰</div>
                    <div className="stat-info">
                        <h3>{dashboard?.totalDonations || 0}</h3>
                        <p>Khoản tài trợ</p>
                    </div>
                </div>
                <div className="stat-card" onClick={() => navigate('/donation-management/inventory')}>
                    <div className="stat-icon">📦</div>
                    <div className="stat-info">
                        <h3>{dashboard?.totalInventoryItems || 0}</h3>
                        <p>Vật phẩm trong kho</p>
                    </div>
                </div>
                <div className="stat-card" onClick={() => navigate('/donation-management/distribution')}>
                    <div className="stat-icon">📋</div>
                    <div className="stat-info">
                        <h3>{dashboard?.totalPlans || 0}</h3>
                        <p>Kế hoạch phân bổ</p>
                    </div>
                </div>
            </div>

            <div className="management-modules">
                <h2>Chức năng quản lý</h2>
                <div className="module-grid">
                    <div className="module-card" onClick={() => navigate('/donation-management/sponsors')}>
                        <div className="module-icon">🏢</div>
                        <h3>Quản lý nhà tài trợ</h3>
                        <p>Đăng ký, cập nhật và tra cứu thông tin nhà tài trợ, tổ chức quyên góp</p>
                        <span className="module-link">Xem danh sách →</span>
                    </div>
                    <div className="module-card" onClick={() => navigate('/donation-management/donations')}>
                        <div className="module-icon">💰</div>
                        <h3>Quản lý tiếp nhận tài trợ</h3>
                        <p>Ghi nhận các khoản tài trợ bằng tiền và theo dõi lịch sử giao dịch</p>
                        <span className="module-link">Xem danh sách →</span>
                    </div>
                    <div className="module-card" onClick={() => navigate('/donation-management/inventory')}>
                        <div className="module-icon">📦</div>
                        <h3>Quản lý kho hiện vật</h3>
                        <p>Quản lý nhập kho, tồn kho, điều chỉnh số lượng và theo dõi vật phẩm hỗ trợ</p>
                        <span className="module-link">Xem kho →</span>
                    </div>
                    <div className="module-card" onClick={() => navigate('/donation-management/distribution')}>
                        <div className="module-icon">📋</div>
                        <h3>Quản lý phân phát</h3>
                        <p>Lập kế hoạch phân bổ, thực hiện cấp phát vật phẩm đến đối tượng thụ hưởng</p>
                        <span className="module-link">Xem kế hoạch →</span>
                    </div>
                </div>
            </div>

            <style>{`
                .donation-management {
                    padding: 24px;
                    max-width: 1200px;
                    margin: 0 auto;
                }
                .page-header {
                    margin-bottom: 32px;
                }
                .page-header h1 {
                    margin: 0 0 8px;
                    font-size: 28px;
                    color: #1a1a2e;
                }
                .page-header p {
                    color: #666;
                    margin: 0;
                }
                .dashboard-stats {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 16px;
                    margin-bottom: 40px;
                }
                .stat-card {
                    background: #fff;
                    border-radius: 12px;
                    padding: 20px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                    display: flex;
                    align-items: center;
                    gap: 16px;
                    cursor: pointer;
                    transition: transform 0.2s, box-shadow 0.2s;
                }
                .stat-card:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 4px 16px rgba(0,0,0,0.12);
                }
                .stat-icon {
                    font-size: 36px;
                    width: 60px;
                    height: 60px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: #eef2ff;
                    border-radius: 12px;
                }
                .stat-info h3 {
                    margin: 0;
                    font-size: 24px;
                    color: #1a1a2e;
                }
                .stat-info p {
                    margin: 4px 0 0;
                    color: #666;
                    font-size: 14px;
                }
                .management-modules h2 {
                    margin-bottom: 20px;
                    font-size: 22px;
                    color: #1a1a2e;
                }
                .module-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
                    gap: 20px;
                }
                .module-card {
                    background: #fff;
                    border-radius: 12px;
                    padding: 24px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                    cursor: pointer;
                    transition: transform 0.2s, box-shadow 0.2s;
                }
                .module-card:hover {
                    transform: translateY(-4px);
                    box-shadow: 0 6px 20px rgba(0,0,0,0.12);
                }
                .module-icon {
                    font-size: 40px;
                    margin-bottom: 16px;
                }
                .module-card h3 {
                    margin: 0 0 8px;
                    font-size: 18px;
                    color: #1a1a2e;
                }
                .module-card p {
                    color: #666;
                    font-size: 14px;
                    line-height: 1.5;
                    margin: 0 0 16px;
                }
                .module-link {
                    color: #4f46e5;
                    font-weight: 500;
                    font-size: 14px;
                }
                .loading {
                    text-align: center;
                    padding: 60px;
                    color: #666;
                }
            `}</style>
        </div>
    );
};

export default DonationManagementPage;