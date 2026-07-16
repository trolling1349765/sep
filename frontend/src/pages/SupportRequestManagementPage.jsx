import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import {
    getAllRequests,
    getRequestDetail,
    replyToRequest,
    updateRequestStatus,
} from '../api/supportRequestApi';

const CATEGORY_LABELS = {
    APPLICATION_GUIDANCE: 'Application Guidance',
    POLICY_CLARIFICATION: 'Policy Clarification',
    TECHNICAL_ISSUE: 'Technical Issue',
};

const STATUS_LABELS = {
    PENDING: 'Pending',
    IN_PROGRESS: 'In Progress',
    RESOLVED: 'Resolved',
};

const STATUS_COLORS = {
    PENDING: '#f59e0b',
    IN_PROGRESS: '#3b82f6',
    RESOLVED: '#10b981',
};

export default function SupportRequestManagementPage() {
    const { user } = useAuth();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    // Filters
    const [filterStatus, setFilterStatus] = useState('');
    const [filterCategory, setFilterCategory] = useState('');
    const [filterDateFrom, setFilterDateFrom] = useState('');
    const [filterDateTo, setFilterDateTo] = useState('');

    // Detail view
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');

    // Reply
    const [replyMessage, setReplyMessage] = useState('');
    const [replying, setReplying] = useState(false);
    const [replyError, setReplyError] = useState('');

    // Status update
    const [updatingStatus, setUpdatingStatus] = useState(false);

    const fetchRequests = useCallback(async () => {
        setLoading(true);
        try {
            const params = {
                page,
                size: 20,
                status: filterStatus || undefined,
                category: filterCategory || undefined,
                dateFrom: filterDateFrom || undefined,
                dateTo: filterDateTo || undefined,
            };
            const response = await getAllRequests(params);
            if (response.code === 200) {
                setRequests(response.data.content);
                setTotalPages(response.data.totalPages);
            }
        } catch (err) {
            console.error('Failed to fetch requests:', err);
        } finally {
            setLoading(false);
        }
    }, [page, filterStatus, filterCategory, filterDateFrom, filterDateTo]);

    useEffect(() => {
        fetchRequests();
    }, [fetchRequests]);

    const handleViewDetail = async (id) => {
        setDetailLoading(true);
        setDetailError('');
        setReplyError('');
        setReplyMessage('');
        try {
            const response = await getRequestDetail(id);
            if (response.code === 200) {
                setSelectedRequest(response.data);
            } else {
                setDetailError(response.message || 'Failed to load request detail.');
            }
        } catch (err) {
            console.error('Failed to fetch detail:', err);
            setDetailError(err.response?.data?.message || 'Failed to load request detail. Please try again.');
        } finally {
            setDetailLoading(false);
        }
    };

    const handleReply = async () => {
        if (!replyMessage.trim()) {
            setReplyError('Please enter a reply message.');
            return;
        }

        setReplying(true);
        setReplyError('');
        try {
            const response = await replyToRequest(selectedRequest.id, replyMessage.trim());
            if (response.code === 200) {
                setReplyMessage('');
                // Refresh detail
                handleViewDetail(selectedRequest.id);
            }
        } catch (err) {
            setReplyError(err.response?.data?.message || 'Failed to send reply.');
        } finally {
            setReplying(false);
        }
    };

    const handleStatusUpdate = async (newStatus) => {
        setUpdatingStatus(true);
        try {
            const response = await updateRequestStatus(selectedRequest.id, newStatus);
            if (response.code === 200) {
                setSelectedRequest(response.data);
                // Refresh list
                fetchRequests();
            }
        } catch (err) {
            console.error('Failed to update status:', err);
        } finally {
            setUpdatingStatus(false);
        }
    };

    const handleApplyFilters = () => {
        setPage(0);
        fetchRequests();
    };

    const handleClearFilters = () => {
        setFilterStatus('');
        setFilterCategory('');
        setFilterDateFrom('');
        setFilterDateTo('');
        setPage(0);
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return new Date(dateStr).toLocaleDateString('en-GB');
    };

    const formatDateTime = (instant) => {
        if (!instant) return '';
        return new Date(instant).toLocaleString('en-GB');
    };

    // Check if user has officer role (simplified check)
    const isOfficer =
        user?.role === 'Reception' || user?.role === 'Appraisal';

    if (!user) {
        return (
            <div className="page-container">
                <div className="content-area">
                    <p>Please log in to access this page.</p>
                </div>
            </div>
        );
    }

    if (!isOfficer) {
        return (
            <div className="page-container">
                <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
                <div className="content-area">
                    <p>You do not have permission to access this page.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="page-container">
            <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
            <div className="content-area">
                <button
                    className="sidebar-toggle"
                    onClick={() => setSidebarOpen(!sidebarOpen)}
                >
                    ☰
                </button>

                <div className="page-header">
                    <h1>Support Request Management</h1>
                    {(selectedRequest || detailError) && (
                        <button
                            className="btn btn-secondary"
                            onClick={() => {
                                setSelectedRequest(null);
                                setDetailError('');
                            }}
                        >
                            ← Back to List
                        </button>
                    )}
                </div>

                {/* Detail View */}
                {(selectedRequest || detailLoading || detailError) && (
                    <div className="detail-container">
                        {/* Loading state */}
                        {detailLoading && (
                            <div className="card">
                                <div className="card-body">
                                    <p>Loading request detail...</p>
                                </div>
                            </div>
                        )}

                        {/* Error state */}
                        {detailError && !detailLoading && (
                            <div className="card">
                                <div className="card-body">
                                    <div className="alert alert-danger">{detailError}</div>
                                    <button
                                        className="btn btn-secondary"
                                        onClick={() => setSelectedRequest(null)}
                                    >
                                        ← Back to List
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* Request Info */}
                        {selectedRequest && !detailLoading && (
                            <div className="card">
                                <div className="card-header">
                                    <h2>Request - {selectedRequest.referenceNumber}</h2>
                                    <div className="status-controls">
                                        <span
                                            className="status-badge"
                                            style={{
                                                backgroundColor:
                                                    STATUS_COLORS[selectedRequest.status],
                                            }}
                                        >
                                            {STATUS_LABELS[selectedRequest.status]}
                                        </span>
                                        <select
                                            className="form-control form-control-sm"
                                            value={selectedRequest.status}
                                            onChange={(e) => handleStatusUpdate(e.target.value)}
                                            disabled={updatingStatus}
                                            style={{ width: 'auto', marginLeft: '8px' }}
                                        >
                                            <option value="PENDING">Pending</option>
                                            <option value="IN_PROGRESS">In Progress</option>
                                            <option value="RESOLVED">Resolved</option>
                                        </select>
                                    </div>
                                </div>
                                <div className="card-body">
                                    <div className="detail-section">
                                        <h3>Citizen Information</h3>
                                        <div className="detail-row">
                                            <strong>Name:</strong> {selectedRequest.citizenName}
                                        </div>
                                        <div className="detail-row">
                                            <strong>Email:</strong> {selectedRequest.citizenEmail}
                                        </div>
                                        {selectedRequest.citizenPhone && (
                                            <div className="detail-row">
                                                <strong>Phone:</strong> {selectedRequest.citizenPhone}
                                            </div>
                                        )}
                                    </div>

                                    <div className="detail-section">
                                        <h3>Request Details</h3>
                                        <div className="detail-row">
                                            <strong>Category:</strong>{' '}
                                            {CATEGORY_LABELS[selectedRequest.category]}
                                        </div>
                                        <div className="detail-row">
                                            <strong>Subject:</strong> {selectedRequest.subject}
                                        </div>
                                        <div className="detail-row">
                                            <strong>Description:</strong>
                                            <p>{selectedRequest.description}</p>
                                        </div>
                                        <div className="detail-row">
                                            <strong>Submitted:</strong>{' '}
                                            {formatDate(selectedRequest.submissionDate)}
                                        </div>
                                        {selectedRequest.assignedOfficerName && (
                                            <div className="detail-row">
                                                <strong>Assigned To:</strong>{' '}
                                                {selectedRequest.assignedOfficerName}
                                            </div>
                                        )}
                                    </div>

                                    {selectedRequest.attachments?.length > 0 && (
                                        <div className="detail-section">
                                            <h3>Attachments</h3>
                                            <ul className="file-list">
                                                {selectedRequest.attachments.map((att) => (
                                                    <li key={att.id} className="file-list-item">
                                                        <span>📎 {att.fileName}</span>
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Replies */}
                        {selectedRequest && !detailLoading && (
                            <div className="card">
                                <div className="card-header">
                                    <h2>Replies</h2>
                                </div>
                                <div className="card-body">
                                    {selectedRequest.replies?.length === 0 ? (
                                        <p className="text-muted">No replies yet.</p>
                                    ) : (
                                        <div className="replies-list">
                                            {selectedRequest.replies?.map((reply) => (
                                                <div key={reply.id} className="reply-item">
                                                    <div className="reply-header">
                                                        <strong>{reply.officerName}</strong>
                                                        <span className="text-muted">
                                                            {formatDateTime(reply.createdAt)}
                                                        </span>
                                                    </div>
                                                    <p className="reply-message">{reply.message}</p>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {/* Reply Box */}
                                    <div className="reply-box">
                                        <h3>Add a Reply</h3>
                                        {replyError && (
                                            <div className="alert alert-danger">{replyError}</div>
                                        )}
                                        <textarea
                                            className="form-control"
                                            rows="3"
                                            value={replyMessage}
                                            onChange={(e) => setReplyMessage(e.target.value)}
                                            placeholder="Type your reply here..."
                                        />
                                        <button
                                            className="btn btn-primary"
                                            onClick={handleReply}
                                            disabled={replying || !replyMessage.trim()}
                                            style={{ marginTop: '8px' }}
                                        >
                                            {replying ? 'Sending...' : 'Send Reply'}
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* List View */}
                {!selectedRequest && !detailLoading && !detailError && (
                    <div className="card">
                        <div className="card-header">
                            <h2>All Support Requests</h2>
                        </div>
                        <div className="card-body">
                            {/* Filters */}
                            <div className="filter-bar">
                                <div className="filter-group">
                                    <label>Status</label>
                                    <select
                                        className="form-control form-control-sm"
                                        value={filterStatus}
                                        onChange={(e) => setFilterStatus(e.target.value)}
                                    >
                                        <option value="">All</option>
                                        <option value="PENDING">Pending</option>
                                        <option value="IN_PROGRESS">In Progress</option>
                                        <option value="RESOLVED">Resolved</option>
                                    </select>
                                </div>
                                <div className="filter-group">
                                    <label>Category</label>
                                    <select
                                        className="form-control form-control-sm"
                                        value={filterCategory}
                                        onChange={(e) => setFilterCategory(e.target.value)}
                                    >
                                        <option value="">All</option>
                                        {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
                                            <option key={key} value={key}>
                                                {label}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="filter-group">
                                    <label>From Date</label>
                                    <input
                                        type="date"
                                        className="form-control form-control-sm"
                                        value={filterDateFrom}
                                        onChange={(e) => setFilterDateFrom(e.target.value)}
                                    />
                                </div>
                                <div className="filter-group">
                                    <label>To Date</label>
                                    <input
                                        type="date"
                                        className="form-control form-control-sm"
                                        value={filterDateTo}
                                        onChange={(e) => setFilterDateTo(e.target.value)}
                                    />
                                </div>
                                <div className="filter-actions">
                                    <button
                                        className="btn btn-sm btn-primary"
                                        onClick={handleApplyFilters}
                                    >
                                        Apply Filters
                                    </button>
                                    <button
                                        className="btn btn-sm btn-secondary"
                                        onClick={handleClearFilters}
                                    >
                                        Clear
                                    </button>
                                </div>
                            </div>

                            {loading ? (
                                <p>Loading...</p>
                            ) : requests.length === 0 ? (
                                <p className="text-muted">No support requests found.</p>
                            ) : (
                                <>
                                    <div className="table-responsive">
                                        <table className="table">
                                            <thead>
                                                <tr>
                                                    <th>Reference #</th>
                                                    <th>Citizen Name</th>
                                                    <th>Category</th>
                                                    <th>Subject</th>
                                                    <th>Submission Date</th>
                                                    <th>Status</th>
                                                    <th>Action</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {requests.map((req) => (
                                                    <tr key={req.id}>
                                                        <td>{req.referenceNumber}</td>
                                                        <td>{req.citizenName}</td>
                                                        <td>
                                                            {CATEGORY_LABELS[req.category] ||
                                                                req.category}
                                                        </td>
                                                        <td>{req.subject}</td>
                                                        <td>{formatDate(req.submissionDate)}</td>
                                                        <td>
                                                            <span
                                                                className="status-badge"
                                                                style={{
                                                                    backgroundColor:
                                                                        STATUS_COLORS[req.status],
                                                                }}
                                                            >
                                                                {STATUS_LABELS[req.status]}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <button
                                                                className="btn btn-sm btn-outline"
                                                                onClick={() =>
                                                                    handleViewDetail(req.id)
                                                                }
                                                            >
                                                                View & Reply
                                                            </button>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>

                                    {totalPages > 1 && (
                                        <div className="pagination">
                                            <button
                                                className="btn btn-sm btn-outline"
                                                disabled={page === 0}
                                                onClick={() => setPage((p) => p - 1)}
                                            >
                                                Previous
                                            </button>
                                            <span className="page-info">
                                                Page {page + 1} of {totalPages}
                                            </span>
                                            <button
                                                className="btn btn-sm btn-outline"
                                                disabled={page >= totalPages - 1}
                                                onClick={() => setPage((p) => p + 1)}
                                            >
                                                Next
                                            </button>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}