
import axiosClient from "./AxiosClient";

const API_URL = "/api/v1/product/cart";



export const getCartByUserId = (userId) => axiosClient.get(`${API_URL}/${userId}`);

export const removeCartItemSelected = (data) => axiosClient.post(API_URL + '/remove', data, {
    headers: {
        'Content-Type': undefined
    }
});


export const addToCartService = (data) => axiosClient.post(API_URL + '/add', data, {
    headers: {
        'Content-Type': undefined
    }
});

export const updateCartService = (data) => axiosClient.post(API_URL + '/update', data, {
    headers: {
        'Content-Type': undefined
    }
});

// export const updateProduct = (id, data) => axiosClient.put(`${API_URL}/${id}`, data, {
//     headers: {
//         'Content-Type': undefined
//     }
// });