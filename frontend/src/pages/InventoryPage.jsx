import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import donationApi from '../api/donationApi';

const InventoryPage = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [showForm, setShowForm] = useState(false);
    const [showAdjust, setShowAdjust] = useState(false);
    const [selectedItem, setSelectedItem] = useState(null);
    const [adjustQty, setAdjustQty] = useState(0);
    const [adjustReason, setAdjustReason] = useState('');
    const [formData, setFormData] = useState({
        sponsorId: '', itemName: '', unit: '', quantity: 0,
        conditionStatus: 'NEW', location: '', notes: ''
    });
    const [sponsors, setSponsors] = useState([]);

    useEffect(() => {
        if (!user || user.role !== 'CB_QUAN_LY') { navigate('/home'); return; }
        fetchItems();
        fetchSponsors();
    }, [user, navigate]);

    const fetchItems = async (keyword) => {
        try {
            const params = keyword ? { keyword } : {};
            const res = await donationApi.getInventoryItems(params);
            if (res.data?.code === 200) setItems(res.data.data);
        } catch (err) { console.error(err); }
        finally { setLoading(false); }
    };

    const fetchSponsors = async () => {
        try {
            const res = await donationApi.getSponsors();
            if (res.data?.code === 200) setSponsors(res.data.data);
        } catch (err) { console.error(err); }
    };

    const handleSearch = () => {
        setLoading(true);
        fetchItems(searchTerm || undefined);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await donationApi.createInventoryItem({ ...formData, quantity: parseInt(formData.quantity) });
            setShowForm(false);
            resetForm();
            fetchItems();
        } catch (err) { console.error(err); }
    };

    const handleAdjust = async () => {
        if (!selectedItem) return;
        try {
            await donationApi.adjustQuantity(selectedItem.id, parseInt(adjustQty), adjustReason);
            setShowAdjust(false);
            setSelectedItem(null);
            setAdjustQty(0);
            setAdjustReason('');
            fetchItems();
        } catch (err) { console.error(err); }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Bạn có chắc chắn muốn xóa?')) return;
        try {
            await donationApi.deleteInventoryItem(id);
            fetchItems();
        } catch (err) { console.error(err); }
    };

    const resetForm = () => {
        setFormData({ sponsorId: '', itemName: '', unit: '', quantity: 0, conditionStatus: 'NEW', location: '', notes: '' });
    };

    if (loading) return <div className="loading">Đang tải...</div>;

    return (
        <div className="inventory-page">
            <div className="page-header">
                <div>
                    <button className="btn-back" onClick={() => navigate('/donation-management')}>← Quay lại</button>
                    <h1>Quản lý kho hiện vật</h1>
                </div>
                <button className="btn-primary" onClick={() => { resetForm(); setShowForm(true); }}>+ Nhập kho</button>
            </div>

            <div className="search-bar">
                <input type="text" placeholder="Tìm kiếm vật phẩm..." value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && handleSearch()} />
                <button onClick={handleSearch}>Tìm kiếm</button>
            </div>

            {showForm && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2>Tiếp nhận vật phẩm / Nhập kho</h2>
                        <form onSubmit={handleSubmit}>
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>Tên vật phẩm *</label>
                                    <input required value={formData.itemName} onChange={(e) => setFormData({ ...formData, itemName: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Đơn vị</label>
                                    <input value={formData.unit} placeholder="KG, Cái, Thùng..." onChange={(e) => setFormData({ ...formData, unit: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Số lượng</label>
                                    <input type="number" min="0" value={formData.quantity} onChange={(e) => setFormData({ ...formData, quantity: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Nhà tài trợ</label>
                                    <select value={formData.sponsorId} onChange={(e) => setFormData({ ...formData, sponsorId: e.target.value })}>
                                        <option value="">-- Chọn --</option>
                                        {sponsors.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label>Tình trạng</label>
                                    <select value={formData.conditionStatus} onChange={(e) => setFormData({ ...formData, conditionStatus: e.target.value })}>
                                        <option value="NEW">Mới</option>
                                        <option value="GOOD">Tốt</option>
                                        <option value="DAMAGED">Hư hỏng</option>
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label>Vị trí lưu kho</label>
                                    <input value={formData.location} onChange={(e) => setFormData({ ...formData, location: e.target.value })} />
                                </div>
                                <div className="form-group full-width">
                                    <label>Ghi chú</label>
                                    <textarea value={formData.notes} onChange={(e) => setFormData({ ...formData, notes: e.target.value })} />
                                </div>
                            </div>
                            <div className="form-actions">
                                <button type="submit" className="btn-primary">Lưu</button>
                                <button type="button" className="btn-secondary" onClick={() => setShowForm(false)}>Hủy</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {showAdjust && selectedItem && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2>Điều chỉnh số lượng: {selectedItem.itemName}</h2>
                        <p className="text-muted">Tồn kho hiện tại: <strong>{selectedItem.quantity} {selectedItem.unit}</strong></p>
                        <div className="form-group">
                            <label>Số lượng điều chỉnh (dương: nhập thêm, âm: xuất bớt)</label>
                            <input type="number" value={adjustQty} onChange={(e) => setAdjustQty(e.target.value)} />
                        </div>
                        <div className="form-group">
                            <label>Lý do</label>
                            <textarea value={adjustReason} onChange={(e) => setAdjustReason(e.target.value)} />
                        </div>
                        <div className="form-actions">
                            <button className="btn-primary" onClick={handleAdjust}>Xác nhận</button>
                            <button className="btn-secondary" onClick={() => setShowAdjust(false)}>Hủy</button>
                        </div>
                    </div>
                </div>
            )}

            <div className="data-table">
                <table>
                    <thead>
                        <tr>
                            <th>Tên vật phẩm</th>
                            <th>ĐVT</th>
                            <th>Tồn kho</th>
                            <th>Đã đặt trước</th>
                            <th>Có sẵn</th>
                            <th>Tình trạng</th>
                            <th>Nhà tài trợ</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        {items.length === 0 ? (
                            <tr><td colSpan="8" className="no-data">Không có dữ liệu</td></tr>
                        ) : items.map((item) => (
                            <tr key={item.id}>
                                <td>{item.itemName}</td>
                                <td>{item.unit}</td>
                                <td>{item.quantity}</td>
                                <td>{item.reservedQuantity}</td>
                                <td>{item.availableQuantity}</td>
                                <td><span className={`badge ${item.status === 'AVAILABLE' ? 'badge-success' : item.status === 'DEPLETED' ? 'badge-danger' : 'badge-warning'}`}>
                                    {item.status === 'AVAILABLE' ? 'Còn hàng' : item.status === 'DEPLETED' ? 'Hết hàng' : 'Đã đặt trước'}
                                </span></td>
                                <td>{item.sponsorName || 'N/A'}</td>
                                <td className="actions">
                                    <button className="btn-sm" onClick={() => { setSelectedItem(item); setAdjustQty(0); setAdjustReason(''); setShowAdjust(true); }}>Điều chỉnh</button>
                                    <button className="btn-sm btn-danger" onClick={() => handleDelete(item.id)}>Xóa</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <style>{`
                .inventory-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
                .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
                .page-header div { display: flex; align-items: center; gap: 12px; }
                .page-header h1 { margin: 0; font-size: 24px; color: #1a1a2e; }
                .btn-back { background: none; border: none; color: #4f46e5; cursor: pointer; font-size: 14px; padding: 4px 0; }
                .btn-primary { background: #4f46e5; color: #fff; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 500; }
                .btn-secondary { background: #e5e7eb; color: #374151; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; }
                .btn-sm { background: #eef2ff; color: #4f46e5; border: none; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; }
                .btn-danger { background: #fef2f2; color: #ef4444; }
                .search-bar { display: flex; gap: 8px; margin-bottom: 20px; }
                .search-bar input { flex: 1; padding: 10px 16px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; }
                .search-bar button { padding: 10px 20px; background: #4f46e5; color: #fff; border: none; border-radius: 8px; cursor: pointer; }
                .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
                .modal-content { background: #fff; border-radius: 12px; padding: 32px; width: 640px; max-height: 90vh; overflow-y: auto; }
                .modal-content h2 { margin: 0 0 20px; }
                .text-muted { color: #6b7280; font-size: 14px; margin-bottom: 16px; }
                .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
                .form-group { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; }
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
                .loading { text-align: center; padding: 60px; color: #666; }
            `}</style>
        </div>
    );
};

export default InventoryPage;