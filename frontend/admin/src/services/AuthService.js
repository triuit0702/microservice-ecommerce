import axiosClient from './AxiosClient'
const API_URL = '/api/v1/user'

export const serviceLogin = (loginDetail) => axiosClient.post(API_URL + `/auth/login`, loginDetail);

export const serviceFetchCurrentUser = () => axiosClient.get(API_URL + `/auth/me`);

export const serviceLogout = () => axiosClient.post(API_URL + `/auth/logout`);
