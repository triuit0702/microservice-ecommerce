import axios from 'axios'
import { getErrorMessage } from '../utils/ToastHelper'

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

// login/refresh/logout never trigger a refresh (avoid loops); /auth/me does, so a reload after 15 min stays logged in
const NO_REFRESH_PATHS = ['/auth/login', '/auth/register', '/auth/google', '/auth/refresh', '/auth/logout']
const isAuthRequest = (config) => NO_REFRESH_PATHS.some((path) => config?.url?.includes(path))

const redirectToLogin = () => {
    if (!window.location.hash.startsWith('#/login')) {
        window.location.href = "#/login"
    }
}

axiosClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const status = error.response?.status
        const originalRequest = error.config

        // 401: access token expired -> refresh once, then retry the original request
        if (status === 401) {
            if (originalRequest && !originalRequest._retry && !isAuthRequest(originalRequest)) {
                originalRequest._retry = true
                try {
                    await refreshAccessToken()
                    return axiosClient(originalRequest)
                } catch {
                    redirectToLogin()
                    return Promise.reject(error)
                }
            }
            if (!isAuthRequest(originalRequest)) {
                redirectToLogin()
            }
            return Promise.reject(error)
        }

        if (showToastGlobal) {
            if (status === 403) {
                // logged in but missing permission: inform, do not redirect
                showToastGlobal('You do not have permission to perform this action', 'danger')
            } else if (status === 400 || status === 404 || status === 409 || status >= 500 || !error.response) {
                showToastGlobal(getErrorMessage(error), 'danger')
            }
        }
        return Promise.reject(error)
    }
)

export default axiosClient
