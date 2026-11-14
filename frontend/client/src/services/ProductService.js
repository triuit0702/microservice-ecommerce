import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/product/products";

// Products by ids (public endpoint), used to show name/image of order items
export const getProductsByIdsService = (ids) => axiosClient.get(`${API_URL}/by-ids`, {
    params: { ids: ids.join(",") }
});
