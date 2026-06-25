import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import {
    createSupportRequest,
    getMyRequests,
    getRequestDetail,
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

export default function SupportRequestPage() {
    const { user } = useAuth();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    // Form state
    const [showForm, setShowForm] = useState(false);
    const [category, setCategory] = useState('');
    const [subject, setSubject] = useState('');
    const [description, setDescription] = useState('');
    const [attachments, setAttachments] = useState([]);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    // Detail view state
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');

    const fetchRequests = useCallback(async () => {
        setLoading(true);
        try {
            const response = await getMyRequests(page);
            if (response.code === 200) {
                setRequests(response.data.content);
                setTotalPages(response.data.totalPages);
            }
        } catch (err) {
            console.error('Failed to fetch requests:', err);
        } finally {
            setLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchRequests();
    }, [fetchRequests]);

    const handleFileChange = (e) => {
        const files = Array.from(e.target.files);
        // Validate files
        const validFiles = files.filter((file) => {
            const validTypes = ['image/jpeg', 'image/png', 'application/pdf'];
            const isValidType = validTypes.includes(file.type);
            const isValidSize = file.size <= 5 * 1024 * 1024;
            if (!isValidType || !isValidSize) {
                alert(`File "${file.name}" is invalid. Only JPG, PNG, PDF files up to 5MB are allowed.`);
                return false;
            }
            return true;
        });
        setAttachments((prev) => [...prev, ...validFiles]);
    };

    const removeAttachment = (index) => {
        setAttachments((prev) => prev.filter((_, i) => i !== index));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');

        if (!category || !subject.trim() || !description.trim()) {
            setError('Please fill in all required fields.');
            return;
        }

        setSubmitting(true);
        try {
            // Upload attachments first and get IDs
            const attachmentIds = [];
            for (const file of attachments) {
                const formData = new FormData();
                formData.append('file', file);
                const uploadResponse = await fetch('/api/support-requests/upload', {
                    method: 'POST',
                    body: formData,
                    credentials: 'include',
                });
                const uploadData = await uploadResponse.json();
                if (uploadData.code === 200) {
                    attachmentIds.push(uploadData.data);
                }
            }

            // Create support request
            await createSupportRequest({
                category,
                subject: subject.trim(),
                description: description.trim(),
                attachmentIds: attachmentIds.length > 0 ? attachmentIds : null,
            });

            setSuccessMessage('Support request has been sent successfully.');
            setShowForm(false);
            setCategory('');
            setSubject('');
            setDescription('');
            setAttachments([]);
            fetchRequests();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to submit support request.');
        } finally {
            setSubmitting(false);
        }
    };

    const handleViewDetail = async (id) => {
        setDetailLoading(true);
        setDetailError('');
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

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return new Date(dateStr).toLocaleDateString('en-GB');
    };

    const formatDateTime = (instant) => {
        if (!instant) return '';
        return new Date(instant).toLocaleString('en-GB');
    };

    if (!user) {
        return (
            <div className="page-container">
                <div className="content-area">
                    <p>Please log in to access support requests.</p>
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
                    <h1>Support Requests</h1>
                    {!showForm && !selectedRequest && (
                        <button
                            className="btn btn-primary"
                            onClick={() => setShowForm(true)}
                        >
                            + New Request
                        </button>
                    )}
                    {(showForm || selectedRequest) && (
                        <button
                            className="btn btn-secondary"
                            onClick={() => {
                                setShowForm(false);
                                setSelectedRequest(null);
                            }}
                        >
                            ← Back to List
                        </button>
                    )}
                </div>

                {successMessage && (
                    <div className="alert alert-success">{successMessage}</div>
                )}

                {error && <div className="alert alert-danger">{error}</div>}

                {/* Submission Form */}
                {showForm && (
                    <div className="card">
                        <div className="card-header">
                            <h2>New Support Request</h2>
                        </div>
                        <div className="card-body">
                            <form onSubmit={handleSubmit}>
                                <div className="form-group">
                                    <label htmlFor="category">Category *</label>
                                    <select
                                        id="category"
                                        className="form-control"
                                        value={category}
                                        onChange={(e) => setCategory(e.target.value)}
                                        required
                                    >
                                        <option value="">Select category...</option>
                                        {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
                                            <option key={key} value={key}>
                                                {label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label htmlFor="subject">Subject *</label>
                                    <input
                                        id="subject"
                                        type="text"
                                        className="form-control"
                                        value={subject}
                                        onChange={(e) => setSubject(e.target.value)}
                                        placeholder="Summarize your issue"
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="description">Description *</label>
                                    <textarea
                                        id="description"
                                        className="form-control"
                                        rows="5"
                                        value={description}
                                        onChange={(e) => setDescription(e.target.value)}
                                        placeholder="Explain your problem in detail"
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label>Attachments (Optional)</label>
                                    <div className="file-drop-zone">
                                        <input
                                            type="file"
                                            multiple
                                            onChange={handleFileChange}
                                            accept=".jpg,.jpeg,.png,.pdf"
                                            className="file-input"
                                        />
                                        <p className="file-hint">
                                            Drag & drop or click to upload (JPG, PNG, PDF - max 5MB each)
                                        </p>
                                    </div>
                                    {attachments.length > 0 && (
                                        <ul className="file-list">
                                            {attachments.map((file, index) => (
                                                <li key={index} className="file-list-item">
                                                    <span>{file.name}</span>
                                                    <button
                                                        type="button"
                                                        className="btn-remove"
                                                        onClick={() => removeAttachment(index)}
                                                    >
                                                        ✕
                                                    </button>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    disabled={submitting}
                                >
                                    {submitting ? 'Submitting...' : 'Submit Request'}
                                </button>
                            </form>
                        </div>
                    </div>
                )}

                {/* Detail View */}
                {selectedRequest && (
                    <div className="card">
                        <div className="card-header">
                            <h2>Request Detail - {selectedRequest.referenceNumber}</h2>
                            <span
                                className="status-badge"
                                style={{
                                    backgroundColor: STATUS_COLORS[selectedRequest.status],
                                }}
                            >
                                {STATUS_LABELS[selectedRequest.status]}
                            </span>
                        </div>
                        <div className="card-body">
                            <div className="detail-section">
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
                                {selectedRequest.resolvedAt && (
                                    <div className="detail-row">
                                        <strong>Resolved:</strong>{' '}
                                        {formatDateTime(selectedRequest.resolvedAt)}
                                    </div>
                                )}
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

                            <div className="detail-section">
                                <h3>Replies</h3>
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
                            </div>
                        </div>
                    </div>
                )}

                {/* List View */}
                {!showForm && !selectedRequest && (
                    <div className="card">
                        <div className="card-header">
                            <h2>My Requests</h2>
                        </div>
                        <div className="card-body">
                            {loading ? (
                                <p>Loading...</p>
                            ) : requests.length === 0 ? (
                                <p className="text-muted">
                                    You haven't submitted any support requests yet.
                                </p>
                            ) : (
                                <>
                                    <div className="table-responsive">
                                        <table className="table">
                                            <thead>
                                                <tr>
                                                    <th>Reference #</th>
                                                    <th>Subject</th>
                                                    <th>Category</th>
                                                    <th>Submission Date</th>
                                                    <th>Status</th>
                                                    <th>Action</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {requests.map((req) => (
                                                    <tr key={req.id}>
                                                        <td>{req.referenceNumber}</td>
                                                        <td>{req.subject}</td>
                                                        <td>
                                                            {CATEGORY_LABELS[req.category] ||
                                                                req.category}
                                                        </td>
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
                                                                View
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