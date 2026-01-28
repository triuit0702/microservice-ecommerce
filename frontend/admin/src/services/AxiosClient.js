import axios from 'axios'
import { getErrorMessage } from '../utils/ToastHelper'

let showToastGlobal = null


export const injectToast = (showToast) => {
    showToastGlobal = showToast
}

const axiosClient = axios.create({

    baseURL: "http://localhost:9191",
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
})

axiosClient.interceptors.request.use(config => {
    // const token = localStorage.getItem('token')
    // if (token) {
    //     config.headers.Authorization = `Bearer ${token}`
    // }
    return config
})

axiosClient.interceptors.response.use(

    (response) => response,
    (error) => {
        if (showToastGlobal) {
            const status = error.response?.status;
            if (status === 403) {

                window.location.href = "#/403";
            } else {
                const message = getErrorMessage(error)
                showToastGlobal(message, 'danger')
                window.location.href = '#/login'
            }
        }
        return Promise.reject(error)
    }
)

export default axiosClient
