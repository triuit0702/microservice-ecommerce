import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { fetchProductById } from "../api/productApi";
import { Typography, Button, CircularProgress, TextField, CardMedia, Box } from "@mui/material";
import QuantitySelector from "../components/QuantitySelector";

import { useDispatch } from "react-redux";
import { addToCart } from "../features/cartSlice";

export default function ProductDetail() {
    const { id } = useParams();

    const [product, setProduct] = useState(null);

    const [quantity, setQuantity] = useState(1);

    const dispatch = useDispatch();

    useEffect(() => {
        fetchProductById(id).then(setProduct);
    }, [id]);


    // handle add to cart
    function handleAddToCart() {
        // const
        dispatch(
            addToCart({ ...product, quantity: quantity })
        )
    }

    if (!product) return <CircularProgress />;

    return (
        <>
            <CardMedia
                component="img"
                image={product.imageUrl}
                alt={product.name}
                sx={{
                    width: "100%",
                    maxWidth: 400,
                    aspectRatio: "1 / 1",
                    objectFit: "contain",
                }}
            />
            <Typography variant="h4">{product.name}</Typography>
            <Typography variant="h6" color="primary">
                ${product.price}
            </Typography>
            <Typography sx={{ my: 2 }}>{product.description}</Typography>




            <Box sx={{ display: "flex", flexDirection: "column", gap: 2, alignItems: "flex-start" }}>


                <QuantitySelector
                    value={quantity}
                    onChange={setQuantity}
                />

                <Button variant="contained" onClick={() => handleAddToCart()}>
                    Add to Cart
                </Button>
            </Box >

        </>
    );
}
