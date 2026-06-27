import apiClient from './client';

export const createSupportRequest = async (data) => {
    const response = await apiClient.post('/support-requests', data);
    return response.data;
};

export const getMyRequests = async (page = 0, size = 20) => {
    const response = await apiClient.get('/support-requests/my', {
        params: { page, size },
    });
    return response.data;
};

export const getRequestDetail = async (id) => {
    const response = await apiClient.get(`/support-requests/${id}`);
    return response.data;
};

export const getAllRequests = async (params = {}) => {
    const response = await apiClient.get('/support-requests/manage', {
        params: {
            page: params.page || 0,
            size: params.size || 20,
            status: params.status || undefined,
            category: params.category || undefined,
            dateFrom: params.dateFrom || undefined,
            dateTo: params.dateTo || undefined,
        },
    });
    return response.data;
};

export const replyToRequest = async (id, message) => {
    const response = await apiClient.post(`/support-requests/${id}/reply`, { message });
    return response.data;
};

export const updateRequestStatus = async (id, status) => {
    const response = await apiClient.put(`/support-requests/${id}/status`, { status });
    return response.data;
};

export const uploadAttachment = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post('/support-requests/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};