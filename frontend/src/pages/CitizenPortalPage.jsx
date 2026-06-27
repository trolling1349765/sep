import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import { searchPolicies, getCategories } from '../api/citizenPortalApi';
import ChatbotWidget from '../components/ChatbotWidget';
import './CitizenPortalPage.css';

export default function CitizenPortalPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [policies, setPolicies] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 });
    const [categories, setCategories] = useState([]);
    const [keyword, setKeyword] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('');
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const PAGE_SIZE = 10;

    const fetchPolicies = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await searchPolicies({
                keyword: keyword || undefined,
                category: selectedCategory || undefined,
                page,
                size: PAGE_SIZE,
            });
            if (response?.code === 200 && response?.data) {
                setPolicies(response.data);
            } else {
                setPolicies({ content: [], totalPages: 0, totalElements: 0, number: 0 });
            }
        } catch (err) {
            setError('Failed to load policies. Please try again.');
            console.error('Error fetching policies:', err);
        } finally {
            setLoading(false);
        }
    }, [keyword, selectedCategory, page]);

    const fetchCategories = useCallback(async () => {
        try {
            const response = await getCategories();
            if (response?.code === 200 && response?.data) {
                setCategories(response.data);
            }
        } catch (err) {
            console.error('Error fetching categories:', err);
        }
    }, []);

    useEffect(() => {
        fetchCategories();
    }, [fetchCategories]);

    useEffect(() => {
        fetchPolicies();
    }, [fetchPolicies]);

    const handleSearch = (e) => {
        e.preventDefault();
        setPage(0);
        fetchPolicies();
    };

    const handleCategoryChange = (category) => {
        setSelectedCategory(category === selectedCategory ? '' : category);
        setPage(0);
    };

    const handleViewDetail = (policyId) => {
        navigate(`/citizen-portal/policy/${policyId}`);
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return new Date(dateStr).toLocaleDateString('en-GB');
    };

    if (!user) {
        return (
            <div className="page-container">
                <div className="content-area">
                    <p>Please log in to access the Citizen Portal.</p>
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
                    <h1>Citizen Portal</h1>
                </div>

                <p className="portal-subtitle" style={{ marginBottom: '16px', color: 'var(--text-secondary)' }}>
                    Browse and explore social assistance policies. Use the search bar or filter by category.
                </p>

                {/* Search Bar */}
                <form className="portal-search" onSubmit={handleSearch}>
                    <div className="search-input-wrapper">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            className="search-input"
                            placeholder="Search policies by keyword, title, or document number..."
                            value={keyword}
                            onChange={(e) => setKeyword(e.target.value)}
                        />
                    </div>
                    <button type="submit" className="btn btn-primary">Search</button>
                    {(keyword || selectedCategory) && (
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => {
                                setKeyword('');
                                setSelectedCategory('');
                                setPage(0);
                            }}
                        >
                            Clear
                        </button>
                    )}
                </form>

                {/* Category Filter */}
                {categories.length > 0 && (
                    <div className="portal-categories">
                        {categories.map((cat) => (
                            <button
                                key={cat.documentType}
                                className={`category-chip ${selectedCategory === cat.documentType ? 'category-chip--active' : ''}`}
                                onClick={() => handleCategoryChange(cat.documentType)}
                            >
                                {cat.documentType}
                                <span className="category-count">{cat.count}</span>
                            </button>
                        ))}
                    </div>
                )}

                {/* Error State */}
                {error && (
                    <div className="portal-error">
                        <span>⚠️</span>
                        <p>{error}</p>
                        <button onClick={fetchPolicies} className="btn btn-primary">Retry</button>
                    </div>
                )}

                {/* Loading State */}
                {loading && (
                    <div className="portal-loading">
                        <div className="spinner" />
                        <p>Loading policies...</p>
                    </div>
                )}

                {/* Policy Table */}
                {!loading && !error && (
                    <div className="card">
                        <div className="card-header">
                            <h2>Policy Directory</h2>
                            {policies.totalElements > 0 && (
                                <span className="text-muted" style={{ fontSize: '13px' }}>
                                    {policies.totalElements} policies found
                                </span>
                            )}
                        </div>
                        <div className="card-body">
                            {policies.content.length === 0 ? (
                                <p className="text-muted">No policies found. Try adjusting your search terms or category filter.</p>
                            ) : (
                                <>
                                    <div className="table-responsive">
                                        <table className="table">
                                            <thead>
                                                <tr>
                                                    <th style={{ width: '60px' }}>#</th>
                                                    <th>Name</th>
                                                    <th>Description</th>
                                                    <th style={{ width: '100px' }}>Action</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {policies.content.map((policy, index) => (
                                                    <tr key={policy.id}>
                                                        <td>{page * PAGE_SIZE + index + 1}</td>
                                                        <td>
                                                            <strong>{policy.title}</strong>
                                                            <br />
                                                            <small className="text-muted">
                                                                {policy.documentType}
                                                                {policy.documentNo && ` · ${policy.documentNo}`}
                                                            </small>
                                                        </td>
                                                        <td>
                                                            {policy.summary && policy.summary.length > 120
                                                                ? policy.summary.substring(0, 120) + '...'
                                                                : policy.summary || 'No summary available.'}
                                                        </td>
                                                        <td>
                                                            <button
                                                                className="btn btn-sm btn-outline"
                                                                onClick={() => handleViewDetail(policy.id)}
                                                            >
                                                                View
                                                            </button>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>

                                    {/* Pagination */}
                                    {policies.totalPages > 1 && (
                                        <div className="pagination">
                                            <button
                                                className="btn btn-sm btn-outline"
                                                disabled={page === 0}
                                                onClick={() => setPage((p) => p - 1)}
                                            >
                                                Previous
                                            </button>
                                            <span className="page-info">
                                                Page {page + 1} of {policies.totalPages}
                                            </span>
                                            <button
                                                className="btn btn-sm btn-outline"
                                                disabled={page >= policies.totalPages - 1}
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

                {/* Chatbot Widget */}
                <ChatbotWidget />
            </div>
        </div>
    );
}