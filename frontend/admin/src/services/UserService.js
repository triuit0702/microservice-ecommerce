import axios from 'axios'
const API_URL = 'http://localhost:9898/user/api/v1/users'



export const serviceGetAllUser = () => axios.get(API_URL)

export const serviceSaveUser = (signUpRequest) => axios.post(API_URL, signUpRequest)