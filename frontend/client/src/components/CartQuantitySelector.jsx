
import QuantitySelector from "./QuantitySelector";
import { updateCartService } from "../services/CartService";

import React, { useState } from "react";

export default function CartQuantitySelector({
    productId,
    variantId,
    userId,
    quantity,
    stock
}) {

    const [newQuantity, setNewQuantity] = useState(quantity);

    function handleChange(value) {
        console.log("productId: " + productId);
        console.log("variantId: " + variantId);
        console.log("userId: " + userId);

        console.log("check value: " + value);


        const cartInfo = {
            variantId: variantId,
            quantity: value, // quantity khi click tăng hoac giảm
            productId: productId,
            userId: userId,
        }
        // send api to update quantity to cache redis
        updateCartService(cartInfo).then((res) => {

            console.log("ket qua update quantity: " + res.data);
            setNewQuantity(res.data);

        }).catch((err) => {
            console.log("error update quantity: ", err);
        })

    }

    return (
        <QuantitySelector
            value={newQuantity}
            onChange={handleChange}
            max={stock}
        />
    );
}