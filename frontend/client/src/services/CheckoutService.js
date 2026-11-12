import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/order";

// idempotencyKey: same value for every retry of one checkout -> backend never creates a duplicate order
export const orderService = (data, idempotencyKey) => axiosClient.post(API_URL, data, {
    headers: {
        'Content-Type': undefined,
        'Idempotency-Key': idempotencyKey
    }
});