import React from "react";
import { useNavigate } from "react-router-dom";
import { CButton } from "@coreui/react";
import CIcon from "@coreui/icons-react";
import { cilBan } from "@coreui/icons";

const Page403 = () => {
    const navigate = useNavigate();

    return (
        <div className="min-vh-100 d-flex flex-column justify-content-center align-items-center text-center">
            <CIcon icon={cilBan} size="5xl" className="mb-4 text-danger" />
            <h1 className="display-4 fw-bold">403</h1>
            <h4 className="mb-3">Forbidden</h4>
            <p className="text-muted mb-4">
                Bạn không có quyền truy cập trang này.
            </p>

            <div className="d-flex gap-3">
                <CButton color="primary" onClick={() => navigate("/")}>
                    Về Dashboard
                </CButton>
                <CButton color="secondary" onClick={() => navigate(-1)}>
                    Quay lại
                </CButton>
            </div>
        </div>
    );
};

export default Page403;
