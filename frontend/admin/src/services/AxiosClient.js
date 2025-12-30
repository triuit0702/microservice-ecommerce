import axios from 'axios'
import { getErrorMessage } from '../utils/ToastHelper'

let showToastGlobal = null

export const injectToast = (showToast) => {
    showToastGlobal = showToast
}
const API_URL = 'http://localhost:9898/user/api/v1/users'

const axiosClient = axios.create({
    //baseURL: process.env.REACT_APP_API_URL,
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
})

axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (showToastGlobal) {
            const message = getErrorMessage(error)
            showToastGlobal(message, 'danger')
        }
        return Promise.reject(error)
    }
)

export default axiosClient
