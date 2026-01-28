import axios from "axios";

import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/product";

export const getProducts = (params) => axiosClient.get(API_URL + '/products', { params });
export const getProductById = (id) => axios.get(`${API_URL}/${id}`);

export const createProduct = (data) => axiosClient.post(API_URL + '/products', data, {
    headers: {
        'Content-Type': undefined
    }
});

export const updateProduct = (id, data) => axios.put(`${API_URL}/${id}`, data);
export const deleteProduct = (id) => axios.delete(`${API_URL}/${id}`);

export const uploadImage = (data) => axiosClient.post(API_URL + '/uploads/temp',
    data,
    {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    }
);
//export const uploadImage = (data) => axiosClient.get(API_URL + '/uploads');
