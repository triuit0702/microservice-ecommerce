import {
    CCard,
    CCardHeader,
    CCardBody,
    CTable,
    CTableHead,
    CTableRow,
    CTableHeaderCell,
    CTableBody,
    CTableDataCell,
    CSpinner
} from "@coreui/react";
import { useEffect, useState } from "react";
import { getRecentAuditLogs } from "../../services/AuditSerivce";

const formatTime = (iso) => {
    return new Date(iso).toLocaleString();
};

const mapActionLabel = (action) => {
    switch (action) {
        case "LOGIN":
            return "Đăng nhập";
        case "UPDATE_USER":
            return "Cập nhật user";
        case "CREATE_USER":
            return "Tạo user";
        case "DELETE_USER":
            return "Xóa user";
        default:
            return action;
    }
};

const RecentActivity = () => {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);

    const dataLoad = [
        {
            "id": 1,
            "username": "admin",
            "action": "LOGIN",
            "resource": null,
            "createdAt": "2026-01-01T10:32:00"
        },
        {
            "id": 2,
            "username": "admin",
            "action": "UPDATE_USER",
            "resource": "user:15",
            "createdAt": "2026-01-01T10:15:00"
        }
    ]


    useEffect(() => {
        getRecentAuditLogs(5)
            .then(res => setData(res.data))
            .finally(() => setLoading(false));
    }, []);

    // useEffect(() => {
    //     setData(dataLoad);
    //     setLoading(false)
    // }, []);

    return (
        <CCard className="mb-4">
            <CCardHeader>
                <strong>Hoạt động gần đây</strong>
            </CCardHeader>

            <CCardBody>
                {loading ? (
                    <CSpinner />
                ) : (
                    <CTable small hover responsive>
                        <CTableHead>
                            <CTableRow>
                                <CTableHeaderCell>Thời gian</CTableHeaderCell>
                                <CTableHeaderCell>User</CTableHeaderCell>
                                <CTableHeaderCell>Hành động</CTableHeaderCell>
                                <CTableHeaderCell>Đối tượng</CTableHeaderCell>
                            </CTableRow>
                        </CTableHead>

                        <CTableBody>
                            {data.map(item => (
                                <CTableRow key={item.id}>
                                    <CTableDataCell>{formatTime(item.createdAt)}</CTableDataCell>
                                    <CTableDataCell>{item.username}</CTableDataCell>
                                    <CTableDataCell>{mapActionLabel(item.action)}</CTableDataCell>
                                    <CTableDataCell>{item.resource || "-"}</CTableDataCell>
                                </CTableRow>
                            ))}
                        </CTableBody>
                    </CTable>
                )}
            </CCardBody>
        </CCard>
    );
};

export default RecentActivity;
