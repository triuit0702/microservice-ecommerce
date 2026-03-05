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
            // ✅ 401: chưa login -> về login
            if (status === 401) {
                window.location.href = "#/login";
                return Promise.reject(error);
            }
            else if (status === 403) {

                window.location.href = "#/403";
            }    // ✅ 400: validation / business error -> KHÔNG redirect
            else if (status === 400) {
                // Để component tự xử lý
                return Promise.reject(error);
            }

            // ✅ 500 trở lên: server error -> toast
            else if (status >= 500) {
                const message = getErrorMessage(error);
                showToastGlobal?.(message, "danger");
            }
        }
        return Promise.reject(error)
    }
)

export default axiosClient
