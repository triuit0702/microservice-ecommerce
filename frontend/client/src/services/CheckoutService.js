import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/order";

export const orderService = (data) => axiosClient.post(API_URL, data, {
    headers: {
        'Content-Type': undefined
    }
});