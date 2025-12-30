import axios from 'axios'
const API_URL = 'http://localhost:9898/user/api/v1/users'



export const serviceGetAllUser = (page, size) => axios.get(API_URL, {
    params: {
        page, size
    }
})

export const serviceSaveUser = (signUpRequest) => axios.post(API_URL, signUpRequest)

export const serviceDeleteUser = (userId) => axios.delete(API_URL, {
    params: { userId: userId }
});

export const serviceUpdateUser = (signUpRequest) => axios.put(API_URL, signUpRequest)