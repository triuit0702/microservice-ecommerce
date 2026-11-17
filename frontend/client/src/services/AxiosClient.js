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

        // 401: access token expired -> refresh once, then retry the request
        if (status === 401 && originalRequest && !originalRequest._retry && !isAuthRequest(originalRequest)) {
            originalRequest._retry = true
            try {
                await refreshAccessToken()
                return axiosClient(originalRequest)
            } catch {
                // refresh token expired too -> treat as logged out (below)
            }
        }

        // 401: not logged in -> go to login
        if (status === 401) {

            // login/register requests show their own errors, no redirect
            if (isAuthRequest(originalRequest)) {
                return Promise.reject(error);
            }

            // background requests (e.g. Navbar cart load) handle 401 themselves, no redirect
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
        }    // 400: validation / business error -> no redirect
        else if (status === 400) {
            // let the component handle it
            return Promise.reject(error);
        }

        // 5xx: server error -> toast
        else if (status >= 500) {
            // const message = getErrorMessage(error);
            // showToastGlobal?.(message, "danger");
        }
        return Promise.reject(error)
    }
)

export default axiosClient
