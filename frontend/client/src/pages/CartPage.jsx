// src/pages/CartPage.jsx
import {
    Container,
    Grid,
    Typography,
    Card,
    CardContent,
    Box,
    TextField,
    IconButton,
    Button,
    Paper,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { useSelector, useDispatch } from "react-redux";
import {
    removeItem
} from "../features/cartSlice";
import { getCartByUserId, removeCartItemSelected } from "../services/CartService";
import { useEffect, useState } from "react";
import CartQuantitySelector from "../components/CartQuantitySelector";


import { useNavigate } from "react-router-dom";

export default function CartPage() {
    const dispatch = useDispatch();

    const user = useSelector(state => state.auth.user);

    const [items, setItems] = useState([]);
    const [total, setTotal] = useState(0);

    const navigate = useNavigate();


    useEffect(() => {
        if (user?.id) {
            getCartByUserId(user.id).then((res) => {
                setItems(res.data.items);
                const totalVal = res.data.items.reduce((sum, item) => {
                    return sum + (item.price * item.quantity)
                }, 0);
                setTotal(totalVal);
            }).catch((err) => {
                console.log(err);
            })
        } else {
            navigate(`/login`)
        }

    }, [user]);


    function getListCartItem(userId) {
        getCartByUserId(userId).then((res) => {
            setItems(res.data.items);
            const totalVal = res.data.items.reduce((sum, item) => {
                return sum + (item.price * item.quantity)
            }, 0);
            setTotal(totalVal);
        }).catch((err) => {
            console.log(err);
        })
    }

    const deleteItem = async (productId, variantId) => {

        const cartItem = {
            variantId: variantId,
            productId: productId,
            userId: user.id,
        }
        await removeCartItemSelected(cartItem);

        removeItem(productId);
        getListCartItem(user.id);
    }

    if (items.length === 0) {
        return (
            <Container sx={{ mt: 5 }}>
                <Typography variant="h5" align="center">
                    Your cart is empty
                </Typography>
            </Container>
        );
    }

    //console.log(items);
    return (
        <Container sx={{ mt: 5 }}>
            <Typography variant="h4" gutterBottom>
                Shopping Cart
            </Typography>

            <Grid container spacing={4}>
                {/* LEFT SIDE - CART ITEMS */}
                <Grid item xs={12} md={8}>
                    {items.map((item) => (
                        <Card key={item.productId} sx={{ mb: 2 }}>
                            <CardContent sx={{ display: "flex", gap: 3, alignItems: "center" }}>
                                {/* image url */}
                                <Box
                                    component="img"
                                    src={item.imageUrl}
                                    alt={item.name}
                                    sx={{ width: 100, height: 100, objectFit: "cover" }}
                                />

                                {/* name variant and price */}
                                <Box sx={{ flex: 1 }}>
                                    <Typography variant="h6" sx={{ maxWidth: 350 }}>{item.name}</Typography>
                                    <Typography color="text.secondary">
                                        ${item.price}
                                    </Typography>
                                </Box>

                                {/* quantity */}
                                {/* <QuantitySelector
                                    value={item.quantity}
                                    onChange={updateStock}
                                /> */}

                                <CartQuantitySelector
                                    productId={item.productId}
                                    variantId={item.variantId}
                                    userId={user?.id}
                                    quantity={item.quantity}
                                />

                                {/* total price */}

                                <Typography sx={{ width: 100, textAlign: "right" }}>
                                    ${(item.price * item.quantity).toFixed(2)}
                                </Typography>

                                {/* delete order button */}
                                <IconButton
                                    color="error"
                                    onClick={() => deleteItem(item.productId, item.variantId)}
                                >
                                    <DeleteIcon />
                                </IconButton>
                            </CardContent>
                        </Card>
                    ))}
                </Grid>

                {/* RIGHT SIDE - SUMMARY */}
                <Grid item xs={12} md={4}>
                    <Paper sx={{ p: 3 }}>
                        <Typography variant="h6" gutterBottom>
                            Order Summary
                        </Typography>

                        <Box
                            sx={{ display: "flex", justifyContent: "space-between", mb: 2 }}
                        >
                            <Typography>Subtotal</Typography>
                            <Typography>${total}</Typography>
                        </Box>

                        <Button
                            variant="contained"
                            fullWidth
                            size="large"
                            disabled={total === 0}
                            onClick={() => navigate(`/checkout`)}
                        >
                            Checkout
                        </Button>
                    </Paper>
                </Grid>
            </Grid>
        </Container>
    );
}