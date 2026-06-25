import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import { getPolicyDetail } from '../api/citizenPortalApi';
import ChatbotWidget from '../components/ChatbotWidget';
import './PolicyDetailPage.css';

export default function PolicyDetailPage() {
    const { user } = useAuth();
    const { id } = useParams();
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [policy, setPolicy] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showEligibility, setShowEligibility] = useState(false);

    const fetchPolicy = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await getPolicyDetail(id);
            if (response?.code === 200 && response?.data) {
                setPolicy(response.data);
            } else {
                setError('Policy not found.');
            }
        } catch (err) {
            setError('Failed to load policy details. Please try again.');
            console.error('Error fetching policy detail:', err);
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        fetchPolicy();
    }, [fetchPolicy]);

    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        return new Date(dateStr).toLocaleDateString('en-GB', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
        });
    };

    const handleCheckEligibility = () => {
        setShowEligibility(!showEligibility);
    };

    const handleApplyNow = () => {
        navigate(`/applications/new?policyId=${id}`);
    };

    // Loading State
    if (loading) {
        return (
            <div className="page-container">
                <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
                <div className="content-area">
                    <button className="sidebar-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>☰</button>
                    <div className="policy-detail-loading">
                        <div className="spinner"></div>
                        <p>Loading policy details...</p>
                    </div>
                </div>
            </div>
        );
    }

    // Error State
    if (error || !policy) {
        return (
            <div className="page-container">
                <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
                <div className="content-area">
                    <button className="sidebar-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>☰</button>
                    <div className="policy-detail-error">
                        <span>⚠️</span>
                        <h3>Error</h3>
                        <p>{error || 'Policy not found.'}</p>
                        <button onClick={() => navigate('/citizen-portal')} className="btn btn-primary">
                            ← Back to Policy Directory
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="page-container">
            <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
            <div className="content-area">
                <button className="sidebar-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>☰</button>

                {/* Back Navigation */}
                <button onClick={() => navigate('/citizen-portal')} className="back-link">
                    ← Back to Policy Directory
                </button>

                {/* Policy Header */}
                <div className="policy-detail-header">
                    <div className="policy-detail-meta">
                        <span className="policy-type-badge policy-type-badge--large">{policy.documentType}</span>
                        {policy.documentNo && (
                            <span className="policy-doc-no">Document No: {policy.documentNo}</span>
                        )}
                    </div>
                    <h1 className="policy-detail-title">{policy.title}</h1>
                    <div className="policy-detail-dates">
                        {policy.issuer && (
                            <div className="policy-info-item">
                                <span className="info-label">Issuer:</span>
                                <span className="info-value">{policy.issuer}</span>
                            </div>
                        )}
                        <div className="policy-info-item">
                            <span className="info-label">Issued Date:</span>
                            <span className="info-value">{formatDate(policy.issuedDate)}</span>
                        </div>
                        {policy.effectiveDate && (
                            <div className="policy-info-item">
                                <span className="info-label">Effective Date:</span>
                                <span className="info-value">{formatDate(policy.effectiveDate)}</span>
                            </div>
                        )}
                        {policy.expiredDate && (
                            <div className="policy-info-item">
                                <span className="info-label">Expired Date:</span>
                                <span className="info-value">{formatDate(policy.expiredDate)}</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Action Buttons */}
                <div className="policy-detail-actions">
                    <button className="btn btn-primary" onClick={handleCheckEligibility}>
                        {showEligibility ? 'Hide Eligibility' : 'Check Eligibility'}
                    </button>
                    <button className="btn btn-success" onClick={handleApplyNow}>
                        Apply Now
                    </button>
                </div>

                {/* Eligibility Criteria (Conditional) */}
                {showEligibility && (
                    <div className="card" style={{ borderLeft: '4px solid #d97706', marginBottom: '20px' }}>
                        <div className="card-header">
                            <h2>Eligibility Check</h2>
                        </div>
                        <div className="card-body">
                            {policy.eligibilityCriterias && policy.eligibilityCriterias.length > 0 ? (
                                <div className="eligibility-list">
                                    {policy.eligibilityCriterias.map((criteria) => (
                                        <div key={criteria.id} className="eligibility-item">
                                            <div className="eligibility-item-header">
                                                <span className="eligibility-subject">{criteria.applicableSubject}</span>
                                            </div>
                                            {criteria.conditionValue && (
                                                <p className="eligibility-condition">
                                                    <strong>Condition:</strong> {criteria.conditionValue}
                                                </p>
                                            )}
                                            {criteria.benchmark && (
                                                <p className="eligibility-benchmark">
                                                    <strong>Benchmark:</strong> {criteria.benchmark}
                                                </p>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className="text-muted">No specific eligibility criteria listed for this policy.</p>
                            )}
                            {policy.benefitRule && (
                                <div className="benefit-rule-box">
                                    <h3>Benefit Information</h3>
                                    {policy.benefitRule.formula && (
                                        <p><strong>Formula:</strong> {policy.benefitRule.formula}</p>
                                    )}
                                    {policy.benefitRule.benchmark && (
                                        <p><strong>Benchmark:</strong> {policy.benefitRule.benchmark}</p>
                                    )}
                                    {policy.benefitRule.multiplier && (
                                        <p><strong>Multiplier:</strong> {policy.benefitRule.multiplier}</p>
                                    )}
                                </div>
                            )}
                            <p className="eligibility-disclaimer">
                                ⚠️ This information is for reference only. Official eligibility determination will be made by the competent authority upon application review.
                            </p>
                        </div>
                    </div>
                )}

                {/* Policy Summary */}
                {policy.summary && (
                    <div className="card" style={{ marginBottom: '20px' }}>
                        <div className="card-header">
                            <h2>Summary</h2>
                        </div>
                        <div className="card-body">
                            <p className="policy-summary-text">{policy.summary}</p>
                        </div>
                    </div>
                )}

                {/* Articles */}
                {policy.articles && policy.articles.length > 0 && (
                    <div className="card" style={{ marginBottom: '20px' }}>
                        <div className="card-header">
                            <h2>Articles / Provisions</h2>
                        </div>
                        <div className="card-body">
                            <div className="articles-list">
                                {policy.articles.map((article) => (
                                    <details key={article.id} className="article-item">
                                        <summary className="article-header">
                                            <span className="article-no">Article {article.articleNo}</span>
                                            <span className="article-title">{article.title}</span>
                                        </summary>
                                        <div className="article-content">
                                            <p>{article.content}</p>
                                        </div>
                                    </details>
                                ))}
                            </div>
                        </div>
                    </div>
                )}

                {/* File URL */}
                {policy.fileURL && (
                    <div className="card" style={{ marginBottom: '20px' }}>
                        <div className="card-header">
                            <h2>Reference Document</h2>
                        </div>
                        <div className="card-body">
                            <a href={policy.fileURL} target="_blank" rel="noopener noreferrer" className="file-link">
                                📄 View Official Document
                            </a>
                        </div>
                    </div>
                )}

                {/* Chatbot Widget */}
                <ChatbotWidget />
            </div>
        </div>
    );
}