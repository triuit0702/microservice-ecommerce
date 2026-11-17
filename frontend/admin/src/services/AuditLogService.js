import axiosClient from './AxiosClient'

// backend returns at most 50 rows
export const serviceGetRecentAuditLogs = (limit = 50) =>
    axiosClient.get('/api/v1/user/audit-logs/recent', { params: { limit } })
