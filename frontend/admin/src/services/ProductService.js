import axios from "axios";

import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/products";

export const getProducts = (params) => axios.get(API_URL, { params });
export const getProductById = (id) => axios.get(`${API_URL}/${id}`);
export const createProduct = (data) => axios.post(API_URL, data);
export const updateProduct = (id, data) => axios.put(`${API_URL}/${id}`, data);
export const deleteProduct = (id) => axios.delete(`${API_URL}/${id}`);
