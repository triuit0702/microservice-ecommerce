import axios from 'axios'
import axiosClient from './AxiosClient'
const API_URL = '/user/api/v1/users'



export const serviceGetAllUser = (page, size) => axiosClient.get(API_URL, {
    params: {
        page, size
    }
})

export const serviceSaveUser = (signUpRequest) => axiosClient.post(API_URL, signUpRequest)

export const serviceDeleteUser = (userId) => axiosClient.delete(API_URL, {
    params: { userId: userId }
});

export const serviceUpdateUser = (signUpRequest) => axiosClient.put(API_URL, signUpRequest)


