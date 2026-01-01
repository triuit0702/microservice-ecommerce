// views/dashboard/StatCard.jsx
import { CCard, CCardBody } from "@coreui/react";

const StatCard = ({ title, value }) => {
    return (
        <CCard className="mb-4">
            <CCardBody>
                <div className="text-muted small">{title}</div>
                <div className="fs-4 fw-semibold">{value}</div>
            </CCardBody>
        </CCard>
    );
};

export default StatCard;
