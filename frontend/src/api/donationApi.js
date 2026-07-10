import apiClient from './client';

const donationApi = {
    // Sponsor Management
    getSponsors: (params) => apiClient.get('/sponsors', { params }),
    getSponsor: (id) => apiClient.get(`/sponsors/${id}`),
    createSponsor: (data) => apiClient.post('/sponsors', data),
    updateSponsor: (id, data) => apiClient.put(`/sponsors/${id}`, data),
    deleteSponsor: (id) => apiClient.delete(`/sponsors/${id}`),

    // Donation Management
    getDonations: (params) => apiClient.get('/donations', { params }),
    getDonation: (id) => apiClient.get(`/donations/${id}`),
    createDonation: (data) => apiClient.post('/donations', data),
    deleteDonation: (id) => apiClient.delete(`/donations/${id}`),

    // Inventory Management
    getInventoryItems: (params) => apiClient.get('/inventory', { params }),
    getInventoryItem: (id) => apiClient.get(`/inventory/${id}`),
    createInventoryItem: (data) => apiClient.post('/inventory', data),
    updateInventoryItem: (id, data) => apiClient.put(`/inventory/${id}`, data),
    adjustQuantity: (id, quantity, reason) =>
        apiClient.patch(`/inventory/${id}/adjust`, null, { params: { quantity, reason } }),
    deleteInventoryItem: (id) => apiClient.delete(`/inventory/${id}`),

    // Allocation Plans
    getAllocationPlans: (params) => apiClient.get('/distribution/plans', { params }),
    getAllocationPlan: (id) => apiClient.get(`/distribution/plans/${id}`),
    createAllocationPlan: (data) => apiClient.post('/distribution/plans', data),
    updateAllocationPlan: (id, data) => apiClient.put(`/distribution/plans/${id}`, data),
    deleteAllocationPlan: (id) => apiClient.delete(`/distribution/plans/${id}`),

    // Distribution
    getDistributions: (params) => apiClient.get('/distribution/distribute', { params }),
    getDistribution: (id) => apiClient.get(`/distribution/distribute/${id}`),
    createDistribution: (data) => apiClient.post('/distribution/distribute', data),
    confirmDistribution: (id, status) =>
        apiClient.patch(`/distribution/distribute/${id}/confirm`, null, { params: { status } }),

    // Dashboard
    getDonationDashboard: () => apiClient.get('/donation-management/dashboard'),
};

export default donationApi;