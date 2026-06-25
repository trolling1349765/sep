import apiClient from './client';

export const searchPolicies = async (params = {}) => {
    const response = await apiClient.get('/citizen-portal/policies', {
        params: {
            keyword: params.keyword || undefined,
            category: params.category || undefined,
            page: params.page || 0,
            size: params.size || 12,
        },
    });
    return response.data;
};

export const getPolicyDetail = async (id) => {
    const response = await apiClient.get(`/citizen-portal/policies/${id}`);
    return response.data;
};

export const getCategories = async () => {
    const response = await apiClient.get('/citizen-portal/policies/categories');
    return response.data;
};

export const sendChatbotQuery = async (message) => {
    const response = await apiClient.post('/citizen-portal/chatbot/query', { message });
    return response.data;
};