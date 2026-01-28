import axiosClient from './AxiosClient'

export const serviceGetDashboard = () => axiosClient.get(`/api/v1/user/dashboard`);