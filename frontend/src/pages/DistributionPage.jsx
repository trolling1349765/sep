import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import donationApi from '../api/donationApi';

const DistributionPage = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [plans, setPlans] = useState([]);
    const [distributions, setDistributions] = useState([]);
    const [activeTab, setActiveTab] = useState('plans');
    const [loading, setLoading] = useState(true);
    const [showPlanForm, setShowPlanForm] = useState(false);
    const [showDistForm, setShowDistForm] = useState(false);
    const [planForm, setPlanForm] = useState({ planCode: '', title: '', description: '', plannedDate: '', notes: '' });
    const [distForm, setDistForm] = useState({ allocationPlanId: '', goodsInventoryId: '', benificiaryId: '', quantity: 1, notes: '' });
    const [inventoryItems, setInventoryItems] = useState([]);

    useEffect(() => {
        if (!user || user.role !== 'CB_QUAN_LY') { navigate('/home'); return; }
        fetchData();
    }, [user, navigate]);

    const fetchData = async () => {
        try {
            const [plansRes, distRes, invRes] = await Promise.all([
                donationApi.getAllocationPlans(),
                donationApi.getDistributions(),
                donationApi.getInventoryItems()
            ]);
            if (plansRes.data?.code === 200) setPlans(plansRes.data.data);
            if (distRes.data?.code === 200) setDistributions(distRes.data.data);
            if (invRes.data?.code === 200) setInventoryItems(invRes.data.data);
        } catch (err) { console.error(err); }
        finally { setLoading(false); }
    };

    const handleCreatePlan = async (e) => {
        e.preventDefault();
        try {
            await donationApi.createAllocationPlan(planForm);
            setShowPlanForm(false);
            setPlanForm({ planCode: '', title: '', description: '', plannedDate: '', notes: '' });
            fetchData();
        } catch (err) { console.error(err); }
    };

    const handleCreateDist = async (e) => {
        e.preventDefault();
        try {
            await donationApi.createDistribution({
                ...distForm,
                allocationPlanId: parseInt(distForm.allocationPlanId) || null,
                goodsInventoryId: parseInt(distForm.goodsInventoryId) || null,
                benificiaryId: parseInt(distForm.benificiaryId) || null,
                quantity: parseInt(distForm.quantity)
            });
            setShowDistForm(false);
            setDistForm({ allocationPlanId: '', goodsInventoryId: '', benificiaryId: '', quantity: 1, notes: '' });
            fetchData();
        } catch (err) { console.error(err); }
    };

    const handleConfirmDist = async (id, status) => {
        try {
            await donationApi.confirmDistribution(id, status);
            fetchData();
        } catch (err) { console.error(err); }
    };

    const handleDeletePlan = async (id) => {
        if (!window.confirm('Xóa kế hoạch này?')) return;
        try {
            await donationApi.deleteAllocationPlan(id);
            fetchData();
        } catch (err) { console.error(err); }
    };

    const getStatusBadge = (status) => {
        const map = {
            'DRAFT': { cls: 'badge-warning', label: 'Nháp' },
            'CONFIRMED': { cls: 'badge-info', label: 'Đã xác nhận' },
            'COMPLETED': { cls: 'badge-success', label: 'Hoàn thành' },
            'CANCELLED': { cls: 'badge-danger', label: 'Đã hủy' }
        };
        const s = map[status] || { cls: 'badge-warning', label: status };
        return <span className={`badge ${s.cls}`}>{s.label}</span>;
    };

    const getConfirmBadge = (status) => {
        const map = {
            'PENDING': { cls: 'badge-warning', label: 'Chờ xác nhận' },
            'CONFIRMED': { cls: 'badge-success', label: 'Đã nhận' },
            'ABSENT': { cls: 'badge-danger', label: 'Vắng mặt' }
        };
        const s = map[status] || { cls: 'badge-warning', label: status };
        return <span className={`badge ${s.cls}`}>{s.label}</span>;
    };

    if (loading) return <div className="loading">Đang tải...</div>;

    return (
        <div className="distribution-page">
            <div className="page-header">
                <div>
                    <button className="btn-back" onClick={() => navigate('/donation-management')}>← Quay lại</button>
                    <h1>Quản lý phân phát</h1>
                </div>
            </div>

            <div className="tabs">
                <button className={`tab ${activeTab === 'plans' ? 'active' : ''}`} onClick={() => setActiveTab('plans')}>Kế hoạch phân bổ</button>
                <button className={`tab ${activeTab === 'dist' ? 'active' : ''}`} onClick={() => setActiveTab('dist')}>Lịch sử cấp phát</button>
            </div>

            {activeTab === 'plans' && (
                <>
                    <div className="section-header">
                        <h2>Danh sách kế hoạch</h2>
                        <button className="btn-primary" onClick={() => setShowPlanForm(true)}>+ Tạo kế hoạch</button>
                    </div>

                    {showPlanForm && (
                        <div className="modal-overlay">
                            <div className="modal-content">
                                <h2>Tạo kế hoạch phân bổ</h2>
                                <form onSubmit={handleCreatePlan}>
                                    <div className="form-grid">
                                        <div className="form-group">
                                            <label>Mã kế hoạch *</label>
                                            <input required value={planForm.planCode} onChange={(e) => setPlanForm({ ...planForm, planCode: e.target.value })} />
                                        </div>
                                        <div className="form-group">
                                            <label>Tiêu đề *</label>
                                            <input required value={planForm.title} onChange={(e) => setPlanForm({ ...planForm, title: e.target.value })} />
                                        </div>
                                        <div className="form-group">
                                            <label>Ngày dự kiến</label>
                                            <input type="date" value={planForm.plannedDate} onChange={(e) => setPlanForm({ ...planForm, plannedDate: e.target.value })} />
                                        </div>
                                        <div className="form-group">
                                            <label>Mô tả</label>
                                            <input value={planForm.description} onChange={(e) => setPlanForm({ ...planForm, description: e.target.value })} />
                                        </div>
                                        <div className="form-group full-width">
                                            <label>Ghi chú</label>
                                            <textarea value={planForm.notes} onChange={(e) => setPlanForm({ ...planForm, notes: e.target.value })} />
                                        </div>
                                    </div>
                                    <div className="form-actions">
                                        <button type="submit" className="btn-primary">Tạo</button>
                                        <button type="button" className="btn-secondary" onClick={() => setShowPlanForm(false)}>Hủy</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    )}

                    <div className="data-table">
                        <table>
                            <thead>
                                <tr>
                                    <th>Mã KH</th>
                                    <th>Tiêu đề</th>
                                    <th>Trạng thái</th>
                                    <th>Ngày dự kiến</th>
                                    <th>Ghi chú</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {plans.length === 0 ? (
                                    <tr><td colSpan="6" className="no-data">Không có dữ liệu</td></tr>
                                ) : plans.map(p => (
                                    <tr key={p.id}>
                                        <td>{p.planCode}</td>
                                        <td>{p.title}</td>
                                        <td>{getStatusBadge(p.status)}</td>
                                        <td>{p.plannedDate}</td>
                                        <td>{p.notes}</td>
                                        <td className="actions">
                                            <button className="btn-sm btn-danger" onClick={() => handleDeletePlan(p.id)}>Xóa</button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            {activeTab === 'dist' && (
                <>
                    <div className="section-header">
                        <h2>Lịch sử cấp phát</h2>
                        <button className="btn-primary" onClick={() => setShowDistForm(true)}>+ Cấp phát vật phẩm</button>
                    </div>

                    {showDistForm && (
                        <div className="modal-overlay">
                            <div className="modal-content">
                                <h2>Cấp phát vật phẩm</h2>
                                <form onSubmit={handleCreateDist}>
                                    <div className="form-grid">
                                        <div className="form-group">
                                            <label>Kế hoạch phân bổ</label>
                                            <select value={distForm.allocationPlanId} onChange={(e) => setDistForm({ ...distForm, allocationPlanId: e.target.value })}>
                                                <option value="">-- Chọn --</option>
                                                {plans.map(p => <option key={p.id} value={p.id}>{p.planCode} - {p.title}</option>)}
                                            </select>
                                        </div>
                                        <div className="form-group">
                                            <label>Vật phẩm trong kho</label>
                                            <select value={distForm.goodsInventoryId} onChange={(e) => setDistForm({ ...distForm, goodsInventoryId: e.target.value })}>
                                                <option value="">-- Chọn --</option>
                                                {inventoryItems.filter(i => i.status === 'AVAILABLE').map(item => (
                                                    <option key={item.id} value={item.id}>{item.itemName} (còn: {item.availableQuantity})</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div className="form-group">
                                            <label>Số lượng</label>
                                            <input type="number" min="1" value={distForm.quantity} onChange={(e) => setDistForm({ ...distForm, quantity: e.target.value })} />
                                        </div>
                                        <div className="form-group full-width">
                                            <label>Ghi chú</label>
                                            <textarea value={distForm.notes} onChange={(e) => setDistForm({ ...distForm, notes: e.target.value })} />
                                        </div>
                                    </div>
                                    <div className="form-actions">
                                        <button type="submit" className="btn-primary">Cấp phát</button>
                                        <button type="button" className="btn-secondary" onClick={() => setShowDistForm(false)}>Hủy</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    )}

                    <div className="data-table">
                        <table>
                            <thead>
                                <tr>
                                    <th>Vật phẩm</th>
                                    <th>Số lượng</th>
                                    <th>Ngày cấp</th>
                                    <th>Trạng thái</th>
                                    <th>Ghi chú</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {distributions.length === 0 ? (
                                    <tr><td colSpan="6" className="no-data">Không có dữ liệu</td></tr>
                                ) : distributions.map(d => (
                                    <tr key={d.id}>
                                        <td>{d.itemName || 'N/A'}</td>
                                        <td>{d.quantity}</td>
                                        <td>{d.transferDate}</td>
                                        <td>{getConfirmBadge(d.confirmationStatus)}</td>
                                        <td>{d.notes}</td>
                                        <td className="actions">
                                            {d.confirmationStatus === 'PENDING' && (
                                                <>
                                                    <button className="btn-sm" onClick={() => handleConfirmDist(d.id, 'CONFIRMED')}>Xác nhận</button>
                                                    <button className="btn-sm btn-danger" onClick={() => handleConfirmDist(d.id, 'ABSENT')}>Vắng</button>
                                                </>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <style>{`
                .distribution-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
                .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
                .page-header div { display: flex; align-items: center; gap: 12px; }
                .page-header h1 { margin: 0; font-size: 24px; color: #1a1a2e; }
                .btn-back { background: none; border: none; color: #4f46e5; cursor: pointer; font-size: 14px; padding: 4px 0; }
                .btn-primary { background: #4f46e5; color: #fff; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 500; }
                .btn-secondary { background: #e5e7eb; color: #374151; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; }
                .btn-sm { background: #eef2ff; color: #4f46e5; border: none; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; }
                .btn-danger { background: #fef2f2; color: #ef4444; }
                .tabs { display: flex; gap: 0; margin-bottom: 24px; background: #f3f4f6; border-radius: 8px; padding: 4px; }
                .tab { flex: 1; padding: 10px; border: none; background: transparent; cursor: pointer; border-radius: 6px; font-size: 14px; font-weight: 500; }
                .tab.active { background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.1); color: #4f46e5; }
                .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
                .section-header h2 { margin: 0; font-size: 18px; color: #1a1a2e; }
                .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
                .modal-content { background: #fff; border-radius: 12px; padding: 32px; width: 640px; max-height: 90vh; overflow-y: auto; }
                .modal-content h2 { margin: 0 0 20px; }
                .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
                .form-group { display: flex; flex-direction: column; gap: 4px; }
                .form-group label { font-size: 13px; color: #374151; font-weight: 500; }
                .form-group input, .form-group select, .form-group textarea { padding: 8px 12px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 14px; }
                .form-group.full-width { grid-column: 1 / -1; }
                .form-group textarea { min-height: 80px; resize: vertical; }
                .form-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 24px; }
                .data-table { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                .data-table table { width: 100%; border-collapse: collapse; }
                .data-table th { background: #f9fafb; padding: 12px 16px; text-align: left; font-size: 13px; color: #6b7280; font-weight: 600; }
                .data-table td { padding: 12px 16px; border-top: 1px solid #f3f4f6; font-size: 14px; color: #374151; }
                .data-table .no-data { text-align: center; color: #9ca3af; padding: 40px; }
                .actions { display: flex; gap: 8px; }
                .badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
                .badge-success { background: #ecfdf5; color: #059669; }
                .badge-danger { background: #fef2f2; color: #ef4444; }
                .badge-warning { background: #fffbeb; color: #d97706; }
                .badge-info { background: #eff6ff; color: #2563eb; }
                .loading { text-align: center; padding: 60px; color: #666; }
            `}</style>
        </div>
    );
};

export default DistributionPage;