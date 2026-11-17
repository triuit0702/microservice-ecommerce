import axiosClient from './AxiosClient'
const API_URL = '/api/v1/user/admin/users'

export const serviceGetAllUser = (page, size) => axiosClient.get(API_URL, {
    params: {
        page, size
    }
})

export const serviceSaveUser = (createUserRequest) => axiosClient.post(API_URL, createUserRequest)

export const serviceUpdateUser = (userId, updateUserRequest) => axiosClient.put(`${API_URL}/${userId}`, updateUserRequest)

export const serviceDeleteUser = (userId) => axiosClient.delete(`${API_URL}/${userId}`)

export const serviceGetRoles = () => axiosClient.get('/api/v1/user/roles')
