import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import donationApi from '../api/donationApi';

const SponsorListPage = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [sponsors, setSponsors] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [showForm, setShowForm] = useState(false);
    const [editingSponsor, setEditingSponsor] = useState(null);
    const [formData, setFormData] = useState({
        name: '', sponsorType: 'TO_CHUC', contactInfo: '', phone: '',
        email: '', address: '', representative: '', taxCode: '', status: 'ACTIVE'
    });

    useEffect(() => {
        if (!user || user.role !== 'CB_QUAN_LY') { navigate('/home'); return; }
        fetchSponsors();
    }, [user, navigate]);

    const fetchSponsors = async (keyword) => {
        try {
            const params = keyword ? { keyword } : {};
            const res = await donationApi.getSponsors(params);
            if (res.data?.code === 200) setSponsors(res.data.data);
        } catch (err) { console.error(err); }
        finally { setLoading(false); }
    };

    const handleSearch = () => {
        setLoading(true);
        fetchSponsors(searchTerm || undefined);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingSponsor) {
                await donationApi.updateSponsor(editingSponsor.id, formData);
            } else {
                await donationApi.createSponsor(formData);
            }
            setShowForm(false);
            setEditingSponsor(null);
            resetForm();
            fetchSponsors();
        } catch (err) { console.error(err); }
    };

    const handleEdit = (sponsor) => {
        setEditingSponsor(sponsor);
        setFormData({
            name: sponsor.name || '', sponsorType: sponsor.sponsorType || 'TO_CHUC',
            contactInfo: sponsor.contactInfo || '', phone: sponsor.phone || '',
            email: sponsor.email || '', address: sponsor.address || '',
            representative: sponsor.representative || '', taxCode: sponsor.taxCode || '',
            status: sponsor.status || 'ACTIVE'
        });
        setShowForm(true);
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Bạn có chắc chắn muốn xóa nhà tài trợ này?')) return;
        try {
            await donationApi.deleteSponsor(id);
            fetchSponsors();
        } catch (err) { console.error(err); }
    };

    const resetForm = () => {
        setFormData({
            name: '', sponsorType: 'TO_CHUC', contactInfo: '', phone: '',
            email: '', address: '', representative: '', taxCode: '', status: 'ACTIVE'
        });
    };

    if (loading) return <div className="loading">Đang tải...</div>;

    return (
        <div className="sponsor-page">
            <div className="page-header">
                <div>
                    <button className="btn-back" onClick={() => navigate('/donation-management')}>← Quay lại</button>
                    <h1>Quản lý nhà tài trợ</h1>
                </div>
                <button className="btn-primary" onClick={() => { resetForm(); setEditingSponsor(null); setShowForm(true); }}>
                    + Thêm nhà tài trợ
                </button>
            </div>

            <div className="search-bar">
                <input type="text" placeholder="Tìm kiếm nhà tài trợ..." value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && handleSearch()} />
                <button onClick={handleSearch}>Tìm kiếm</button>
            </div>

            {showForm && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2>{editingSponsor ? 'Cập nhật nhà tài trợ' : 'Thêm nhà tài trợ'}</h2>
                        <form onSubmit={handleSubmit}>
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>Tên nhà tài trợ *</label>
                                    <input required value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Loại</label>
                                    <select value={formData.sponsorType} onChange={(e) => setFormData({ ...formData, sponsorType: e.target.value })}>
                                        <option value="TO_CHUC">Tổ chức</option>
                                        <option value="CA_NHAN">Cá nhân</option>
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label>Số điện thoại</label>
                                    <input value={formData.phone} onChange={(e) => setFormData({ ...formData, phone: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Email</label>
                                    <input type="email" value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Địa chỉ</label>
                                    <input value={formData.address} onChange={(e) => setFormData({ ...formData, address: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Người đại diện</label>
                                    <input value={formData.representative} onChange={(e) => setFormData({ ...formData, representative: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Mã số thuế</label>
                                    <input value={formData.taxCode} onChange={(e) => setFormData({ ...formData, taxCode: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Thông tin liên hệ</label>
                                    <input value={formData.contactInfo} onChange={(e) => setFormData({ ...formData, contactInfo: e.target.value })} />
                                </div>
                            </div>
                            <div className="form-actions">
                                <button type="submit" className="btn-primary">{editingSponsor ? 'Cập nhật' : 'Lưu'}</button>
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
                            <th>Tên</th>
                            <th>Loại</th>
                            <th>SĐT</th>
                            <th>Email</th>
                            <th>Người đại diện</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        {sponsors.length === 0 ? (
                            <tr><td colSpan="7" className="no-data">Không có dữ liệu</td></tr>
                        ) : sponsors.map((s) => (
                            <tr key={s.id}>
                                <td>{s.name}</td>
                                <td>{s.sponsorType === 'TO_CHUC' ? 'Tổ chức' : 'Cá nhân'}</td>
                                <td>{s.phone}</td>
                                <td>{s.email}</td>
                                <td>{s.representative}</td>
                                <td><span className={`badge ${s.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`}>{s.status === 'ACTIVE' ? 'Hoạt động' : 'Không hoạt động'}</span></td>
                                <td className="actions">
                                    <button className="btn-sm" onClick={() => handleEdit(s)}>Sửa</button>
                                    <button className="btn-sm btn-danger" onClick={() => handleDelete(s.id)}>Xóa</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <style>{`
                .sponsor-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
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
                .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
                .form-group { display: flex; flex-direction: column; gap: 4px; }
                .form-group label { font-size: 13px; color: #374151; font-weight: 500; }
                .form-group input, .form-group select { padding: 8px 12px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 14px; }
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
                .loading { text-align: center; padding: 60px; color: #666; }
            `}</style>
        </div>
    );
};

export default SponsorListPage;