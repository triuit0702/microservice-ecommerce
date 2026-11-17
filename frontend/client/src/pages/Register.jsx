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

    // the backend always creates a CUSTOMER account; no role is sent
    const onSubmit = async ({ name, email, password }) => {
        setErrorMessage(null)
        try {
            await serviceRegister({ name, email, password })
            navigate("/login", { replace: true, state: { registered: true } })
        } catch (error) {
            setErrorMessage(getErrorMessage(error, "Registration failed."))
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
                    rules={{ required: "Please enter a username" }}
                />

                <CommonTextField
                    name="email"
                    label="Email"
                    control={control}
                    rules={{
                        required: "Please enter your email",
                        pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: "Invalid email" }
                    }}
                />

                <CommonTextField
                    name="password"
                    label="Password"
                    type="password"
                    control={control}
                    rules={{
                        required: "Please enter a password",
                        minLength: { value: 6, message: "Password must be at least 6 characters" }
                    }}
                />

                <CommonTextField
                    name="confirmPassword"
                    label="Confirm password"
                    type="password"
                    control={control}
                    rules={{
                        required: "Please confirm your password",
                        validate: (value) => value === getValues("password") || "Passwords do not match"
                    }}
                />

                <Button variant="contained" type="submit" disabled={isSubmitting} sx={{ height: 40, mt: 1 }}>
                    Register
                </Button>

                {errorMessage && (
                    <Alert severity="error" onClose={() => setErrorMessage(null)}>
                        {errorMessage}
                    </Alert>
                )}

                <Typography variant="body2" sx={{ textAlign: "center", mt: 1 }}>
                    Already have an account?{" "}
                    <Link component={RouterLink} to="/login">Log in</Link>
                </Typography>
            </Box>
        </form>
    );
}
