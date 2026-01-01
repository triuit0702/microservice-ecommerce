import axiosClient from './AxiosClient'

export const getRecentAuditLogs = () => axiosClient.get(`/user/api/v1/audit-logs/recent`);