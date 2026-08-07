import axiosClient from './AxiosClient'
const API_URL = '/api/v1/user'

export const serviceLogin = (loginDetail) => axiosClient.post(API_URL + `/auth/login`, loginDetail);

export const serviceLogout = () => axiosClient.post(API_URL + `/auth/logout`, {});

export const serviceGetMe = () => axiosClient.get(API_URL + `/auth/me`);

