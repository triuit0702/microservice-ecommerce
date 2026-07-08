import React, { useEffect, useState } from "react";
import {
    Grid,
    Typography,
    Button,
    ToggleButton,
    ToggleButtonGroup,
    Box,
    Stack,
    Container
} from "@mui/material";
import { fetchProductById } from "../api/productApi";
import { useParams } from "react-router-dom";
import QuantitySelector from "../components/QuantitySelector";
import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { addToCartService } from "../services/CartService";
import { useSelector, useDispatch } from 'react-redux';

import { addToCart } from "../features/cartSlice";

// const product = {
//     name: "Basic T-Shirt",
//     variants: [
//         { id: 1, color: "Black", size: "M", price: 39, stock: 10, image: "/black-m.jpg" },
//         { id: 2, color: "Black", size: "L", price: 39, stock: 0, image: "/black-l.jpg" },
//         { id: 3, color: "White", size: "M", price: 39, stock: 5, image: "/white-m.jpg" }
//     ]
// }

export default function ProductDetail() {
    const dispatch = useDispatch();

    const [product, setProduct] = useState({});

    const [color, setColor] = useState(null);
    const [size, setSize] = useState(null);

    const [stock, setStock] = useState(1);

    // const [formCart, setFormCart] = useState({
    //     productId: '',
    //     variantId: '',
    //     quantity: '',
    //     userId: ''
    // })

    const navigate = useNavigate();

    // get product id from param
    const { id } = useParams();


    useEffect(() => {
        fetchProductById(id).then((data) => {
            setProduct(data);
            if (data?.variants?.length) {
                setColor(data.variants[0].color);
                setSize(data.variants[0].size);
            }
        });
    }, [id]);

    // create variant map
    const variantMap = useMemo(() => {

        const map = {};

        product?.variants?.forEach(v => {
            if (!map[v.color]) {
                map[v.color] = {};
            }

            map[v.color][v.size] = v;
        });

        return map;

    }, [product]);


    // create sizes
    const sizes = useMemo(() => {
        if (!color) return [];
        return Object.keys(variantMap[color] || {});
    }, [color, variantMap]);



    const colors = [...new Set(product?.variants?.map(v => v.color) || [])];

    const selectedVariant = product?.variants?.find(
        v => v.color === color && v.size === size
    );

    function chooseColor(color) {
        setColor(color);




        const sizes = Object.keys(variantMap[color]);
        setSize(sizes[0]);
        setStock(1);
    }


    const user = useSelector(state => state.auth.user);

    const handleAddToCart = async () => {

        if (!user) {
            const currentPath = window.location.pathname + window.location.search;
            navigate(`/login?redirect=${encodeURIComponent(currentPath)}`);
            return;
        }

        // create data before add cart
        const selectVariantByColor = variantMap[color][size];

        const newCart = {
            variantId: selectVariantByColor.id,
            quantity: stock,
            productId: id,
            userId: user.id,
            imageUrl: product.imageUrl,
            color: color
        }

        // call api add to cart 
        await addToCartService(newCart);
        // update biểu tượng giỏ hàng

        dispatch(
            addToCart({ ...product, quantity: stock })
        )

    }

    return (
        <Container maxWidth="lg" >
            <Grid container spacing={2} sx={{ display: "flex" }} >

                {/* IMAGE */}
                <Grid size={6}>
                    <img
                        src={selectedVariant?.imageUrl?.replace(
                            "/upload/",
                            "/upload/w_320,q_auto,f_auto/"
                        )}
                        alt={product.name}

                    />
                </Grid>

                {/* PRODUCT INFO */}
                <Grid size={6} >
                    <Stack spacing={3}>
                        <Typography variant="h4">{product.name}</Typography>

                        <Typography variant="h5" sx={{ mt: 2 }}>
                            ${selectedVariant?.price}
                        </Typography>

                        {/* COLOR */}
                        <Box sx={{ mt: 3 }}>
                            <Typography variant="subtitle1">Color</Typography>

                            <ToggleButtonGroup
                                value={color}
                                exclusive
                                onChange={(e, value) => chooseColor(value)}
                            >
                                {colors.map(c => (
                                    <ToggleButton key={c} value={c}>
                                        {c}
                                    </ToggleButton>
                                ))}
                            </ToggleButtonGroup>
                        </Box>

                        {/* SIZE */}
                        <Box sx={{ mt: 3 }}>
                            <Typography variant="subtitle1">Size</Typography>

                            <ToggleButtonGroup
                                value={size}
                                exclusive
                                onChange={(e, value) => {
                                    value && setSize(value);
                                    setStock(1);
                                }}
                            >
                                {sizes.map(s => {
                                    const variant = variantMap[color]?.[s];
                                    return (
                                        <ToggleButton
                                            key={s}
                                            value={s}
                                            disabled={!variant || variant.stock <= 0}
                                        >
                                            {s}
                                        </ToggleButton>
                                    )
                                })}





                            </ToggleButtonGroup>
                        </Box>

                        {/* STOCK */}
                        <Typography sx={{ mt: 2 }}>
                            Stock
                        </Typography>


                        <QuantitySelector
                            value={stock}
                            onChange={setStock}
                            max={variantMap[color]?.[size].stockQuantity}
                        />


                        {/* BUTTON */}
                        <Button
                            variant="contained"
                            size="large"
                            sx={{ mt: 3 }}
                            onClick={handleAddToCart}
                            disabled={!selectedVariant || selectedVariant.stock === 0}
                        >
                            Add to Cart
                        </Button>
                    </Stack>
                </Grid>

                {/* DESCRIPTION */}
                <Grid size={12}>
                    <Box sx={{ mt: 3 }}>
                        <Typography variant="h5" gutterBottom>
                            Product Description
                        </Typography>

                        <Typography variant="body1" color="text.secondary">
                            {product.description}
                        </Typography>
                    </Box>
                </Grid>

            </Grid>
        </Container>
    );
}