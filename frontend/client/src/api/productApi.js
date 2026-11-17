import axios from "axios";

// API gateway URL
//const API_BASE = "https://fakestoreapi.com";

const API_BASE = "http://localhost:9191/api/v1/product/products";
export const fetchProducts = async () => {
    const res = await axios.get(`${API_BASE}`);
    return res.data.data.content;
};

export const fetchProductById = async (id) => {
    const res = await axios.get(`${API_BASE}/${id}`);
    return res.data.data;
};
