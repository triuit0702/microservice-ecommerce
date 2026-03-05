import { useState } from "react";
import { CartContext } from "./CartContext";

export default function CartProvider({ children }) {
    const [cart, setCart] = useState([]);

    const addToCart = (product) => {
        setCart((prev) => [...prev, product]);
    };

    return (
        <CartContext.Provider value={{ cart, addToCart }}>
            {children}
        </CartContext.Provider>
    );
}
