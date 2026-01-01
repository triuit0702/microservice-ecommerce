import axiosClient from './AxiosClient'


export const serviceLogin = (loginDetail) => axiosClient.post(`/user/api/v1/auth/token`, loginDetail);

export const serviceFetchCurrentUser = () => axiosClient.get(`/user/api/v1/auth/me`);