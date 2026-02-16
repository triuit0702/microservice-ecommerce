import axios from "axios";

import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/product/products";

export const getProducts = (params) => axiosClient.get(API_URL, { params });
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


export const deleteProduct = (id) => axios.delete(`${API_URL}/${id}`);

export const uploadImage = (data) => axiosClient.post('/api/v1/product/uploads/temp',
    data,
    {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    }
);
//export const uploadImage = (data) => axiosClient.get(API_URL + '/uploads');
