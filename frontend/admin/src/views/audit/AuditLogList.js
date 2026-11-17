import { useEffect, useState } from 'react'
import {
    CBadge,
    CButton,
    CCard,
    CCardBody,
    CCardHeader,
    CSpinner,
    CTable,
    CTableBody,
    CTableDataCell,
    CTableHead,
    CTableHeaderCell,
    CTableRow,
} from '@coreui/react'
import { serviceGetRecentAuditLogs } from '../../services/AuditLogService'

// actions written by @Audit in identity-service
const ACTION_LABELS = {
    USER_UPDATE: { label: 'Update user', color: 'warning' },
    USER_DELETE: { label: 'Delete user', color: 'danger' },
}

const formatTime = (value) => (value ? new Date(value).toLocaleString() : '')

export default function AuditLogList() {
    const [logs, setLogs] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(false)

    const loadLogs = () => {
        setLoading(true)
        setError(false)
        serviceGetRecentAuditLogs()
            .then((res) => setLogs(res.data.data ?? []))
            .catch(() => setError(true)) // AxiosClient already shows a toast
            .finally(() => setLoading(false))
    }

    useEffect(() => {
        loadLogs()
    }, [])

    const renderBody = () => {
        if (loading) {
            return (
                <CTableRow>
                    <CTableDataCell colSpan={4} className="text-center">
                        <CSpinner size="sm" />
                    </CTableDataCell>
                </CTableRow>
            )
        }
        if (error || logs.length === 0) {
            return (
                <CTableRow>
                    <CTableDataCell colSpan={4} className="text-center text-body-secondary">
                        {error ? 'Could not load the audit log' : 'No activity yet'}
                    </CTableDataCell>
                </CTableRow>
            )
        }
        return logs.map((log) => {
            const action = ACTION_LABELS[log.action]
            return (
                <CTableRow key={log.id}>
                    <CTableDataCell>{formatTime(log.createdAt)}</CTableDataCell>
                    <CTableDataCell>{log.username}</CTableDataCell>
                    <CTableDataCell>
                        <CBadge color={action?.color ?? 'secondary'}>{action?.label ?? log.action}</CBadge>
                    </CTableDataCell>
                    <CTableDataCell>{log.resource}</CTableDataCell>
                </CTableRow>
            )
        })
    }

    return (
        <CCard>
            <CCardHeader className="d-flex justify-content-between align-items-center">
                <strong>Audit Log</strong>
                <CButton color="secondary" variant="outline" size="sm" onClick={loadLogs} disabled={loading}>
                    Reload
                </CButton>
            </CCardHeader>
            <CCardBody>
                <p className="text-body-secondary small">Latest 50 activities</p>
                <CTable hover responsive>
                    <CTableHead>
                        <CTableRow>
                            <CTableHeaderCell>Time</CTableHeaderCell>
                            <CTableHeaderCell>User</CTableHeaderCell>
                            <CTableHeaderCell>Action</CTableHeaderCell>
                            <CTableHeaderCell>Resource</CTableHeaderCell>
                        </CTableRow>
                    </CTableHead>
                    <CTableBody>{renderBody()}</CTableBody>
                </CTable>
            </CCardBody>
        </CCard>
    )
}
