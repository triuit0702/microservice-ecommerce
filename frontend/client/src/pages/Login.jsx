import React, { useState } from "react";
import { Button, Box, Divider, Alert } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import { useDispatch } from "react-redux";
import { useForm } from "react-hook-form";
import CommonTextField from "../components/FormValidate/CommonTextField";
import { serviceLogin, serviceGoogleLogin } from "../services/AuthService";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
const GOOGLE_GSI_SRC = "https://accounts.google.com/gsi/client"

// logo Google nhiều màu (MUI chỉ có icon Google một màu)
const GoogleLogo = () => (
    <svg width="18" height="18" viewBox="0 0 48 48">
        <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
        <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
        <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
        <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </svg>
);

// load script Google Identity Services một lần
let gsiPromise = null
const loadGoogleScript = () => {
    if (window.google?.accounts?.oauth2) return Promise.resolve()
    if (!gsiPromise) {
        gsiPromise = new Promise((resolve, reject) => {
            const script = document.createElement("script")
            script.src = GOOGLE_GSI_SRC
            script.async = true
            script.onload = resolve
            script.onerror = () => {
                gsiPromise = null
                reject(new Error("Không tải được Google script"))
            }
            document.head.appendChild(script)
        })
    }
    return gsiPromise
}

const getErrorMessage = (error, fallback) => error.response?.data?.error?.message || fallback

export default function Login() {
    const navigate = useNavigate()
    const location = useLocation()
    const dispatch = useDispatch()

    const { control, handleSubmit, formState: { isSubmitting } } = useForm({
        defaultValues: { username: "", password: "" }
    })

    const [errorMessage, setErrorMessage] = useState(null)
    const [googleLoading, setGoogleLoading] = useState(false)

    // lưu thông tin user sau khi login thành công (dùng chung cho login thường và Google)
    // JWT nằm trong cookie httpOnly do backend set, frontend không lưu token
    const handleLoginSuccess = (data) => {
        dispatch({
            type: "LOGIN_SUCCESS",
            payload: {
                id: data.id,
                userName: data.userName,
                permissions: data.permissions
            },
        })

        const params = new URLSearchParams(location.search)
        navigate(params.get("redirect") || "/", { replace: true })
    }

    const onSubmit = async (formData) => {
        setErrorMessage(null)
        try {
            const res = await serviceLogin(formData)
            handleLoginSuccess(res.data.data)
        } catch (error) {
            setErrorMessage(getErrorMessage(error, "Sai tên đăng nhập hoặc mật khẩu."))
        }
    }

    // mở popup Google (authorization code flow) rồi gửi code cho identity-service
    const handleGoogleLogin = async () => {
        setErrorMessage(null)
        if (!GOOGLE_CLIENT_ID) {
            setErrorMessage("Chưa cấu hình VITE_GOOGLE_CLIENT_ID.")
            return
        }

        setGoogleLoading(true)
        try {
            await loadGoogleScript()
        } catch (e) {
            setErrorMessage(e.message)
            setGoogleLoading(false)
            return
        }

        const client = window.google.accounts.oauth2.initCodeClient({
            client_id: GOOGLE_CLIENT_ID,
            scope: "openid email profile",
            ux_mode: "popup",
            callback: async (response) => {
                if (response.error) {
                    setErrorMessage("Đăng nhập Google thất bại.")
                    setGoogleLoading(false)
                    return
                }
                try {
                    const res = await serviceGoogleLogin(response.code)
                    handleLoginSuccess(res.data.data)
                } catch (error) {
                    setErrorMessage(getErrorMessage(error, "Đăng nhập Google thất bại."))
                } finally {
                    setGoogleLoading(false)
                }
            },
            // user đóng popup hoặc popup bị chặn
            error_callback: () => setGoogleLoading(false),
        })
        client.requestCode()
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
                    name="username"
                    label="Email"
                    control={control}
                    rules={{ required: "Vui lòng nhập tên đăng nhập" }}
                />

                <CommonTextField
                    name="password"
                    label="Password"
                    type="password"
                    control={control}
                    rules={{ required: "Vui lòng nhập mật khẩu" }}
                />

                <Button variant="contained" type="submit" disabled={isSubmitting} sx={{ height: 40, mt: 1 }}>
                    Login
                </Button>

                <Divider sx={{ my: 1 }}>hoặc</Divider>

                <Button
                    variant="outlined"
                    startIcon={<GoogleLogo />}
                    onClick={handleGoogleLogin}
                    disabled={googleLoading}
                    sx={{
                        height: 40,
                        textTransform: "none",
                        fontSize: 14,
                        color: "text.primary",
                        borderColor: "grey.400",
                    }}
                >
                    Đăng nhập bằng Google
                </Button>

                {errorMessage && (
                    <Alert severity="error" onClose={() => setErrorMessage(null)}>
                        {errorMessage}
                    </Alert>
                )}
            </Box>
        </form>
    );
}
