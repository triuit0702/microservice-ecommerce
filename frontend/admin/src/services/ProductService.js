
import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/product/products";

export const getProducts = (page, size) => axiosClient.get(API_URL, {
    params: {
        page, size
    }
});

export const getProductById = (id) => axiosClient.get(`${API_URL}/${id}`);

export const createProduct = (data) => axiosClient.post(API_URL, data, {
    headers: {
        'Content-Type': undefined
    }
});

export const updateProduct = (id, data) => axiosClient.put(`${API_URL}/${id}`, data, {
    headers: {
        'Content-Type': undefined
    }
});


export const deleteProduct = (id) => axiosClient.delete(`${API_URL}/${id}`);

export const uploadImage = (data) => axiosClient.post('/api/v1/product/uploads/temp',
    data,
    {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    }
);
