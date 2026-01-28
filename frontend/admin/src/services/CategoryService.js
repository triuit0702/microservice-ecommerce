import axiosClient from './AxiosClient'

const API_URL = '/api/v1/product/categories'

export const serviceGetAllCategories = () => axiosClient.get(API_URL)

export const serviceCreateCategory = (requestDto) => axiosClient.post(API_URL, requestDto)

export const serviceGetById = (id) => axiosClient.get(`${API_URL}/${id}`);

export const serviceUpdateCategory = (id, requestDto) => axiosClient.put(`${API_URL}/${id}`, requestDto)