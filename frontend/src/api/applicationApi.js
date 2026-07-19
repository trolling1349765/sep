import client from './client';

export const applicationApi = {
    receiptApplication: (officierData) => {
        return client.post('/application/receipt', officierData)
    }
};
