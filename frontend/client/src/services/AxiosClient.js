import axios from 'axios'
//import { getErrorMessage } from '../utils/ToastHelper'

let showToastGlobal = null


export const injectToast = (showToast) => {
    showToastGlobal = showToast
}

const BASE_URL = "http://localhost:9191"
const REFRESH_URL = '/api/v1/user/auth/refresh'

// Auth cookies (access_token, refresh_token) are httpOnly: the browser sends them, JS never reads them
const axiosClient = axios.create({

    baseURL: BASE_URL,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
})

// Only one refresh call at a time; other 401 requests wait for it
let refreshPromise = null

const refreshAccessToken = () => {
    if (!refreshPromise) {
        refreshPromise = axios.post(BASE_URL + REFRESH_URL, null, { withCredentials: true })
            .finally(() => { refreshPromise = null })
    }
    return refreshPromise
}

// login/refresh/logout never trigger a refresh (avoid loops); /auth/me does
const NO_REFRESH_PATHS = ['/auth/login', '/auth/register', '/auth/google', '/auth/refresh', '/auth/logout']
const isAuthRequest = (config) => NO_REFRESH_PATHS.some((path) => config?.url?.includes(path))

axiosClient.interceptors.response.use(

    (response) => response,
    async (error) => {
        const status = error.response?.status;
        const originalRequest = error.config;

        // ✅ 401: access token hết hạn -> refresh 1 lần rồi gửi lại request
        if (status === 401 && originalRequest && !originalRequest._retry && !isAuthRequest(originalRequest)) {
            originalRequest._retry = true
            try {
                await refreshAccessToken()
                return axiosClient(originalRequest)
            } catch {
                // refresh token cũng hết hạn -> xử lý như chưa login (bên dưới)
            }
        }

        // ✅ 401: chưa login -> về login
        if (status === 401) {

            // Request đăng nhập/đăng ký tự hiển thị lỗi, không redirect
            if (isAuthRequest(originalRequest)) {
                return Promise.reject(error);
            }

            // Request chạy nền (vd: load giỏ hàng ở Navbar) tự xử lý 401, không redirect
            if (error.config?.skipAuthRedirect) {
                return Promise.reject(error);
            }
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
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
            // const message = getErrorMessage(error);
            // showToastGlobal?.(message, "danger");
        }
        return Promise.reject(error)
    }
)

export default axiosClient
