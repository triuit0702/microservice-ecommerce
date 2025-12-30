import axiosClient from './AxiosClient'
import axios from 'axios';

const API_URL = 'http://localhost:9898/user/api/v1/auth/token'



export const serviceLogin = (loginDetail) => axios.post(API_URL, loginDetail);