import axiosClient from './AxiosClient'

export const serviceGetDashboard = () => axiosClient.get(`/user/api/v1/dashboard`);