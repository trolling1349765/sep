import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import donationApi from '../api/donationApi';

const DonationListPage = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [donations, setDonations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [formData, setFormData] = useState({
        sponsorId: '', amount: '', transferDate: '', purpose: '',
        paymentMethod: 'CASH', notes: ''
    });
    const [sponsors, setSponsors] = useState([]);

    useEffect(() => {
        if (!user || user.role !== 'CB_QUAN_LY') { navigate('/home'); return; }
        fetchDonations();
        fetchSponsors();
    }, [user, navigate]);

    const fetchDonations = async () => {
        try {
            const res = await donationApi.getDonations();
            if (res.data?.code === 200) setDonations(res.data.data);
        } catch (err) { console.error(err); }
        finally { setLoading(false); }
    };

    const fetchSponsors = async () => {
        try {
            const res = await donationApi.getSponsors();
            if (res.data?.code === 200) setSponsors(res.data.data);
        } catch (err) { console.error(err); }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await donationApi.createDonation({
                ...formData,
                amount: parseFloat(formData.amount)
            });
            setShowForm(false);
            resetForm();
            fetchDonations();
        } catch (err) { console.error(err); }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Bạn có chắc chắn muốn xóa?')) return;
        try {
            await donationApi.deleteDonation(id);
            fetchDonations();
        } catch (err) { console.error(err); }
    };

    const resetForm = () => {
        setFormData({ sponsorId: '', amount: '', transferDate: '', purpose: '', paymentMethod: 'CASH', notes: '' });
    };

    const getPaymentMethodLabel = (method) => {
        const map = { CASH: 'Tiền mặt', TRANSFER: 'Chuyển khoản', OTHER: 'Khác' };
        return map[method] || method;
    };

    if (loading) return <div className="loading">Đang tải...</div>;

    return (
        <div className="donation-page">
            <div className="page-header">
                <div>
                    <button className="btn-back" onClick={() => navigate('/donation-management')}>← Quay lại</button>
                    <h1>Quản lý tiếp nhận tài trợ</h1>
                </div>
                <button className="btn-primary" onClick={() => { resetForm(); setShowForm(true); }}>+ Ghi nhận tài trợ</button>
            </div>

            {showForm && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2>Ghi nhận nguồn kinh phí</h2>
                        <form onSubmit={handleSubmit}>
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>Nhà tài trợ</label>
                                    <select value={formData.sponsorId} onChange={(e) => setFormData({ ...formData, sponsorId: e.target.value })}>
                                        <option value="">-- Chọn --</option>
                                        {sponsors.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label>Số tiền *</label>
                                    <input type="number" step="0.01" required value={formData.amount}
                                        onChange={(e) => setFormData({ ...formData, amount: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Ngày chuyển</label>
                                    <input type="date" value={formData.transferDate}
                                        onChange={(e) => setFormData({ ...formData, transferDate: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Phương thức</label>
                                    <select value={formData.paymentMethod} onChange={(e) => setFormData({ ...formData, paymentMethod: e.target.value })}>
                                        <option value="CASH">Tiền mặt</option>
                                        <option value="TRANSFER">Chuyển khoản</option>
                                        <option value="OTHER">Khác</option>
                                    </select>
                                </div>
                                <div className="form-group full-width">
                                    <label>Mục đích</label>
                                    <input value={formData.purpose} onChange={(e) => setFormData({ ...formData, purpose: e.target.value })} />
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

            <div className="data-table">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Nhà tài trợ</th>
                            <th>Số tiền</th>
                            <th>Ngày</th>
                            <th>Phương thức</th>
                            <th>Mục đích</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        {donations.length === 0 ? (
                            <tr><td colSpan="8" className="no-data">Không có dữ liệu</td></tr>
                        ) : donations.map((d) => (
                            <tr key={d.id}>
                                <td>{d.id}</td>
                                <td>{d.sponsorName || 'N/A'}</td>
                                <td>{d.amount?.toLocaleString()} VNĐ</td>
                                <td>{d.transferDate}</td>
                                <td>{getPaymentMethodLabel(d.paymentMethod)}</td>
                                <td>{d.purpose}</td>
                                <td><span className="badge badge-success">{d.receiptStatus === 'ISSUED' ? 'Đã xuất HĐ' : 'Chờ xuất HĐ'}</span></td>
                                <td className="actions">
                                    <button className="btn-sm btn-danger" onClick={() => handleDelete(d.id)}>Xóa</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <style>{`
                .donation-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
                .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
                .page-header div { display: flex; align-items: center; gap: 12px; }
                .page-header h1 { margin: 0; font-size: 24px; color: #1a1a2e; }
                .btn-back { background: none; border: none; color: #4f46e5; cursor: pointer; font-size: 14px; padding: 4px 0; }
                .btn-primary { background: #4f46e5; color: #fff; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 500; }
                .btn-secondary { background: #e5e7eb; color: #374151; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; }
                .btn-sm { background: #eef2ff; color: #4f46e5; border: none; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; }
                .btn-danger { background: #fef2f2; color: #ef4444; }
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
                .loading { text-align: center; padding: 60px; color: #666; }
            `}</style>
        </div>
    );
};

export default DonationListPage;