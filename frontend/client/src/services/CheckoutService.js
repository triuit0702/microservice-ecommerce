import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/order";

// idempotencyKey: same value for every retry of one checkout -> backend never creates a duplicate order
export const orderService = (data, idempotencyKey) => axiosClient.post(API_URL, data, {
    headers: {
        'Content-Type': undefined,
        'Idempotency-Key': idempotencyKey
    }
});

// Order of the current user (404 if it belongs to another user)
export const getOrderByIdService = (orderId) => axiosClient.get(`${API_URL}/${orderId}`);

// Orders of the current user, newest first. params: { page (0-based), size }
export const getMyOrdersService = (params) => axiosClient.get(API_URL, { params });