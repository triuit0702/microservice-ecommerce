import axiosClient from './AxiosClient'

export const getRecentAuditLogs = () => axiosClient.get(`/api/v1/user/audit-logs/recent`);