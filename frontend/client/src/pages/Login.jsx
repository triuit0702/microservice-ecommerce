import React, { useState } from "react";
import { TextField, Button, Box } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { serviceLogin } from "../services/AuthService";
import { useDispatch } from 'react-redux'

import { useForm } from 'react-hook-form';

export default function Login() {
    const navigate = useNavigate();


    const dispatch = useDispatch()

    const { handleSubmit } = useForm();

    const [formLogin, setFormLogin] = useState({
        username: '',
        password: ''
    })




    const onSubmit = async () => {
        console.log("login check");
        let res = await serviceLogin(formLogin);

        let data = res.data.data

        // 🔑 lưu JWT
        localStorage.setItem('token', data.token)

        let userDetail = {
            id: data.id,
            userName: formLogin.username,
            token: data.token,
            permissions: data.permissions
        }


        dispatch({
            type: 'LOGIN_SUCCESS',
            payload: userDetail,
        })


        const params = new URLSearchParams(location.search);
        const redirectPath = params.get("redirect") || "/";
        // 👉 vào trang chính
        navigate(redirectPath, { replace: true });
    }


    const handleChange = (e) => {
        setFormLogin({ ...formLogin, [e.target.name]: e.target.value })
    }

    return (

        <form onSubmit={handleSubmit(onSubmit)}>
            <Box
                sx={{
                    width: 300,
                    margin: "100px auto",
                    display: "flex",
                    flexDirection: "column",
                    gap: 2
                }}
            >
                <TextField
                    label="Email"
                    name="username"
                    onChange={handleChange}
                />

                <TextField
                    label="Password"
                    type="password"
                    name="password"
                    onChange={handleChange}
                />

                <Button variant="contained" type="submit">
                    Login
                </Button>
            </Box>
        </form >

    );
}