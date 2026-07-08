import React from "react";
import Button from "@mui/material/Button";
import LogoutIcon from "@mui/icons-material/Logout";

import { useNavigate } from "react-router-dom";

const LogoutButton = () => {

    const navigate = useNavigate();


    const handleLogout = () => {
        // Xóa token / session
        localStorage.removeItem("token");

        // Redirect về login
        //window.location.href = "/login";

        console.log("logout ne ");
        navigate(`/login`)
    };

    return (
        <Button
            variant="contained"
            color="error"
            startIcon={<LogoutIcon />}
            onClick={handleLogout}
        >
            Logout
        </Button>
    );
};

export default LogoutButton;