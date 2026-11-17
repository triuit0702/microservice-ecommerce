import React, { useState } from "react";
import { Button, Box, Alert, Link, Typography } from "@mui/material";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import CommonTextField from "../components/FormValidate/CommonTextField";
import { serviceRegister } from "../services/AuthService";

const getErrorMessage = (error, fallback) => error.response?.data?.error?.message || fallback

export default function Register() {
    const navigate = useNavigate()

    const { control, handleSubmit, getValues, formState: { isSubmitting } } = useForm({
        defaultValues: { name: "", email: "", password: "", confirmPassword: "" }
    })

    const [errorMessage, setErrorMessage] = useState(null)

    // backend luôn tạo tài khoản CUSTOMER, không gửi roleId
    const onSubmit = async ({ name, email, password }) => {
        setErrorMessage(null)
        try {
            await serviceRegister({ name, email, password })
            navigate("/login", { replace: true, state: { registered: true } })
        } catch (error) {
            setErrorMessage(getErrorMessage(error, "Đăng ký thất bại."))
        }
    }

    return (
        <form onSubmit={handleSubmit(onSubmit)}>
            <Box
                sx={{
                    width: 300,
                    margin: "100px auto",
                    display: "flex",
                    flexDirection: "column",
                    gap: 1
                }}
            >
                <CommonTextField
                    name="name"
                    label="Username"
                    control={control}
                    rules={{ required: "Vui lòng nhập tên đăng nhập" }}
                />

                <CommonTextField
                    name="email"
                    label="Email"
                    control={control}
                    rules={{
                        required: "Vui lòng nhập email",
                        pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: "Email không hợp lệ" }
                    }}
                />

                <CommonTextField
                    name="password"
                    label="Password"
                    type="password"
                    control={control}
                    rules={{
                        required: "Vui lòng nhập mật khẩu",
                        minLength: { value: 6, message: "Mật khẩu tối thiểu 6 ký tự" }
                    }}
                />

                <CommonTextField
                    name="confirmPassword"
                    label="Nhập lại password"
                    type="password"
                    control={control}
                    rules={{
                        required: "Vui lòng nhập lại mật khẩu",
                        validate: (value) => value === getValues("password") || "Mật khẩu không khớp"
                    }}
                />

                <Button variant="contained" type="submit" disabled={isSubmitting} sx={{ height: 40, mt: 1 }}>
                    Đăng ký
                </Button>

                {errorMessage && (
                    <Alert severity="error" onClose={() => setErrorMessage(null)}>
                        {errorMessage}
                    </Alert>
                )}

                <Typography variant="body2" sx={{ textAlign: "center", mt: 1 }}>
                    Đã có tài khoản?{" "}
                    <Link component={RouterLink} to="/login">Đăng nhập</Link>
                </Typography>
            </Box>
        </form>
    );
}
